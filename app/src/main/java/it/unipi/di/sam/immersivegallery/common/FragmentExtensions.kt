package it.unipi.di.sam.immersivegallery.common

import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import it.unipi.di.sam.immersivegallery.MainActivity
import java.util.*

fun Fragment.reload() {
    val transaction = requireActivity().supportFragmentManager.beginTransaction()
    transaction.detach(this).attach(this).commit()
}

val Fragment.currentLocale: Locale
    get() {
        return resources.configuration.locales.get(0)
    }

// =================================================================================================

@RequiresApi(Build.VERSION_CODES.R)
fun Fragment.insetsController() = requireActivity().window.decorView.windowInsetsController

fun Fragment.isFullScreen(): Boolean = (requireActivity() as MainActivity).isFullScreen

fun Fragment.enterFullScreen() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        insetsController()?.hide(
            WindowInsets.Type.systemBars()
                    or WindowInsets.Type.statusBars()
                    or WindowInsets.Type.navigationBars()
        )
    } else {
        requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }
}

fun Fragment.exitFullScreen() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        insetsController()?.show(
            WindowInsets.Type.systemBars()
                    or WindowInsets.Type.statusBars()
                    or WindowInsets.Type.navigationBars()
        )
    } else {
        requireActivity().window.clearFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }
}

fun Fragment.toggleFullScreen() {
    if (!isFullScreen()) enterFullScreen()
    else exitFullScreen()
}
