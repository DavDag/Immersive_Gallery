package it.unipi.di.sam.immersivegallery.common

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

fun Uri.toBitmap(resolver: ContentResolver): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(resolver, this)
        ImageDecoder.decodeBitmap(source)
            .let {
                val result = it.copy(Bitmap.Config.ARGB_8888, false)
                it.recycle()
                result
            }
    } else {
        MediaStore.Images.Media.getBitmap(resolver, this)
    }
}

