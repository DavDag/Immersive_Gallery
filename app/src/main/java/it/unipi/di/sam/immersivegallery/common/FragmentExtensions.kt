package it.unipi.di.sam.immersivegallery.common

import androidx.fragment.app.Fragment

fun Fragment.reload() {
    val transaction = requireActivity().supportFragmentManager.beginTransaction()
    transaction.detach(this).attach(this).commit()
}

