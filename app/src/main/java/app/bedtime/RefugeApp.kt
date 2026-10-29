package app.bedtime

import android.app.Application
import app.bedtime.data.Problems
import app.bedtime.service.BlockingState

/** Starts the problems log (writing a crash into it before Android handles it as usual) and picks up a running break. */
class RefugeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Problems.init(this)
        BlockingState.init(this)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            Problems.note("crash in ${thread.name}", error)
            previous?.uncaughtException(thread, error)
        }
    }
}
