package app.forgetaboutit

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.forgetaboutit.data.Backup
import app.forgetaboutit.data.Occurrence
import app.forgetaboutit.data.Reminder
import app.forgetaboutit.data.dao
import app.forgetaboutit.domain.DayItem
import app.forgetaboutit.domain.OccKey
import app.forgetaboutit.domain.byKey
import app.forgetaboutit.notify.Actions
import app.forgetaboutit.notify.Notifier
import app.forgetaboutit.notify.Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context = app.applicationContext
    private val dao = ctx.dao()

    val reminders: StateFlow<List<Reminder>> =
        dao.observeReminders().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val occurrences: StateFlow<Map<OccKey, Occurrence>> =
        dao.observeOccurrences().map { it.byKey() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Wall clock, refreshed twice a minute so "overdue" labels stay current. */
    val now: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), System.currentTimeMillis())

    private fun io(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { block() }
    }

    // ---- actions on one occurrence
    fun markDone(i: DayItem) = io { Actions.done(ctx, i.reminder.id, i.occDay, i.slot) }
    fun undo(i: DayItem) = io { Actions.undo(ctx, i.reminder.id, i.occDay, i.slot) }
    fun markDone(rid: Long, day: Long, slot: Int) = io { Actions.done(ctx, rid, day, slot) }
    fun skip(rid: Long, day: Long, slot: Int) = io { Actions.skip(ctx, rid, day, slot) }
    fun snooze(rid: Long, day: Long, slot: Int, minutes: Int) =
        io { Actions.snooze(ctx, rid, day, slot, minutes) }

    // ---- reminders
    suspend fun getReminder(id: Long): Reminder? = withContext(Dispatchers.IO) { dao.reminder(id) }

    fun save(r: Reminder, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) {
                val savedId = if (r.id == 0L) {
                    dao.insert(r)
                } else {
                    dao.update(r)
                    r.id
                }
                dao.reminder(savedId)?.let { Scheduler.onSaved(ctx, it) }
                savedId
            }
            onSaved(id)
        }
    }

    fun delete(id: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val pending = dao.occurrences().filter { it.reminderId == id }
                pending.forEach { Notifier.cancel(ctx, it.reminderId, it.day, it.slot) }
                dao.deleteOccurrencesOf(id)
                dao.deleteReminder(id)
                Scheduler.reschedule(ctx)
            }
            onDeleted()
        }
    }

    /** Re-arm the alarm. Cheap; called whenever the app opens. */
    fun rescheduleAll() = io { Scheduler.reschedule(ctx) }

    // ---- backup
    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        Backup.toJson(dao.reminders(), dao.occurrences())
    }

    /** Replaces everything with the backup. Returns how many reminders were restored. */
    suspend fun importJson(text: String): Int = withContext(Dispatchers.IO) {
        val (rs, os) = Backup.fromJson(text)
        dao.clearOccurrences()
        dao.clearReminders()
        dao.insertAllReminders(rs)
        dao.insertAllOccurrences(os)
        Scheduler.reschedule(ctx)
        rs.size
    }
}
