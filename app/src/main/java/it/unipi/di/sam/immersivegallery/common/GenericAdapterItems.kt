package it.unipi.di.sam.immersivegallery.common

import android.content.ContentUris
import android.database.Cursor
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import it.unipi.di.sam.immersivegallery.databinding.CarouselItemBinding
import it.unipi.di.sam.immersivegallery.models.ImageData

interface GenericAdapterItemHandler<T, B : ViewBinding> {
    fun id(data: T): Long
    fun inflate(inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean): View
    fun bind(view: View): B
    fun updateUI(binding: B, data: T?): Unit
}

interface WithCursorSupport<T> {
    fun fromCursorPosition(cursor: Cursor?, position: Int): T?
    fun onUpdateCursor(oldCursor: Cursor?, newCursor: Cursor?): Unit
}

open class CarouselImageAdapterItemHandler :
    GenericAdapterItemHandler<ImageData, CarouselItemBinding> {

    override fun id(data: ImageData): Long = data.bucketId

    override fun inflate(
        inflater: LayoutInflater,
        parent: ViewGroup,
        attachToParent: Boolean
    ): View =
        CarouselItemBinding.inflate(inflater, parent, attachToParent).root

    override fun bind(view: View): CarouselItemBinding = CarouselItemBinding.bind(view)

    override fun updateUI(binding: CarouselItemBinding, data: ImageData?) =
        with(binding) {
            image.setImageURI(data?.uri)
        }
}

class CarouselImageAdapterItemHandlerWithCursor :
    CarouselImageAdapterItemHandler(),
    WithCursorSupport<ImageData> {

    private var idColumn: Int? = null
    private var widthColumn: Int? = null
    private var heightColumn: Int? = null
    private var sizeColumn: Int? = null
    private var mimeColumn: Int? = null
    private var dateTakenColumn: Int? = null
    private var dateModifiedColumn: Int? = null
    private var bucketIdColumn: Int? = null
    private var bucketNameColumn: Int? = null

    override fun onUpdateCursor(oldCursor: Cursor?, newCursor: Cursor?) {
        idColumn = newCursor?.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        widthColumn = newCursor?.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
        heightColumn = newCursor?.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
        sizeColumn = newCursor?.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        mimeColumn = newCursor?.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
        bucketIdColumn = newCursor?.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
        bucketNameColumn = newCursor?.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        dateTakenColumn = newCursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
        dateModifiedColumn = newCursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
    }

    override fun fromCursorPosition(cursor: Cursor?, position: Int): ImageData? {
        if (cursor == null) return null

        cursor.moveToPosition(position)

        val id = idColumn?.run(cursor::getLong) ?: return null
        val width = widthColumn?.run(cursor::getInt) ?: return null
        val height = heightColumn?.run(cursor::getInt) ?: return null
        val size = sizeColumn?.run(cursor::getInt) ?: return null
        val mime = mimeColumn?.run(cursor::getString) ?: return null
        val dataTaken = dateTakenColumn?.run(cursor::getLong) ?: return null
        val dataModified = dateModifiedColumn?.run(cursor::getLong) ?: return null
        val bucketId = bucketIdColumn?.run(cursor::getLong) ?: return null
        val bucketName = bucketNameColumn?.run(cursor::getString) ?: return null

        val resourceUri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            id
        )

        return ImageData(
            uri = resourceUri,
            id = id,
            width = width,
            height = height,
            size = size,
            mime = mime,
            dataTaken = dataTaken, // already in ms
            dataModified = dataModified * 1000L, // sec => ms
            bucketId = bucketId,
            bucketName = bucketName,
        )
    }

}