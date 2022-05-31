package it.unipi.di.sam.immersivegallery.common

import java.util.*
import kotlin.concurrent.schedule

class RestartableAsyncTask constructor(
    val name: String,
    val delay: Long, // ms
    val task: TimerTask.() -> Unit,
) {

    private val timer: Timer by lazy { Timer(name, false) }
    private var _task: TimerTask? = null

    public fun start() {
        _task = timer.schedule(delay, task)
    }

    public fun cancel() {
        _task?.cancel()
        _task = null
    }

    public fun restart() {
        cancel()
        start()
    }

}