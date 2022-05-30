package it.unipi.di.sam.immersivegallery.ui.main

import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.AutoCompleteTextView
import androidx.core.content.ContentResolverCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import androidx.navigation.navGraphViewModels
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import dagger.hilt.android.AndroidEntryPoint
import it.unipi.di.sam.immersivegallery.R
import it.unipi.di.sam.immersivegallery.common.*
import it.unipi.di.sam.immersivegallery.databinding.FragmentMainScreenBinding
import it.unipi.di.sam.immersivegallery.models.ALL_BUCKET_FILTER
import it.unipi.di.sam.immersivegallery.models.ImageSearchFilterBucket
import it.unipi.di.sam.immersivegallery.models.ImageSearchFilters
import it.unipi.di.sam.immersivegallery.models.ImageSearchFiltersData
import kotlin.math.abs

@AndroidEntryPoint
class MainScreenFragment :
    BaseFragment<FragmentMainScreenBinding>(FragmentMainScreenBinding::inflate) {

    companion object {
        const val V_SLIDE_TRIGGER = 0.15 // Percentage
        const val H_SLIDE_TRIGGER = 0.15 // Percentage
    }

    private val viewModel by navGraphViewModels<MainScreenViewModel>(R.id.main_navigation) { defaultViewModelProviderFactory }

    private val gestureDetector by lazy { GestureDetectorCompat(requireContext(), gestureListener) }

    override fun setup(savedInstanceState: Bundle?) {
        setupUI()
        setupObservers()
        loadFiltersAsync()
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
                handler = CarouselImageAdapterItemHandler(),
                items = emptyList(),
            )

            imagesList.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                // Always consume event to "disable" standard interactions
                return@setOnTouchListener true
            }
        }
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
            binding.imagesList.isVisible = it.isNotEmpty()
            binding.imagesListPlaceholder.isVisible = it.isEmpty()
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

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {

        private var _processed = true

        override fun onDown(e: MotionEvent?): Boolean {
            _processed = false
            return super.onDown(e)
        }

        // TODO: Handle click to focus image

        // https://developer.android.com/reference/android/view/GestureDetector.OnGestureListener
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, cdx: Float, cdy: Float): Boolean {
            if (_processed) return true

            // Cannot be null
            val p1 = e1!!
            val p2 = e2!!

            // Compute delta and absolute delta
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val adx = abs(dx)
            val ady = abs(dy)

            // User is sliding horizontally
            if (adx > ady) {
                // Check distance
                if (adx > H_SLIDE_TRIGGER * binding.imagesList.width) {
                    if (dx < 0) {
                        Log.d("SCROLL", "Right")
                        binding.imagesList.smoothScrollToPosition(
                            binding.imagesList.adapter!!.nextPosition(false)
                        )
                    } else {
                        Log.d("SCROLL", "Left")
                        binding.imagesList.smoothScrollToPosition(
                            binding.imagesList.adapter!!.prevPosition(false)
                        )
                    }
                    _processed = true
                }
            }
            // User is sliding vertically
            else {
                // Check distance
                if (ady > V_SLIDE_TRIGGER * binding.imagesList.height) {
                    // binding.imagesList.smoothScrollToPosition()
                    // return true
                }
            }

            return true
        }
    }
}