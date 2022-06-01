package it.unipi.di.sam.immersivegallery.common

import androidx.fragment.app.Fragment
import java.util.*

fun Fragment.reload() {
    val transaction = requireActivity().supportFragmentManager.beginTransaction()
    transaction.detach(this).attach(this).commit()
}

val Fragment.currentLocale: Locale
    get() {
        return resources.configuration.locales.get(0)
    }
