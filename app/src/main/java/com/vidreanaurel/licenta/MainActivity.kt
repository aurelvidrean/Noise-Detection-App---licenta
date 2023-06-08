package com.vidreanaurel.licenta

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.vidreanaurel.licenta.fragments.ChartsFragment
import com.vidreanaurel.licenta.fragments.DetailsFragment
import com.vidreanaurel.licenta.fragments.MainFragment


const val TIMER_ACTION = "TimerAction"
class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var mapFragment: MainFragment
    private lateinit var detailsFragment: DetailsFragment
    private lateinit var chartsFragment: ChartsFragment

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
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

        mapFragment = MainFragment.newInstance()
        detailsFragment = DetailsFragment.newInstance()
        chartsFragment = ChartsFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, mapFragment)
            .add(R.id.fragment_container, detailsFragment)
            .add(R.id.fragment_container, chartsFragment)
            .hide(detailsFragment)
            .hide(chartsFragment)
            .commit()
        bottomNavigationView = findViewById(R.id.bottomnav)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) -> {
                navigateToAudioFragment()
            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun navigateToAudioFragment() {
        supportFragmentManager.beginTransaction().hide(detailsFragment).show(mapFragment).commit()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.map -> supportFragmentManager.beginTransaction().hide(detailsFragment).hide(chartsFragment).show(mapFragment).commit()
            R.id.details -> supportFragmentManager.beginTransaction().hide(mapFragment).hide(chartsFragment).show(detailsFragment).commit()
            R.id.charts -> supportFragmentManager.beginTransaction().hide(mapFragment).hide(detailsFragment).show(chartsFragment).commit()
        }
        return true
    }
}