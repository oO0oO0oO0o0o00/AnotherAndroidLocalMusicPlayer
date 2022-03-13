package rbq2012.strangemusics.data

import androidx.room.*
import rbq2012.strangemusics.model.ActivePlayingItem
import rbq2012.strangemusics.model.PlayingListItem

@Dao
interface PlayingListDao {

    @Query("SELECT * FROM playing_list_item order by `order`")
    fun getList(): List<PlayingListItem>

    @Query("SELECT * FROM active_playing_item where id=0")
    fun getCurrent(): ActivePlayingItem?

    @Query("DELETE FROM playing_list_item")
    fun deleteList()

    @Insert
    fun addList(list: List<PlayingListItem>)

    @Transaction
    fun resetList(list: List<PlayingListItem>) {
        deleteList(); addList(list)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setCurrent(item: ActivePlayingItem)

}