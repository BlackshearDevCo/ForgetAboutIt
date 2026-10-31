package app.forgetaboutit.data

import org.json.JSONArray
import org.json.JSONObject

/** Plain JSON export / import so reminders survive a reinstall or a new phone. */
object Backup {
    fun toJson(reminders: List<Reminder>, occurrences: List<Occurrence>): String {
        val rs = JSONArray()
        for (r in reminders) {
            rs.put(
                JSONObject()
                    .put("id", r.id)
                    .put("title", r.title)
                    .put("icon", r.icon)
                    .put("repeat", r.repeat.name)
                    .put("intervalDays", r.intervalDays)
                    .put("weekdays", r.weekdays)
                    .put("anchorDay", r.anchorDay)
                    .put("times", r.times)
                    .put("nudgeFromMinutes", r.nudgeFromMinutes)
                    .put("nudge", r.nudge.name)
                    .put("createdAt", r.createdAt)
                    .put("archived", r.archived),
            )
        }
        val os = JSONArray()
        for (o in occurrences) {
            os.put(
                JSONObject()
                    .put("reminderId", o.reminderId)
                    .put("day", o.day)
                    .put("slot", o.slot)
                    .put("status", o.status.name)
                    .put("doneAt", o.doneAt)
                    .put("nudgeCount", o.nudgeCount)
                    .put("lastNotifiedAt", o.lastNotifiedAt)
                    .put("snoozedUntil", o.snoozedUntil),
            )
        }
        return JSONObject().put("app", "ForgetAboutIt").put("version", 1)
            .put("reminders", rs).put("occurrences", os).toString(2)
    }

    fun fromJson(text: String): Pair<List<Reminder>, List<Occurrence>> {
        val root = JSONObject(text)
        val rs = root.getJSONArray("reminders")
        val reminders = (0 until rs.length()).map { i ->
            val o = rs.getJSONObject(i)
            Reminder(
                id = o.getLong("id"),
                title = o.getString("title"),
                icon = o.optString("icon", "bell"),
                repeat = Repeat.valueOf(o.getString("repeat")),
                intervalDays = o.optInt("intervalDays", 1),
                weekdays = o.optInt("weekdays", 0),
                anchorDay = o.getLong("anchorDay"),
                times = o.optString("times", ""),
                nudgeFromMinutes = o.optInt("nudgeFromMinutes", 16 * 60),
                nudge = Nudge.valueOf(o.optString("nudge", Nudge.PERSISTENT.name)),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                archived = o.optBoolean("archived", false),
            )
        }
        val os = root.optJSONArray("occurrences") ?: JSONArray()
        val occurrences = (0 until os.length()).map { i ->
            val o = os.getJSONObject(i)
            Occurrence(
                reminderId = o.getLong("reminderId"),
                day = o.getLong("day"),
                slot = o.getInt("slot"),
                status = Status.valueOf(o.optString("status", Status.PENDING.name)),
                doneAt = o.optLong("doneAt", 0),
                nudgeCount = o.optInt("nudgeCount", 0),
                lastNotifiedAt = o.optLong("lastNotifiedAt", 0),
                snoozedUntil = o.optLong("snoozedUntil", 0),
            )
        }
        return reminders to occurrences
    }
}
