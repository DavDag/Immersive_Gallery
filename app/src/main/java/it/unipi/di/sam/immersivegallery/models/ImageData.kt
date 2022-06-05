package it.unipi.di.sam.immersivegallery.models

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import it.unipi.di.sam.immersivegallery.common.toBitmap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Lock

data class ImageData(
    val uri: Uri,
    val id: Long,
    val width: Int,
    val height: Int,
    val size: Int,
    val mime: String,
    val dataTaken: Long,
    val dataModified: Long,
    val bucketId: Long,
    val bucketName: String,
) {

    private var _bitmap: Bitmap? = null

    public fun bitmap(resolver: ContentResolver): Bitmap {
        synchronized(this) {
            if (_bitmap == null) {
                _bitmap = uri.toBitmap(resolver)
            }
        }
        return _bitmap!!
    }

}
