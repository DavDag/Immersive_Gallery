package it.unipi.di.sam.immersivegallery.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import it.unipi.di.sam.immersivegallery.common.GenericAdapterItemHandler
import it.unipi.di.sam.immersivegallery.databinding.SpinnerItemBinding

data class ImageSearchFilterBucket(
    val bucketId: Long,
    val bucketName: String,

    val displayName: String,
)

val ALL_BUCKET_FILTER = ImageSearchFilterBucket(-1L, "", "All")

class ImageSearchFilterBucketSpinnerItem :
    GenericAdapterItemHandler<ImageSearchFilterBucket, SpinnerItemBinding>() {

    override fun id(data: ImageSearchFilterBucket): Long = data.bucketId

    override fun inflate(
        layoutInflater: LayoutInflater,
        parent: ViewGroup,
        attachToParent: Boolean
    ): View = SpinnerItemBinding.inflate(layoutInflater, parent, attachToParent).root

    override fun bind(view: View): SpinnerItemBinding = SpinnerItemBinding.bind(view)

    override fun updateUI(binding: SpinnerItemBinding, data: ImageSearchFilterBucket) =
        with(binding) {
            spinnerText.text = data.bucketName
        }

}