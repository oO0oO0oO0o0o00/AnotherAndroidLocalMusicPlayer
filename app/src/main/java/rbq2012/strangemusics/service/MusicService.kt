package rbq2012.strangemusics.service

import android.annotation.SuppressLint
import android.app.*
import android.app.PendingIntent.*
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import androidx.media3.common.C.WAKE_MODE_LOCAL
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import rbq2012.strangemusics.BuildConfig
import rbq2012.strangemusics.R
import rbq2012.strangemusics.data.AppDatabase
import rbq2012.strangemusics.data.TracksStorage
import rbq2012.strangemusics.model.ActivePlayingItem
import rbq2012.strangemusics.model.PlayingListItem
import rbq2012.strangemusics.model.Track
import rbq2012.strangemusics.ui.PlayingActivity
import rbq2012.strangemusics.viewmodel.PlayingViewModel
import java.lang.ref.WeakReference

@SuppressLint("UnsafeOptInUsageError")
class MusicService : Service(), Player.Listener, AudioManager.OnAudioFocusChangeListener {

    private var mPlayer: ExoPlayer? = null

    // for persistence
    private var mSaveProgressTaskRunning = false

    // for async operations e.g. persistence
    private val mYouCannotFindAJob = SupervisorJob()
    private val mScope = CoroutineScope(Dispatchers.Main + mYouCannotFindAJob)

