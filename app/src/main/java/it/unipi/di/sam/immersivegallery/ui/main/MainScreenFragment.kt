package it.unipi.di.sam.immersivegallery.ui.main

import android.os.Bundle
import android.provider.MediaStore
import android.widget.AutoCompleteTextView
import androidx.core.content.ContentResolverCompat
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import dagger.hilt.android.AndroidEntryPoint
import it.unipi.di.sam.immersivegallery.R
import it.unipi.di.sam.immersivegallery.common.BaseFragment
import it.unipi.di.sam.immersivegallery.common.GenericRecyclerAdapter
import it.unipi.di.sam.immersivegallery.common.ImageSearchFilterBucketSpinnerItem
import it.unipi.di.sam.immersivegallery.common.replaceList
import it.unipi.di.sam.immersivegallery.databinding.FragmentMainScreenBinding
import it.unipi.di.sam.immersivegallery.models.ALL_BUCKET_FILTER
import it.unipi.di.sam.immersivegallery.models.ImageSearchFilterBucket
import it.unipi.di.sam.immersivegallery.models.ImageSearchFilters
import it.unipi.di.sam.immersivegallery.models.ImageSearchFiltersData

@AndroidEntryPoint
class MainScreenFragment :
    BaseFragment<FragmentMainScreenBinding>(FragmentMainScreenBinding::inflate) {

    private val viewModel by navGraphViewModels<MainScreenViewModel>(R.id.main_navigation) { defaultViewModelProviderFactory }

    override fun setup(savedInstanceState: Bundle?) {
        setupUI()
        setupObservers()
        loadFiltersAsync()
    }

    private fun setupObservers() {
        // Filters loaded event.
        // (sent once after first async load)
        viewModel.filtersData.observe(viewLifecycleOwner) {
            val filtersData = it.first
            val oldFilters = it.second

            // Update ui elements (ex. text inputs, checkboxes, ...)
            updateUI(oldFilters, filtersData)

            // Communicate view model to load old filters
            viewModel.restoreFilters(oldFilters)
        }

        // Images list event.
        // Sent every time a search gives results.
        viewModel.images.observe(viewLifecycleOwner) {
            // TODO: Temporary
            binding.imagesListText.text = it.size.toString()

            // Update carousel
            binding.imagesList.adapter!!.replaceList(it)

            // TODO: Handle "0" results
        }

        // Reload event requested.
        // Sent every time a new search is requested.
        viewModel.reload.observe(viewLifecycleOwner) {
            // Create and send cursor to view model
            loadImageListAsync(it)

            // Save filters
            viewModel.saveFilters(it)

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
            imagesList.adapter = GenericRecyclerAdapter(
                context = requireContext(),
                handler = ImageSearchFilterBucketSpinnerItem(),
                items = emptyList(),
            )

            // TODO: Handle click to focus image
            // TODO: Slow down recycler (?)
        }
    }

    private fun loadFiltersAsync() {
        // Send request to read filters from shared preferences
        viewModel.getOldFilters()

        // Create cursor to retrieve filters data
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

        // Send cursor to the ViewModel to proceed asynchronously
        viewModel.loadFiltersQueryAsync(query)
    }

    private fun loadImageListAsync(filters: ImageSearchFilters) {
        // Dynamically create selection & selection args
        val selection = StringBuilder("")
        val selectionArgs = mutableListOf<String>()

        // Check if user has a bucket specified
        filters.bucket
            .takeIf { bucket -> bucket.bucketId != ALL_BUCKET_FILTER.bucketId }
            ?.let { bucket ->
                selection.append("${MediaStore.Images.Media.BUCKET_ID} = ?")
                selectionArgs.add(bucket.bucketId.toString())
            }

        // TODO: Merge cursors to gen INTERNAL/EXTERNAL queries ?

        // Create cursor to retrieve images data
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

        // Send cursor to the ViewModel to proceed asynchronously
        viewModel.loadImagesQueryAsync(query)
    }

    private fun updateUI(oldFilters: ImageSearchFilters, filtersData: ImageSearchFiltersData) {
        // Update corresponding UI elements
        with(binding) {
            // Insert old values (or default ones)
            // Album
            (filterAlbum.editText as MaterialAutoCompleteTextView)
                .setText(oldFilters.bucket.displayName, false)

            // Fill the album dropdown
            (binding.filterAlbum.editText as MaterialAutoCompleteTextView)
                .setSimpleItems(
                    filtersData.buckets.map(ImageSearchFilterBucket::displayName).toTypedArray()
                )
        }
    }
}