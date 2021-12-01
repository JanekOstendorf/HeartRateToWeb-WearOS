package io.ostendorf.heartratetoweb

import android.Manifest
import android.app.Activity
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.widget.doAfterTextChanged
import io.ostendorf.heartratetoweb.databinding.ActivityMainBinding

class MainActivity : Activity() {

    object Config {
        const val CONF_HTTP_HOSTNAME = "HTTP_HOSTNAME"
        const val CONF_HTTP_PORT = "HTTP_PORT"

        const val CONF_HTTP_HOSTNAME_DEFAULT = ""
        const val CONF_HTTP_PORT_DEFAULT = 6547
    }

    private lateinit var textCurrentHr: TextView
    private lateinit var binding: ActivityMainBinding
    //private lateinit var textCurrentHr: TextView

    private lateinit var preferences: SharedPreferences

    private var runningState: RunningState = RunningState.STOPPED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initConfig()
        bindConfigToInputs()

        textCurrentHr = findViewById(R.id.textViewHeartRate)
        textCurrentHr.text = resources.getString(R.string.text_current_hr, 0)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), 100)
        }

        wireButton()

    }

    private fun wireButton() {
        val buttonInput = findViewById<Button>(R.id.buttonStartStop)

        buttonInput.setOnClickListener {
            if (runningState == RunningState.STOPPED) {
                runningState = RunningState.RUNNING
                buttonInput.setText(R.string.button_stop)
                startMeasure()
            } else if (runningState == RunningState.RUNNING) {
                runningState = RunningState.STOPPED
                buttonInput.setText(R.string.button_start)
                stopMeasure()
            }
        }
    }

    private fun bindConfigToInputs() {
        val hostnameInput = findViewById<EditText>(R.id.editTextHostname)
        val portInput = findViewById<EditText>(R.id.editTextPort)

        hostnameInput.setText(
            preferences.getString(
                Config.CONF_HTTP_HOSTNAME,
                Config.CONF_HTTP_HOSTNAME_DEFAULT
            )
        )
        portInput.setText(
            preferences.getInt(Config.CONF_HTTP_PORT, Config.CONF_HTTP_PORT_DEFAULT).toString()
        )

        hostnameInput.doAfterTextChanged {
            with(preferences.edit()) {
                putString(Config.CONF_HTTP_HOSTNAME, it.toString())
                apply()
                Log.d("config", "Saved new Hostname: " + it.toString())
            }
        }

        portInput.doAfterTextChanged {
            with(preferences.edit()) {
                putInt(Config.CONF_HTTP_PORT, it.toString().toInt())
                apply()
                Log.d("config", "Saved new Port: " + it.toString())
            }
        }
    }

    private fun initConfig() {
        preferences = this.getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)

        with(preferences.edit()) {
            if (!preferences.contains(Config.CONF_HTTP_HOSTNAME)) {
                putString(Config.CONF_HTTP_HOSTNAME, Config.CONF_HTTP_HOSTNAME_DEFAULT)
            }

            if (!preferences.contains(Config.CONF_HTTP_PORT)) {
                putInt(Config.CONF_HTTP_PORT, Config.CONF_HTTP_PORT_DEFAULT)
            }
            apply()
        }
    }

    fun updateHeartRate(heartrate: Int) {
        textCurrentHr.text = resources.getString(R.string.text_current_hr, heartrate)
    }

    private fun startMeasure() {
        Log.d("service", "Starting foreground heart rate service ...")
        HeartRateService.startService(this)
    }

    private fun stopMeasure() {
        Log.d("service", "Stopping foreground heart rate service ...")
        HeartRateService.stopService(this)
    }

}
