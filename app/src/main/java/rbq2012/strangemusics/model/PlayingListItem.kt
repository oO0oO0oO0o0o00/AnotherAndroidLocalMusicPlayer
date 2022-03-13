package rbq2012.strangemusics.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playing_list_item")
data class PlayingListItem(
    @PrimaryKey @ColumnInfo(name = "track_id") val trackId: String,
    @ColumnInfo(name = "source_playlist") val sourcePlaylist: String? = null,
    val order: Int
)