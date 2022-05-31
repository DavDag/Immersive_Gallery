package it.unipi.di.sam.immersivegallery.ui.main

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.AutoCompleteTextView
import androidx.core.content.ContentResolverCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import it.unipi.di.sam.immersivegallery.R
import it.unipi.di.sam.immersivegallery.common.*
import it.unipi.di.sam.immersivegallery.databinding.FragmentMainScreenBinding
import it.unipi.di.sam.immersivegallery.models.*
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.abs

@AndroidEntryPoint
class MainScreenFragment :
    BaseFragment<FragmentMainScreenBinding>(FragmentMainScreenBinding::inflate) {

    // TODO: Fullscreen support (w-landscape)
    // TODO: Catch intent for opening images
    // TODO: Merge cursors to gen INTERNAL/EXTERNAL queries ?
    // TODO: Tutorial (first time)
    // TODO: Auto "next"
    // TODO: Carousel background

    // (?)
    // TODO: OnResume (reload filters ?)
    // TODO: Update ui in loading mode
    // TODO: Create data folders (by size, by ratio, ecc)
    // TODO: Query to find old position (cause may change if user remove inner elements)

    companion object {
        const val V_SLIDE_TRIGGER = 0.75 // Percentage
        const val H_SLIDE_TRIGGER = 0.45 // Percentage

        const val DIRECTION_NONE = 0
        const val DIRECTION_HORIZONTAL = 1
        const val DIRECTION_VERTICAL = 2

        const val ACTION_NONE = 0
        const val ACTION_HOR_SWIPE = 1
        const val ACTION_OPEN_FILTERS = 2
        const val ACTION_OPEN_DETAILS = 3

        const val OVERLAY_CLOSE_DELAY = 5000L
    }

    override fun onResume() {
        super.onResume()
        loadImageListAsync(false)
    }

    private val viewModel by navGraphViewModels<MainScreenViewModel>(R.id.main_navigation) { defaultViewModelProviderFactory }

    private val gestureDetector by lazy { GestureDetectorCompat(requireContext(), gestureListener) }

    private val isFiltersContainerDown: Boolean
        get() {
            return abs(binding.filtersContainer.translationY).toInt() ==
                    binding.filtersContainer.height
        }

    private val isDetailsContainerDown: Boolean
        get() {
            return abs(binding.detailsContainer.translationY).toInt() ==
                    binding.detailsContainer.height
        }

    override fun setup(savedInstanceState: Bundle?) {
        setupUI()
        setupObservers()
        loadFiltersAsync()
    }

    private fun setupUI() {
        with(binding) {
            // Filters
            filtersContainer.children.forEach { child ->
                if (child is TextInputLayout) {
                    child.editText!!.setText("loading...")
                    child.editText!!.inputType = InputType.TYPE_NULL
                }
            }

            // Filters: Album
            (filtersAlbum.editText as AutoCompleteTextView)
                .setOnItemClickListener { _, _, i, _ ->
                    viewModel.updateSelectedAlbum(i)
                }

            // Results: Carousel
            imagesList.adapter = GenericRecyclerAdapterWithCursor(
                context = requireContext(),
                handler = CarouselImageAdapterItemHandlerWithCursor(),
                cursor = null,
            )
            imagesList.setOnTouchListener { _, event ->
                // Check for scroll ended
                if (event.action == MotionEvent.ACTION_UP) gestureListener.onUp(event)
                // Forward to gesture detector
                gestureDetector.onTouchEvent(event)
                // Always consume event to "disable" standard interactions
                return@setOnTouchListener true
            }
            imagesList.adapter!!.onPositionChangedListener<ImageData> { _, data ->
                detailsUri.editText!!.setText(data?.uri.toString())
                detailsWidth.editText!!.setText(data?.width.toString())
                detailsHeight.editText!!.setText(data?.height.toString())
                detailsSize.editText!!.setText(data?.size.toSizeWithUnit())
                detailsMime.editText!!.setText(data?.mime)

                detailsUri.setEndIconOnClickListener(null)
                if (data == null) return@onPositionChangedListener

                detailsUri.setEndIconOnClickListener {
                    startActivity(
                        Intent.createChooser(
                            Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_STREAM, data.uri)
                                type = data.mime
                            },
                            null
                        )
                    )
                }
            }

            // Details
            detailsContainer.children.forEach { child ->
                if (child is TextInputLayout) {
                    child.editText!!.setText("loading...")
                    child.editText!!.inputType = InputType.TYPE_NULL
                }
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


        // Reload event requested.
        // Sent every time a new search is requested.
        viewModel.reload.observe(viewLifecycleOwner) {
            // Create and send cursor to view model
            loadImageListAsync(true)

            // Save filters
            viewModel.saveFilters()
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

    private fun loadImageListAsync(resetPosition: Boolean) {
        // Exit if filters are not loaded yet
        if (!viewModel.hasLoadedFilters) return

        // Retrieve filters
        val filters = viewModel.filters

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

        // Update cursor
        val isEmpty = (query.count == 0)
        binding.imagesListPlaceholder.isVisible = isEmpty
        binding.imagesListText.editText!!.setText(query.count.toString())
        val position = binding.imagesList.adapter!!.replaceCursor(query, resetPosition)
        binding.imagesList.scrollToPosition(position)
    }

    private fun updateUI(oldFilters: ImageSearchFilters, filtersData: ImageSearchFiltersData) {
        // Update corresponding UI elements
        with(binding) {
            // Insert old values (or default ones)
            // Album
            (filtersAlbum.editText as MaterialAutoCompleteTextView)
                .setText(oldFilters.bucket.displayName, false)
            // Size min
            (filtersSizeMin.editText as MaterialAutoCompleteTextView)
                .setText(oldFilters.sizeMin.displayName, false)
            // Size max
            (filtersSizeMax.editText as MaterialAutoCompleteTextView)
                .setText(oldFilters.sizeMax.displayName, false)
            // Mime
            (filtersMime.editText as MaterialAutoCompleteTextView)
                .setText(oldFilters.mime.displayName, false)

            // Fill the album dropdown
            (binding.filtersAlbum.editText as MaterialAutoCompleteTextView)
                .setSimpleItems(
                    filtersData.buckets.map(ImageSearchFilterBucket::displayName).toTypedArray()
                )

            // Fill the size (min / max) dropdown
            (binding.filtersSizeMin.editText as MaterialAutoCompleteTextView)
                .setSimpleItems(
                    filtersData.sizes.map(ImageSearchFilterSize::displayName).toTypedArray()
                )
            (binding.filtersSizeMax.editText as MaterialAutoCompleteTextView)
                .setSimpleItems(
                    filtersData.sizes.map(ImageSearchFilterSize::displayName).toTypedArray()
                )

            // Fill the mime dropdown
            (binding.filtersMime.editText as MaterialAutoCompleteTextView)
                .setSimpleItems(
                    filtersData.mimes.map(ImageSearchFilterMime::displayName).toTypedArray()
                )
        }
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {

        private var closingFiltersContainerTask: TimerTask? = null
        private var closingDetailsContainerTask: TimerTask? = null

        private var swipeDirection = DIRECTION_NONE
        private var action = ACTION_NONE

        override fun onDown(e: MotionEvent?): Boolean {
            // Clear flags
            swipeDirection = DIRECTION_NONE
            action = ACTION_NONE

            return super.onDown(e)
        }

        fun onUp(e: MotionEvent?) {
            // "Spring" effect for carousel
            if (action != ACTION_HOR_SWIPE) {
                binding.imagesList.smoothScrollToPosition(binding.imagesList.adapter!!.position())
            }

            // "Spring" effect for overlays
            if (action == ACTION_NONE && swipeDirection == DIRECTION_VERTICAL) {
                // Hide only if it was shown
                if (!isFiltersContainerDown) {
                    binding.filtersContainer.animate().translationY(0F)
                }

                // Hide only if it was shown
                if (!isDetailsContainerDown) {
                    binding.detailsContainer.animate().translationY(0F)
                }
            }
        }

        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            // Cancel auto-close tasks (if any)
            closingFiltersContainerTask?.cancel()
            closingDetailsContainerTask?.cancel()

            // Hide containers
            binding.filtersContainer.animate().translationY(0F)
            binding.detailsContainer.animate().translationY(0F)

            return super.onSingleTapConfirmed(e)
        }

        // https://developer.android.com/reference/android/view/GestureDetector.OnGestureListener
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, cdx: Float, cdy: Float): Boolean {
            // Exit if already "processed"
            if (action != ACTION_NONE) return true

            // Cannot be null
            val p1 = e1!!
            val p2 = e2!!

            // Compute delta and absolute delta
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val adx = abs(dx)
            val ady = abs(dy)

            // User is sliding horizontally
            if (adx > ady && swipeDirection != DIRECTION_VERTICAL) {
                // Update sliding direction
                swipeDirection = DIRECTION_HORIZONTAL

                // Check distance
                if (adx > H_SLIDE_TRIGGER * binding.imagesList.width) {
                    if (dx < 0) {
                        // Log.d("SCROLL", "Right to Left")

                        // Focus "next" image for carousel
                        binding.imagesList.smoothScrollToPosition(
                            binding.imagesList.adapter!!.nextPosition(false)
                        )
                    } else {
                        // Log.d("SCROLL", "Left to Right")

                        // Focus "prev" image for carousel
                        binding.imagesList.smoothScrollToPosition(
                            binding.imagesList.adapter!!.prevPosition(false)
                        )
                    }
                    // Declare action done
                    action = ACTION_HOR_SWIPE
                }
                // Scroll to show "responsiveness"
                else {
                    // Simple offset scroll that match user input delta
                    (binding.imagesList.layoutManager as LinearLayoutManager)
                        .scrollToPositionWithOffset(
                            binding.imagesList.adapter!!.position(),
                            dx.toInt()
                        )
                }
            }
            // User is sliding vertically
            else if (swipeDirection != DIRECTION_HORIZONTAL) {
                // Update sliding direction
                swipeDirection = DIRECTION_VERTICAL

                // Check vertical direction
                if (dy > 0) {
                    // Exit if already down
                    if (isFiltersContainerDown) return true

                    // Check distance
                    if (ady > V_SLIDE_TRIGGER * binding.filtersContainer.height) {
                        // Log.d("SCROLL", "Top to Bottom")

                        // Fully show container
                        binding.filtersContainer.translationY =
                            binding.filtersContainer.height.toFloat()

                        // Declare action done
                        action = ACTION_OPEN_FILTERS

                        // Cancel auto-close task (if any)
                        closingFiltersContainerTask?.cancel()

                        // Create new auto-close task
                        closingFiltersContainerTask = Timer("Filters Container close", false)
                            .schedule(OVERLAY_CLOSE_DELAY) {
                                // Log.d("AUTO-CLOSE", "Closing filters")

                                // Close container
                                binding.filtersContainer.animate().translationY(0F)
                            }
                    }
                    // Scroll to show "responsiveness"
                    else {
                        // Simple offset scroll that match user input delta
                        binding.filtersContainer.translationY = ady
                    }
                } else {
                    // Exit if already down
                    if (isDetailsContainerDown) return true

                    // Check distance
                    if (ady > V_SLIDE_TRIGGER * binding.detailsContainer.height) {
                        // Log.d("SCROLL", "Bottom to Top")

                        // Fully show container
                        binding.detailsContainer.translationY =
                            -binding.detailsContainer.height.toFloat()

                        // Declare action done
                        action = ACTION_OPEN_DETAILS

                        // Cancel auto-close task (if any)
                        closingDetailsContainerTask?.cancel()

                        // Create new auto-close task
                        closingDetailsContainerTask = Timer("Details Container close", false)
                            .schedule(OVERLAY_CLOSE_DELAY) {
                                // Log.d("AUTO-CLOSE", "Closing details")

                                // Close container
                                binding.detailsContainer.animate().translationY(0F)
                            }
                    }
                    // Scroll to show "responsiveness"
                    else {
                        // Simple offset scroll that match user input delta
                        binding.detailsContainer.translationY = -ady
                    }
                }
            }

            return true
        }
    }
}