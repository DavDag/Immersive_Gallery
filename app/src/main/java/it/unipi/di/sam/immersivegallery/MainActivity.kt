package it.unipi.di.sam.immersivegallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import it.unipi.di.sam.immersivegallery.databinding.MainActivityBinding

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        val PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()
    }

    private fun inflateNavGraph() {
        (supportFragmentManager
            .findFragmentById(R.id.root_fragment_container) as NavHostFragment)
            .navController
            .setGraph(R.navigation.all_navigation)
    }

    private fun checkPermissions() {
        sendPermissionRequest(
            PERMISSIONS
                .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
                .toTypedArray()
        )
    }

    private fun sendPermissionRequest(permissions: Array<String>) {
        if (permissions.isEmpty()) {
            inflateNavGraph()
            return
        }

        val launcher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                val granted = it.any { entry -> entry.value }
                if (!granted) {
                    closeApplicationGracefullyForDeniedPermissions()
                }
                inflateNavGraph()
            }

        launcher.launch(permissions)
    }

    private fun closeApplicationGracefullyForDeniedPermissions() {
        val toast = Toast.makeText(
            applicationContext,
            "Permissions needed to proceed :(",
            Toast.LENGTH_SHORT
        )

        toast.addCallback(
            object : Toast.Callback() {
                override fun onToastHidden() = finishAndRemoveTask()
            }
        )

        toast.show()
    }
}