package it.unipi.di.sam.immersivegallery.ui.perm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import it.unipi.di.sam.immersivegallery.R
import it.unipi.di.sam.immersivegallery.common.BaseFragment
import it.unipi.di.sam.immersivegallery.databinding.FragmentPermReqBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PermReqFragment :
    BaseFragment<FragmentPermReqBinding>(FragmentPermReqBinding::inflate) {

    companion object {
        val PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    override fun setup(savedInstanceState: Bundle?) {
        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        with(binding) {
            loadingGroup.isVisible = true
            explanationGroup.isVisible = false

            explanationButton.setOnClickListener {
                requireActivity().finishAndRemoveTask()
            }
        }
    }

    private fun inflateNavGraph() {
        with(binding) {
            loadingTitle.editText!!.setText("Loading App...")
        }

        lifecycleScope.launch {
            delay(500L)
            (requireActivity().supportFragmentManager
                .findFragmentById(R.id.root_fragment_container) as NavHostFragment)
                .navController
                .setGraph(R.navigation.app_navigation)
        }
    }

    private fun checkPermissions() {
        sendPermissionRequest(
            PERMISSIONS
                .filter { requireActivity().checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
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
                val notGranted = it.any { entry -> !entry.value }
                if (notGranted) {
                    closeApplicationGracefullyForDeniedPermissions()
                } else {
                    inflateNavGraph()
                }
            }

        lifecycleScope.launch {
            delay(3000L)
            launcher.launch(permissions)
        }
    }

    private fun closeApplicationGracefullyForDeniedPermissions() {
        with(binding) {
            loadingGroup.isVisible = false
            explanationGroup.isVisible = true
        }
    }

}