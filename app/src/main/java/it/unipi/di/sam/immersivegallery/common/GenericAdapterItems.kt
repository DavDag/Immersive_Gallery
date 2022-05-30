package it.unipi.di.sam.immersivegallery.common

import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import it.unipi.di.sam.immersivegallery.databinding.CarouselItemBinding
import it.unipi.di.sam.immersivegallery.models.ImageData

abstract class GenericAdapterItemHandler<T, B : ViewBinding> {
    abstract fun id(data: T): Long
    abstract fun inflate(inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean): View
    abstract fun bind(view: View): B
    abstract fun updateUI(binding: B, data: T): Unit
}

abstract class GenericAdapterItemHandlerWithCursorSupport<T, B : ViewBinding> :
    GenericAdapterItemHandler<T, B>() {
    abstract fun fromCursor(cursor: Cursor): T
}

class CarouselImageAdapterItemHandler :
    GenericAdapterItemHandler<ImageData, CarouselItemBinding>() {

    override fun id(data: ImageData): Long = data.bucketId

    override fun inflate(
        layoutInflater: LayoutInflater,
        parent: ViewGroup,
        attachToParent: Boolean
    ): View = CarouselItemBinding.inflate(layoutInflater, parent, attachToParent).root

    override fun bind(view: View): CarouselItemBinding = CarouselItemBinding.bind(view)

    override fun updateUI(binding: CarouselItemBinding, data: ImageData) =
        with(binding) {
            image.setImageURI(data.uri)
        }

}