package it.unipi.di.sam.immersivegallery.ui.main

import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContentResolverCompat
import androidx.navigation.navGraphViewModels
import it.unipi.di.sam.immersivegallery.R
import it.unipi.di.sam.immersivegallery.common.BaseFragment
import it.unipi.di.sam.immersivegallery.databinding.FragmentMainScreenBinding

class MainScreenFragment :
    BaseFragment<FragmentMainScreenBinding>(FragmentMainScreenBinding::inflate) {

    private val viewModel: MainScreenViewModel by navGraphViewModels(R.id.main_navigation)

    override fun setup(savedInstanceState: Bundle?) {
        setupObservers()
        // ======================
        loadFiltersAsync()
        // loadImageListAsync()
        // ======================
        setupUI()
    }

    private fun setupObservers() {
        viewModel.loading.observe(viewLifecycleOwner) {
            // TODO
        }

        viewModel.buckets.observe(viewLifecycleOwner) {
            it.forEach { bucket ->
                Log.d("AAA", bucket.bucketName)
            }
        }
    }

    private fun setupUI() {
        // TODO
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