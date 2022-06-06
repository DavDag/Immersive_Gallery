package it.unipi.di.sam.immersivegallery

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import it.unipi.di.sam.immersivegallery.databinding.MainActivityBinding

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        const val IDS_KEY = "ids"
    }

    private lateinit var binding: MainActivityBinding

    private var ids = emptyList<Int>()

    private var _isFullScreen = false
    public val isFullScreen
        get() = _isFullScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // To correctly save fullscreen for api >= 30
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            // https://developer.android.com/reference/android/view/Window#setDecorFitsSystemWindows(boolean)
            window.setDecorFitsSystemWindows(false)

            // https://developer.android.com/reference/android/view/WindowInsetsController#setSystemBarsBehavior(int)
            window.decorView.windowInsetsController?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            // Add listener to keep "isFullScreen" up to date
            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                _isFullScreen =
                    !insets.isVisible(
                        WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
                    )
                // Log.d("FS-ACT", _isFullScreen.toString())
                return@setOnApplyWindowInsetsListener view.onApplyWindowInsets(insets)
            }
        }

        // Intercept intents
        when {
            // Preview Single image
            intent?.action == Intent.ACTION_SEND
                    && intent?.type?.startsWith("image/") == true -> {
                // Read uri from intent
                (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
                    preview(listOf(it))
                }
            }
            // Preview Multiple images
            intent?.action == Intent.ACTION_SEND_MULTIPLE
                    && intent?.type?.startsWith("image/") == true -> {
                // Read uris from intent
                (intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM))?.let {
                    preview(it.mapNotNull { el -> el as Uri? })
                }
            }
            else -> {
                // Home screen
            }
        }

        addIds()
    }

    private fun preview(uris: List<Uri>) {
        Log.d("AC", uris.joinToString { uri -> uri.toString() })

        // Retrieve _ID from uri list
        ids = uris.map { uri ->
            // using cursors
            val cursor = contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media._ID),
                null,
                null,
                null
            )

            // Ensure cursor lifetime is always respected
            cursor?.use {
                // Check if element is found and retrieve _ID
                if (it.moveToFirst()) {
                    return@map cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                }
            }

            // Any error => Remove uri from list
            return@map null
        }.filterNotNull()

        Log.d("AC", ids.joinToString { id -> id.toString() })
    }

    private fun addIds() {
        intent.putIntegerArrayListExtra(IDS_KEY, ArrayList(ids))
    }
}