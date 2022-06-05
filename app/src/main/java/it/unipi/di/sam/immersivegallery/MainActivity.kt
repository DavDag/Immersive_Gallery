package it.unipi.di.sam.immersivegallery

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import it.unipi.di.sam.immersivegallery.databinding.MainActivityBinding

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainActivityBinding

    private var _isFullScreen = false
    public val isFullScreen
        get() = _isFullScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.decorView.windowInsetsController?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                _isFullScreen =
                    !insets.isVisible(
                        WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
                    )
                // Log.d("FS-ACT", _isFullScreen.toString())
                return@setOnApplyWindowInsetsListener view.onApplyWindowInsets(insets)
            }
        }

        when {
            intent?.action == Intent.ACTION_SEND
                    && intent?.type?.startsWith("image/") == true -> {
                // Preview *Single*
                (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
                    preview(listOf(it))
                }
            }
            intent?.action == Intent.ACTION_SEND_MULTIPLE
                    && intent?.type?.startsWith("image/") == true -> {
                // Preview *Multiple*
                (intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM))?.let {
                    preview(it.mapNotNull { el -> el as Uri? })
                }
            }
            else -> {
                // Home screen
                homescreen()
            }
        }
    }

    private fun preview(uris: List<Uri>) {
        Log.d("AC", uris.joinToString { uri -> uri.toString() })
    }

    private fun homescreen() {
        Log.d("AC", "HomeScreen")
    }
}