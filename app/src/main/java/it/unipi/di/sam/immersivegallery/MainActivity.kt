package it.unipi.di.sam.immersivegallery

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import it.unipi.di.sam.immersivegallery.databinding.MainActivityBinding

class MainActivity : AppCompatActivity(R.layout.main_activity) {

    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}