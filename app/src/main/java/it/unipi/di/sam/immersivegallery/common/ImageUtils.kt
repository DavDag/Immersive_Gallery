package it.unipi.di.sam.immersivegallery.common

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment

fun Bitmap.toARGB888(): Bitmap {
    if (this.config.equals(Bitmap.Config.ARGB_8888)) return this
    return this.copy(Bitmap.Config.ARGB_8888, false)
}

fun Uri.toBitmap(resolver: ContentResolver): Bitmap {
    return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(resolver, this)
        ImageDecoder.decodeBitmap(source)
    } else {
        MediaStore.Images.Media.getBitmap(resolver, this)
    }).toARGB888()
}

fun Fragment.toBitmap(res: Int): Bitmap? {
    return AppCompatResources.getDrawable(requireContext(), res)?.toBitmap()?.toARGB888()
}
