package it.unipi.di.sam.immersivegallery

import android.os.Build
import android.os.Bundle
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

        window.setDecorFitsSystemWindows(false)
        window.decorView.windowInsetsController?.systemBarsBehavior =
            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                _isFullScreen =
                    !insets.isVisible(
                        WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
                    )
                // Log.d("FS-ACT", _isFullScreen.toString())
                return@setOnApplyWindowInsetsListener view.onApplyWindowInsets(insets)
            }
        }
    }
}