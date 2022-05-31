package it.unipi.di.sam.immersivegallery.common

import android.os.Handler
import android.os.Looper

class RestartableAsyncTask constructor(
    private val name: String,
    private val delay: Long, // ms
    private val task: () -> Unit,
) {

    private val handler by lazy { Handler(Looper.getMainLooper()) }

    public fun start() {
        handler.postDelayed(task, delay)
    }

    public fun cancel() {
        handler.removeCallbacksAndMessages(null)
    }

    public fun restart() {
        cancel()
        start()
    }

}