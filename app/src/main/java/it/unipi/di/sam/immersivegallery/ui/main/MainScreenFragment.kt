package it.unipi.di.sam.immersivegallery.ui.main

import android.os.Bundle
import android.provider.MediaStore
import android.widget.AutoCompleteTextView
import androidx.core.content.ContentResolverCompat
import androidx.navigation.navGraphViewModels
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import it.unipi.di.sam.immersivegallery.R
import it.unipi.di.sam.immersivegallery.common.BaseFragment
import it.unipi.di.sam.immersivegallery.databinding.FragmentMainScreenBinding
import it.unipi.di.sam.immersivegallery.models.ALL_BUCKET_FILTER
import it.unipi.di.sam.immersivegallery.models.ImageSearchFilterBucket

class MainScreenFragment :
    BaseFragment<FragmentMainScreenBinding>(FragmentMainScreenBinding::inflate) {

    private val viewModel: MainScreenViewModel by navGraphViewModels(R.id.main_navigation)

    override fun setup(savedInstanceState: Bundle?) {
        setupUI()
        setupObservers()
        loadFiltersAsync()
    }

    private fun setupObservers() {
        // Filters loaded event.
        // (sent once after first async load)
        viewModel.filtersLoaded.observe(viewLifecycleOwner) {
            // Retrieve old filters
            val oldFilters = getOldFilters()

            // Update ui elements (ex. text inputs, checkboxes, ...)
            updateUI(oldFilters)

            // Communicate view model to load old filters
            viewModel.restoreFilters(oldFilters)
        }

        // Buckets list event.
        // (sent once after first async load)
        viewModel.buckets.observe(viewLifecycleOwner) {
            // Fill the dropdown with the loaded values
            (binding.filterAlbum.editText as MaterialAutoCompleteTextView).run {
                setSimpleItems(
                    it.map(ImageSearchFilterBucket::displayName).toTypedArray()
                )
            }
        }

        // Images list event.
        // Sent every time a search gives results.
        viewModel.images.observe(viewLifecycleOwner) {
            // TODO: Temporary
            binding.imagesListText.text = it.size.toString()

            // Update carousel
            // TODO: Carousel
        }

        // Reload event requested.
        // Sent every time a new search is requested.
        viewModel.reload.observe(viewLifecycleOwner) {
            // Create and send cursor to view model
            loadImageListAsync(it)

            // Save filters
            saveFilters(it)

            // TODO: Update ui in loading mode ?
        }
    }

    private fun setupUI() {
        with(binding) {
            // Filters: Album
            (filterAlbum.editText as AutoCompleteTextView).setText("loading...", false)
            (binding.filterAlbum.editText as AutoCompleteTextView)
                .setOnItemClickListener { _, _, i, _ ->
                    viewModel.updateSelectedAlbum(i)
                }

            // Results
            imagesListText.text = "loading..."
        }
    }

    private fun loadFiltersAsync() {
        val query = ContentResolverCompat.query(
            requireContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            ),
            null,
            null,
            null,
            null,
        )

        viewModel.loadFiltersQueryAsync(query)
    }

    private fun loadImageListAsync(filters: MainScreenViewModel.ImageSearchFilters) {
        val selection = StringBuilder("")
        val selectionArgs = mutableListOf<String>()

        filters.bucket
            .takeIf { bucket -> bucket.bucketId != ALL_BUCKET_FILTER.bucketId }
            ?.let { bucket ->
                selection.append("${MediaStore.Images.Media.BUCKET_ID} = ?")
                selectionArgs.add(bucket.bucketId.toString())
            }

        val query = ContentResolverCompat.query(
            requireContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            ),
            selection.toString(),
            selectionArgs.toTypedArray(),
            null,
            null,
        )

        viewModel.loadImagesQueryAsync(query)
    }

    private fun getOldFilters(): MainScreenViewModel.ImageSearchFilters {
        return MainScreenViewModel.ImageSearchFilters(
            bucket = null ?: ALL_BUCKET_FILTER
        )
    }

    private fun saveFilters(filters: MainScreenViewModel.ImageSearchFilters) {
        // TODO
    }

    private fun updateUI(filters: MainScreenViewModel.ImageSearchFilters) {
        with(binding) {
            filters.bucket.let { bucket ->
                (filterAlbum.editText as MaterialAutoCompleteTextView)
                    .setText(bucket.displayName, false)
            }
        }
    }
}