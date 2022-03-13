package rbq2012.strangemusics.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import rbq2012.strangemusics.model.ActivePlayingItem
import rbq2012.strangemusics.model.PlayingListItem

@Database(entities = [PlayingListItem::class, ActivePlayingItem::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playingListDao(): PlayingListDao

    companion object {

        var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            if (instance == null)
                instance = Room.databaseBuilder(
                    context, AppDatabase::class.java, "app-database"
                ).build()
            return instance!!
        }

    }
}