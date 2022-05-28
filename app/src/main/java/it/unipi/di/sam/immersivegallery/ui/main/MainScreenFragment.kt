package it.unipi.di.sam.immersivegallery.ui.main

import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ArrayAdapter
import androidx.core.content.ContentResolverCompat
import androidx.navigation.navGraphViewModels
import it.unipi.di.sam.immersivegallery.R
import it.unipi.di.sam.immersivegallery.common.BaseFragment
import it.unipi.di.sam.immersivegallery.common.GenericArrayAdapter
import it.unipi.di.sam.immersivegallery.common.replaceList
import it.unipi.di.sam.immersivegallery.databinding.FragmentMainScreenBinding
import it.unipi.di.sam.immersivegallery.databinding.SpinnerItemBinding
import it.unipi.di.sam.immersivegallery.models.ImageSearchFilterBucket
import it.unipi.di.sam.immersivegallery.models.ImageSearchFilterBucketSpinnerItem

class MainScreenFragment :
    BaseFragment<FragmentMainScreenBinding>(FragmentMainScreenBinding::inflate) {

    private val viewModel: MainScreenViewModel by navGraphViewModels(R.id.main_navigation)

    override fun setup(savedInstanceState: Bundle?) {
        // ======================
        setupUI()
        // ======================
        setupObservers()
        // ======================
        loadFiltersAsync()
        // loadImageListAsync()
    }

    private fun setupObservers() {
        viewModel.reload.observe(viewLifecycleOwner) {
            // TODO
        }

        viewModel.loading.observe(viewLifecycleOwner) {
            // TODO
        }

        viewModel.buckets.observe(viewLifecycleOwner) {
            binding.filterAlbum.adapter.replaceList(it)
        }
    }

    private fun setupUI() {
        with(binding) {
            filterAlbum.adapter = GenericArrayAdapter(
                context = requireContext(),
                handler = ImageSearchFilterBucketSpinnerItem(),
                items = emptyList()
            )
        }
    }

    private fun loadFiltersAsync() {
        val query = ContentResolverCompat.query(
            requireContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // Uri
            arrayOf(
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            ), // Projection
            null, // Selection
            null, // Selection args
            null, // Sort order
            null, // Cancellation signal
        )
        viewModel.loadFiltersQueryAsync(query)
    }
}