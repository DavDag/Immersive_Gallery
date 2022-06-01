package it.unipi.di.sam.immersivegallery.ui.main

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.opengl.GLSurfaceView
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
import kotlin.math.abs
import kotlin.math.min

@AndroidEntryPoint
class MainScreenFragment :
    BaseFragment<FragmentMainScreenBinding>(FragmentMainScreenBinding::inflate) {

    // TODO: Add colors
    // TODO: Add string
    // TODO: Add dimens
    // TODO: Gray => White (UI)
    // TODO: Fullscreen support (w-landscape => custom layout for landscape)

    // (High Priority)
    // TODO: Carousel background (adjust to transparent when with => placeholder !)
    // TODO: Catch intent for opening images

    // (Low Priority)
    // TODO: Tutorial (first time)
    // TODO: Auto "next"
    // TODO: Merge cursors to gen INTERNAL/EXTERNAL queries ?

    // (?)
    // TODO: OnResume (reload filters ?)
    // TODO: Create data folders (by size, by ratio, ecc)
    // TODO: Query to find old position (cause may change if user remove inner elements) (id changes ?)

    companion object {
        const val V_SLIDE_TRIGGER = 0.75F // Percentage
        const val H_SLIDE_TRIGGER = 0.6F // Percentage
        const val SPRING_MAX_DELTA = 0.1F // Percentage

        const val DIRECTION_NONE = 0
        const val DIRECTION_HORIZONTAL = 1
        const val DIRECTION_VERTICAL = 2

        const val ACTION_NONE = 0
        const val ACTION_HOR_SWIPE = 1
        const val ACTION_OPEN_FILTERS = 2
        const val ACTION_OPEN_DETAILS = 3

        const val OVERLAY_CLOSE_DELAY = 15000L

        val IMAGE_DATA_QUERY_COLUMNS = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        ).toTypedArray()
    }

    private val DATE_FORMAT by lazy { SimpleDateFormat("dd/MM/yyyy", currentLocale) }

    override fun onResume() {
        super.onResume()
        enterFullScreen()
        loadImageListAsync(false)
    }

    private val viewModel by navGraphViewModels<MainScreenViewModel>(R.id.main_navigation) { defaultViewModelProviderFactory }

    private val gestureDetector by lazy { GestureDetectorCompat(requireContext(), gestureListener) }

    private val isFiltersContainerDown: Boolean
        get() {
            return abs(binding.filtersContainer.translationY).toInt() >=
                    binding.filtersContainer.height
        }

    private val isDetailsContainerDown: Boolean
        get() {
            return abs(binding.detailsContainer.translationY).toInt() >=
                    binding.detailsContainer.height
        }

    private var autoCloseFilters = RestartableAsyncTask(
        name = "Filters Container close",
        delay = OVERLAY_CLOSE_DELAY,
    ) {
        // Log.d("AUTO-CLOSE", "Closing filters")
        binding.filtersContainer.animate().translationY(0F)
    }

    private var autoCloseDetails = RestartableAsyncTask(
        name = "Details Container close",
        delay = OVERLAY_CLOSE_DELAY,
    ) {
        // Log.d("AUTO-CLOSE", "Closing details")
        binding.detailsContainer.animate().translationY(0F)
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
                    child.editText!!.inputType = InputType.TYPE_NULL
                }
            }
            filtersContainerOverlay.setOnTouchListener { _, _ ->
                autoCloseFilters.restart()
                return@setOnTouchListener false
            }

            // Filters: Album
            (filtersAlbum.editText as AutoCompleteTextView)
                .setOnItemClickListener { _, _, i, _ ->
                    viewModel.updateSelectedAlbum(i)
                }

            // Filters: Size (min/max)
            (filtersSizeMin.editText as AutoCompleteTextView)
                .setOnItemClickListener { _, _, i, _ ->
                    viewModel.updateSelectedSizeMin(i)
                }
            (filtersSizeMax.editText as AutoCompleteTextView)
                .setOnItemClickListener { _, _, i, _ ->
                    viewModel.updateSelectedSizeMax(i)
                }

            // Filters: Mime
            (filtersMime.editText as AutoCompleteTextView)
                .setOnItemClickListener { _, _, i, _ ->
                    viewModel.updateSelectedMime(i)
                }

            // Results: Carousel
            imagesList.adapter = GenericRecyclerAdapterWithCursor(
                context = requireContext(),
                handler = CarouselImageAdapterItemHandlerWithCursor(),
                cursor = null,
            )
            imagesList.adapter!!.onPositionChangedListener<ImageData> { _, data ->
                updateDetailsState(false)
                updateDetails(data)
            }
            imagesList.setOnTouchListener { _, event ->
                // Check for scroll ended
                if (event.action == MotionEvent.ACTION_UP ||
                    event.action == MotionEvent.ACTION_OUTSIDE ||
                    event.action == MotionEvent.ACTION_CANCEL ||
                    event.action == MotionEvent.ACTION_POINTER_UP
                ) {
                    gestureListener.onUp(event)
                }
                // Forward to gesture detector
                gestureDetector.onTouchEvent(event)
                // Always consume event to "disable" standard interactions
                return@setOnTouchListener true
            }
            imagesList.setHasFixedSize(true)

            // Placeholder
            imagesListPlaceholder.children.forEach { child ->
                if (child is TextInputLayout) {
                    child.editText!!.inputType = InputType.TYPE_NULL
                }
            }

            // Details
            detailsContainer.children.forEach { child ->
                if (child is TextInputLayout) {
                    child.editText!!.inputType = InputType.TYPE_NULL
                }
            }
            detailsContainerOverlay.setOnTouchListener { _, _ ->
                autoCloseDetails.restart()
                return@setOnTouchListener false
            }

            updateFiltersState(true)
            updateDetailsState(true)
        }
        setupDynamicBackground()
    }

    private fun setupObservers() {
        // Filters loaded event.
        // (sent once after first async load)
        viewModel.filtersData.observe(viewLifecycleOwner) {
            // Toggle loading state
            updateFiltersState(false)

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
            // Toggle loading state
            updateDetailsState(true)

            // Create and send cursor to view model
            loadImageListAsync(true)

            // Save filters
            viewModel.saveFilters()
        }
    }

    private fun loadFiltersAsync() {
        // Toggle loading state
        updateFiltersState(true)

        // Send request to read filters from shared preferences
        viewModel.getOldFilters()

        // Create cursor to retrieve filters data
        val query = ContentResolverCompat.query(
            requireContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE,
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
        val selection = StringBuilder("1")
        val selectionArgs = mutableListOf<String>()

        // Check if user has a bucket specified
        filters.bucket
            .takeIf { bucket -> bucket.bucketId != ALL_BUCKET_FILTER.bucketId }
            ?.let { bucket ->
                selection.append(" AND ${MediaStore.Images.Media.BUCKET_ID} = ?")
                selectionArgs.add(bucket.bucketId.toString())
            }

        // Check if user has a size (min) specified
        filters.sizeMin
            .takeIf { size -> size.bytes != ZERO_SIZE_FILTER.bytes }
            ?.let { size ->
                selection.append(" AND ${MediaStore.Images.Media.SIZE} >= ?")
                selectionArgs.add(size.bytes.toString())
            }

        // Check if user has a size (max) specified
        filters.sizeMax
            .takeIf { size -> size.bytes != INF_SIZE_FILTER.bytes }
            ?.let { size ->
                selection.append(" AND ${MediaStore.Images.Media.SIZE} <= ?")
                selectionArgs.add(size.bytes.toString())
            }

        // Check if user has a mime type specified
        filters.mime
            .takeIf { mime -> mime.type != ALL_MIME_FILTER.type }
            ?.let { mime ->
                selection.append(" AND ${MediaStore.Images.Media.MIME_TYPE} = ?")
                selectionArgs.add(mime.type)
            }

        // Create cursor to retrieve images data
        val query = ContentResolverCompat.query(
            requireContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            IMAGE_DATA_QUERY_COLUMNS,
            selection.toString(),
            selectionArgs.toTypedArray(),
            null,
            null,
        )

        // Update UI
        updateResults(query.count)

        // Update cursor
        val position = binding.imagesList.adapter!!.replaceCursor(query, resetPosition)
        binding.imagesList.scrollToPosition(position)
    }

    private fun updateUI(oldFilters: ImageSearchFilters, filtersData: ImageSearchFiltersData) {
        // Update corresponding UI elements
        with(binding) {
            // Insert old values (or default ones)
            // Album
            filtersAlbum.editText!!.setText(oldFilters.bucket.displayName)
            // Size min
            filtersSizeMin.editText!!.setText(oldFilters.sizeMin.displayName)
            // Size max
            filtersSizeMax.editText!!.setText(oldFilters.sizeMax.displayName)
            // Mime
            filtersMime.editText!!.setText(oldFilters.mime.displayName)

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

    private fun updateDetails(data: ImageData?) {
        with(binding) {
            detailsUri.editText!!.setText(data?.uri.toString())
            detailsWidth.editText!!.setText(data?.width.toString())
            detailsHeight.editText!!.setText(data?.height.toString())
            detailsSize.editText!!.setText(data?.size.toSizeWithUnit())
            detailsMime.editText!!.setText(data?.mime.toString())
            detailsDateTaken.editText!!.setText(data?.dataTaken.toDateTime(DATE_FORMAT))
            detailsDateModified.editText!!.setText(data?.dataModified.toDateTime(DATE_FORMAT))

            detailsUri.setEndIconOnClickListener(null)
            if (data == null) return

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
    }

    private fun updateFiltersState(loading: Boolean) {
        if (loading) {
            binding.imagesListPlaceholderText.editText!!.setText("Loading filters...")
            binding.filtersContainer.children.forEach { child ->
                if (child is TextInputLayout) {
                    child.editText!!.setText("loading...")
                }
            }
        }
    }

    private fun updateDetailsState(loading: Boolean) {
        if (loading) {
            binding.detailsContainer.children.forEach { child ->
                if (child is TextInputLayout) {
                    child.editText!!.setText("loading...")
                }
            }
        }
    }

    private fun updateResults(count: Int) {
        val isEmpty = (count == 0)

        with(binding) {
            imagesListPlaceholderText.editText!!.setText("No results available :(")
            imagesListPlaceholder.isVisible = isEmpty
            imagesListText.editText!!.setText(count.toString())
        }

        updateDetails(null)
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {

        private var swipeDirection = DIRECTION_NONE
        private var action = ACTION_NONE
        private var viewOriginF = 0F
        private var viewOriginD = 0F
        private var scrollDistance = 0

        override fun onDown(e: MotionEvent?): Boolean {
            // Clear flags
            swipeDirection = DIRECTION_NONE
            action = ACTION_NONE
            viewOriginF = 0F
            viewOriginD = 0F
            scrollDistance = 0

            return super.onDown(e)
        }

        fun onUp(e: MotionEvent?) {
            // "Spring" effect for carousel
            if (action != ACTION_HOR_SWIPE) {
                // Smooth scroll does not always work
                binding.imagesList.scrollToPosition(
                    binding.imagesList.adapter!!.position()
                )
            }

            // "Spring" effect for overlays
            if (action == ACTION_NONE && swipeDirection == DIRECTION_VERTICAL) {
                // TODO: Vertical swipe close the *other* container
                binding.filtersContainer.animate().translationY(viewOriginF)
                binding.detailsContainer.animate().translationY(viewOriginD)
            }
        }

        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            // Cancel auto-close tasks (if any)
            autoCloseFilters.cancel()
            autoCloseDetails.cancel()

            // Hide containers
            binding.filtersContainer.animate().translationY(0F)
            binding.detailsContainer.animate().translationY(0F)

            return super.onSingleTapConfirmed(e)
        }

        override fun onLongPress(e: MotionEvent?) {
            super.onLongPress(e)
            toggleFullScreen()
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
                    scrollDistance = 0
                }
                // Scroll to show "responsiveness"
                else {
                    // Simple offset scroll that match user input delta
                    (binding.imagesList.layoutManager as LinearLayoutManager)
                        .scrollToPositionWithOffset(
                            binding.imagesList.adapter!!.position(),
                            dx.toInt()
                        )
                    scrollDistance = adx.toInt()
                }
            }
            // User is sliding vertically
            else if (swipeDirection != DIRECTION_HORIZONTAL) {
                // Update sliding direction
                swipeDirection = DIRECTION_VERTICAL

                // Check vertical direction
                if (dy > 0) {
                    // "Spring" effect
                    if (isFiltersContainerDown) {
                        // Cap delta
                        val mx = SPRING_MAX_DELTA * binding.filtersContainer.height
                        val delta = min(ady, mx)

                        // Fully show container
                        binding.filtersContainer.translationY =
                            binding.filtersContainer.height.toFloat() + delta

                        // Declare action done
                        // action = ACTION_OPEN_DETAILS
                        viewOriginF = binding.filtersContainer.height.toFloat()

                        // Reset auto-close timer
                        autoCloseFilters.restart()
                    } else {
                        // Check distance
                        if (ady > V_SLIDE_TRIGGER * binding.filtersContainer.height) {
                            // Log.d("SCROLL", "Top to Bottom")

                            // Fully show container
                            binding.filtersContainer.translationY =
                                binding.filtersContainer.height.toFloat()

                            // Declare action done
                            action = ACTION_OPEN_FILTERS
                            viewOriginF = binding.filtersContainer.height.toFloat()

                            // Reset auto-close timer
                            autoCloseFilters.restart()
                        }
                        // Scroll to show "responsiveness"
                        else {
                            // Simple offset scroll that match user input delta
                            binding.filtersContainer.translationY = viewOriginF + ady
                            viewOriginF = 0F
                        }
                    }
                } else {
                    // "Spring" effect
                    if (isDetailsContainerDown) {
                        // Cap delta
                        val mx = SPRING_MAX_DELTA * binding.detailsContainer.height
                        val delta = min(ady, mx)

                        // Fully show container
                        binding.detailsContainer.translationY =
                            -binding.detailsContainer.height.toFloat() - delta

                        // Declare action done
                        // action = ACTION_OPEN_DETAILS
                        viewOriginD = -binding.detailsContainer.height.toFloat()

                        // Reset auto-close timer
                        autoCloseDetails.restart()
                    } else {
                        // Check distance
                        if (ady > V_SLIDE_TRIGGER * binding.detailsContainer.height) {
                            // Log.d("SCROLL", "Bottom to Top")

                            // Fully show container
                            binding.detailsContainer.translationY =
                                -binding.detailsContainer.height.toFloat()

                            // Declare action done
                            action = ACTION_OPEN_DETAILS
                            viewOriginD = -binding.detailsContainer.height.toFloat()

                            // Reset auto-close timer
                            autoCloseDetails.restart()
                        }
                        // Scroll to show "responsiveness"
                        else {
                            // Simple offset scroll that match user input delta
                            binding.detailsContainer.translationY = -ady
                            viewOriginD = 0F
                        }
                    }
                }
            }

            return true
        }
    }

    private fun setupDynamicBackground() {
        with(binding.dynamicBackground) {
            setEGLContextClientVersion(2)
            setRenderer(ImmersiveRenderer())
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }
}