package rbq2012.strangemusics.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_playing_item")
data class ActivePlayingItem(
    @PrimaryKey val id: Int,
    val order: Int,
    val progress: Long
)