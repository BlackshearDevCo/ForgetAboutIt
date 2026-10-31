package app.forgetaboutit.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import app.forgetaboutit.App
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE archived = 0 ORDER BY id")
    fun observeReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE archived = 0 ORDER BY id")
    suspend fun reminders(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun reminder(id: Long): Reminder?

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)

    @Query("DELETE FROM occurrences WHERE reminderId = :id")
    suspend fun deleteOccurrencesOf(id: Long)

    @Query("SELECT * FROM occurrences")
    fun observeOccurrences(): Flow<List<Occurrence>>

    @Query("SELECT * FROM occurrences")
    suspend fun occurrences(): List<Occurrence>

    @Query("SELECT * FROM occurrences WHERE reminderId = :reminderId AND day = :day AND slot = :slot")
    suspend fun occurrence(reminderId: Long, day: Long, slot: Int): Occurrence?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(occurrence: Occurrence)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReminders(list: List<Reminder>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOccurrences(list: List<Occurrence>)

    @Query("DELETE FROM reminders")
    suspend fun clearReminders()

    @Query("DELETE FROM occurrences")
    suspend fun clearOccurrences()
}

@Database(entities = [Reminder::class, Occurrence::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): ReminderDao
}

fun Context.dao(): ReminderDao = (applicationContext as App).db.dao()
