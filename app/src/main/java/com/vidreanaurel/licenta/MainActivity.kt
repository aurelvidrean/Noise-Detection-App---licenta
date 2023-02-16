package com.vidreanaurel.licenta

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.vidreanaurel.licenta.fragments.MainFragment

class MainActivity : AppCompatActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permission request granted", Toast.LENGTH_LONG).show()
                navigateToAudioFragment()
            } else {
                Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                navigateToAudioFragment()
            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO)
            }
        }


//        val fragment = MainFragment.newInstance()
//
//        if (savedInstanceState == null) {
//            supportFragmentManager.beginTransaction().add(R.id.fragment_container, fragment, "Main Fragment").commit()
//        }
    }

    private fun navigateToAudioFragment() {
        val fragment = MainFragment.newInstance()
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, fragment, "Main Fragment").commit()
    }

}