package it.unipi.di.sam.immersivegallery.ui.main

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
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
    // TODO: On resume (?) reload cursor
    // TODO: Tutorial (first time)
    // TODO: Merge cursors to gen INTERNAL/EXTERNAL queries ?
    // TODO: Auto "next"

    companion object {
        const val V_SLIDE_TRIGGER = 0.75 // Percentage
        const val H_SLIDE_TRIGGER = 0.35 // Percentage

        const val DIRECTION_NONE = 0
        const val DIRECTION_HORIZONTAL = 1
        const val DIRECTION_VERTICAL = 2

        const val ACTION_NONE = 0
        const val ACTION_HOR_SWIPE = 1
        const val ACTION_OPEN_FILTERS = 2
        const val ACTION_OPEN_DETAILS = 3

        const val OVERLAY_CLOSE_DELAY = 2000L
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

            // Results: Counter
            imagesListText.editText!!.inputType = InputType.TYPE_NULL
            imagesListText.editText!!.setText("loading...")

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
                detailsSize.editText!!.setText(data?.size.toString())
                detailsMime.editText!!.setText(data?.mime.toString())

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
        binding.imagesList.adapter!!.replaceCursor(query)
        binding.imagesListText.editText!!.setText(query.count.toString())
        binding.imagesList.isVisible = (query.count != 0)
        binding.imagesListPlaceholder.isVisible = (query.count == 0)
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
                if (abs(binding.filtersContainer.translationY).toInt() != binding.filtersContainer.height) {
                    binding.filtersContainer.animate().translationY(0F)
                }

                // Hide only if it was shown
                if (abs(binding.detailsContainer.translationY).toInt() != binding.detailsContainer.height) {
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