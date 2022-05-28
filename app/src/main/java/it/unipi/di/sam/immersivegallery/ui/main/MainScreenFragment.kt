package it.unipi.di.sam.immersivegallery.ui.main

import android.os.Bundle
import androidx.navigation.navGraphViewModels
import it.unipi.di.sam.immersivegallery.R
import it.unipi.di.sam.immersivegallery.common.BaseFragment
import it.unipi.di.sam.immersivegallery.databinding.FragmentMainScreenBinding

class MainScreenFragment :
    BaseFragment<FragmentMainScreenBinding>(FragmentMainScreenBinding::inflate) {

    private val viewModel: MainScreenViewModel by navGraphViewModels(R.id.main_navigation)

    override fun setupUI(savedInstanceState: Bundle?) {
        // TODO
    }

}