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

fun Bitmap.bestCrop(swidth: Int, sheight: Int): Bitmap {
    return if (swidth > sheight) {
        val sratio = sheight.toFloat() / swidth.toFloat()
        val finalHeight = (sratio * width.toFloat()).toInt()
        Bitmap.createBitmap(
            this,
            0, height / 2 - finalHeight / 2,
            width, finalHeight
        )
    } else {
        val sratio = swidth.toFloat() / sheight.toFloat()
        val finalWidth = (sratio * height.toFloat()).toInt()
        Bitmap.createBitmap(
            this,
            width / 2 - finalWidth / 2, 0,
            finalWidth, height
        )
    }
}
