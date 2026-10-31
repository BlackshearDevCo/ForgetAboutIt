package app.forgetaboutit

import android.app.Application
import androidx.room.Room
import app.forgetaboutit.data.AppDb
import app.forgetaboutit.notify.Notifier

class App : Application() {
    val db: AppDb by lazy {
        Room.databaseBuilder(this, AppDb::class.java, "forgetaboutit.db").build()
    }

    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannel(this)
    }
}
