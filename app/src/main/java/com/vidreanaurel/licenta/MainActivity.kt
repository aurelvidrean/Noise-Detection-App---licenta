package com.vidreanaurel.licenta

import SERVICE_COMMAND
import SoundLevelMeter
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.vidreanaurel.licenta.fragments.DetailsFragment
import com.vidreanaurel.licenta.fragments.MainFragment
import com.vidreanaurel.licenta.models.TimerState


const val TIMER_ACTION = "TimerAction"
class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var bottomNavigationView: BottomNavigationView

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

        sendCommandToForegroundService(TimerState.START)
    }

    private fun navigateToAudioFragment() {
        val fragment = MainFragment.newInstance()
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, fragment, "Main Fragment").commit()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        var fragment: Fragment? = null
        when (item.itemId) {
            R.id.map -> fragment = MainFragment.newInstance()
            R.id.details -> fragment = DetailsFragment.newInstance()
        }
        if (fragment != null) {
            loadFragment(fragment)
        }
        return true
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }

    // Foreground Service Methods

    private fun sendCommandToForegroundService(timerState: TimerState) {
        ContextCompat.startForegroundService(this, getServiceIntent(timerState))
       // mainViewModel.isForegroundServiceRunning = timerState != TimerState.STOP
    }

    private fun getServiceIntent(command: TimerState) =
        Intent(this, SoundLevelMeter::class.java).apply {
            putExtra(SERVICE_COMMAND, command as Parcelable)
        }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            sendCommandToForegroundService(TimerState.PAUSE)
        }
    }

    override fun onStart() {
        super.onStart()
        sendCommandToForegroundService(TimerState.START)
    }
}