    // for handling auto-paused/ducked states
    private var audioFocusRequest: AudioFocusRequest? = null
    private var mWasPlayingAtFocusLost = false
    private var mPrevAudioFocusState = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        when (intent.action) {
            Actions.Init.command -> setupPlayer()
            Actions.Play.command -> {
                setPlayingList(
                    (intent.getSerializableExtra("tracks") as Array<*>).map {
                        it as Track
                    }, intent.getIntExtra("startsFrom", 0)
                ); resume(true); savePlayingList()
            }
            Actions.Resume.command -> resume(true)
            Actions.Pause.command -> mPlayer?.pause()
            Actions.Previous.command -> {
                setupPlayer().seekToPreviousMediaItem();resume(false)
            }
            Actions.Next.command -> {
                setupPlayer().seekToNextMediaItem(); resume(false)
            }
            Actions.Seek.command -> {
                setupPlayer().seekTo(intent.getLongExtra("at", 0))
                startSaveProgressTask()
            }
            Actions.Exit.command -> stopSelf()
        }
        setupNotification()
        return START_NOT_STICKY
    }

    private fun setPlayingList(tracks: List<Track>, startsFrom: Int = 0, at: Long = 0) {
        setupPlayer().apply {
            setMediaItems(tracks.map {
                MediaItem.Builder().setUri(Uri.fromFile(it.path)).setTag(it).build()
            }, startsFrom, at); prepare()
        }
    }

    private fun setupPlayer(): Player {
        if (mPlayer == null) {
            // setup player
            mPlayer = ExoPlayer.Builder(applicationContext).build().also {
                it.addListener(this)
                it.setWakeMode(WAKE_MODE_LOCAL)
            }
            val thiz = WeakReference(this)
            PlayingViewModel.default.getProgress = {
                thiz.get()?.let {
                    it.setupPlayer()
                    it.mPlayer?.run { Pair(currentPosition, duration) }
                }
            }

            // reload
            val context = this
            mScope.launch {
                val list: List<Track>
                val currentItem: ActivePlayingItem?
                withContext(Dispatchers.IO) {
                    val playingListDao = getPlayingListDao()
                    list = playingListDao.getList().map {
                        TracksStorage.findTrack(context, it.trackId)
                    }
                    currentItem = playingListDao.getCurrent()
                }
                setPlayingList(list, currentItem?.order ?: 0, currentItem?.progress ?: 0)
            }
        }
        return mPlayer!!
    }

    private fun resume(explicit: Boolean) {
        if ((mPrevAudioFocusState == AudioManager.AUDIOFOCUS_LOSS
                    || mPrevAudioFocusState == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
            && !explicit
        ) return
        setupPlayer().play()
        mWasPlayingAtFocusLost = false
        mPrevAudioFocusState = AudioManager.AUDIOFOCUS_GAIN
    }

    private fun setupNotification() {
        val player = mPlayer ?: return
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelName = "music_player_channel"
        NotificationChannel(
            channelName,
            "Playback Control",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            enableLights(false)
            enableVibration(false)
            notificationManager.createNotificationChannel(this)
        }
        val pauseIconResource: Int
        if (player.isPlaying) {
            pauseIconResource = R.drawable.ic_baseline_pause_24
        } else {
            pauseIconResource = R.drawable.ic_baseline_play_arrow_24
        }
        val notification = Notification.Builder(applicationContext, channelName)
            .setContentText(player.currentMediaItem?.getTrack()?.name)
            .setSmallIcon(R.drawable.ic_disc_48)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
//            .setWhen(notifWhen)
//            .setShowWhen(showWhen)
//            .setUsesChronometer(usesChronometer)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(
                        this, PlayingActivity::class.java
                    ), FLAG_IMMUTABLE
                )
            )
            .setOngoing(player.isPlaying)
            .setChannelId(channelName)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setStyle(
                Notification.MediaStyle()
                    .setShowActionsInCompactView(1, 0, 2)
//                    .setMediaSession(mMediaSession?.sessionToken)
            )
            .setDeleteIntent(
                getService(
                    this, 0, Intent(
                        this, MusicService::class.java
                    ).apply { action = Actions.Exit.command }, FLAG_IMMUTABLE
                )
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, pauseIconResource),
                    "Play / Pause",
                    getPauseOrResumeAction(this, player.isPlaying)
                ).build()
            ).addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_baseline_skip_previous_24),
                    "Previous",
                    getPreviousAction(this)
                ).build()
            ).addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_baseline_skip_next_24),
                    "Next",
                    getNextAction(this)
                ).build()
            )
        startForeground(2012, notification.build())
        if (!player.isPlaying) stopForeground(false)
    }

    private fun savePlayingList() {
        val player = mPlayer ?: return
        val list: List<MediaItem>
        synchronized(player) {
            list = (0 until player.mediaItemCount).map { player.getMediaItemAt(it) }
        }
        mScope.launch {
            withContext(Dispatchers.IO) {
                getPlayingListDao().resetList(
                    list.mapIndexedNotNull { order, item ->
                        item.getTrack()
                            ?.run { PlayingListItem(trackId = identifier, order = order) }
                    }
                )
            }
        }
    }

    private fun startSaveProgressTask() {
        synchronized(mSaveProgressTaskRunning) {
            if (mSaveProgressTaskRunning) return
            mSaveProgressTaskRunning = true
            mScope.launch {
                do {
                    val player = mPlayer ?: break
                    val item = ActivePlayingItem(
                        0, order = player.currentMediaItemIndex,
                        progress = player.currentPosition
                    )
                    withContext(Dispatchers.IO) {
                        getPlayingListDao().setCurrent(item)
                    }
                    delay(2000)
                } while (player.isPlaying)
                synchronized(mSaveProgressTaskRunning) {
                    mSaveProgressTaskRunning = false
                }
            }
        }

    }

    private fun getPlayingListDao() = AppDatabase.get(applicationContext).playingListDao()

    override fun onBind(intent: Intent) = null

    override fun onDestroy() {
        super.onDestroy()
        mYouCannotFindAJob.cancel()
        mPlayer?.stop()
        mPlayer?.release()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        PlayingViewModel.default.track.value = mediaItem?.getTrack()
        setupNotification()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        PlayingViewModel.default.paused.value = !isPlaying
        startSaveProgressTask()
        setupNotification()
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (isPlaying) {
            if (this.audioFocusRequest == null) {
                val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(this)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .build()
                audioManager.requestAudioFocus(audioFocusRequest)
                this.audioFocusRequest = audioFocusRequest
            }
        } else audioFocusRequest?.let {
            if (!mWasPlayingAtFocusLost) {
                audioManager.abandonAudioFocusRequest(it); this.audioFocusRequest = null
            }
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                val player = mPlayer
                if (mWasPlayingAtFocusLost && player != null) {
                    if (mPrevAudioFocusState == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                        player.volume = 1f
                    } else {
                        player.play()
                    }
                }
                mWasPlayingAtFocusLost = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mPlayer?.also { it.volume = 0.3f; mWasPlayingAtFocusLost = it.isPlaying }
            }
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                val player = mPlayer
                if (player != null && player.isPlaying) {
                    mWasPlayingAtFocusLost = true; player.pause()
                }
            }
        }
        mPrevAudioFocusState = focusChange
    }

    private fun MediaItem.getTrack(): Track? = localConfiguration?.tag?.let { it as Track }

    companion object {

        private const val PREFIX = BuildConfig.APPLICATION_ID + ".action."

        private enum class Actions(val command: String) {
            Init(PREFIX + "INIT"),
            Play(PREFIX + "PLAY"),
            Resume(PREFIX + "RESUME"),
            Pause(PREFIX + "PAUSE"),
            Next(PREFIX + "NEXT"),
            Previous(PREFIX + "PREVIOUS"),
            Seek(PREFIX + "SEEK"),
            Exit(PREFIX + "EXIT");
        }

        fun load(context: Context) = send(context, Actions.Init)

        fun play(context: Context, tracks: Array<Track>, startsFrom: Int) {
            send(context, Actions.Play) {
                putExtra("tracks", tracks).putExtra("startsFrom", startsFrom)
            }
        }

        fun resume(context: Context) = send(context, Actions.Resume)

        fun pause(context: Context) = send(context, Actions.Pause)

        fun pauseOrResume(context: Context, pause: Boolean) =
            if (pause) this.pause(context) else resume(context)

        fun goNext(context: Context) = send(context, Actions.Next)

        fun previous(context: Context) = send(context, Actions.Previous)

        fun seek(context: Context, at: Long) {
            send(context, Actions.Seek) { putExtra("at", at) }
        }

        fun getPauseOrResumeAction(context: Context, pause: Boolean): PendingIntent =
            getPendingIntent(context, if (pause) Actions.Pause else Actions.Resume)

        fun getPreviousAction(context: Context): PendingIntent =
            getPendingIntent(context, Actions.Previous)

        fun getNextAction(context: Context): PendingIntent = getPendingIntent(context, Actions.Next)

        private fun create(
            context: Context,
            action: Actions,
            modifyIntent: Intent.() -> Unit = {}
        ): Intent {
            return Intent(context, MusicService::class.java)
                .also { it.action = action.command }.apply(modifyIntent)
        }

        private fun getPendingIntent(
            context: Context,
            action: Actions,
            modifyIntent: Intent.() -> Unit = {}
        ): PendingIntent {
            return getForegroundService(
                context,
                0, Intent(context, MusicService::class.java)
                    .also { it.action = action.command }.apply(modifyIntent),
                FLAG_IMMUTABLE
            )
        }

        private fun send(
            context: Context,
            action: Actions,
            modifyIntent: Intent.() -> Unit = {}
        ) {
            context.startForegroundService(create(context, action, modifyIntent))
        }
    }
}