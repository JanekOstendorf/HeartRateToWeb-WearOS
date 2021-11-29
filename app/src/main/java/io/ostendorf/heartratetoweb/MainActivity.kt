package io.ostendorf.heartratetoweb

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import com.android.volley.RequestQueue
import com.android.volley.request.StringRequest
import com.android.volley.toolbox.Volley
import io.ostendorf.heartratetoweb.databinding.ActivityMainBinding
import kotlin.math.roundToInt

class MainActivity : Activity(), SensorEventListener {

    object Config {
        const val CONF_HTTP_HOSTNAME = "HTTP_HOSTNAME"
        const val CONF_HTTP_PORT = "HTTP_PORT"

        const val CONF_HTTP_HOSTNAME_DEFAULT = ""
        const val CONF_HTTP_PORT_DEFAULT = 6547
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var mSensorManager: SensorManager
    private lateinit var mHeartRateSensor: Sensor
    private lateinit var textCurrentHr: TextView

    private lateinit var httpQueue: RequestQueue

    private lateinit var sharedPreferences: SharedPreferences

    private var runningState: RunningState = RunningState.STOPPED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        textCurrentHr = findViewById(R.id.textViewHeartRate)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        initConfig()
        bindConfigToInputs()

        textCurrentHr.text = resources.getString(R.string.text_current_hr, 0)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), 100)
        }

        httpQueue = Volley.newRequestQueue(this)

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

        hostnameInput.setText(sharedPreferences.getString(Config.CONF_HTTP_HOSTNAME, Config.CONF_HTTP_HOSTNAME_DEFAULT))
        portInput.setText(sharedPreferences.getInt(Config.CONF_HTTP_PORT, Config.CONF_HTTP_PORT_DEFAULT).toString())

        hostnameInput.doAfterTextChanged {
            with(sharedPreferences.edit()) {
                putString(Config.CONF_HTTP_HOSTNAME, it.toString())
                apply()
                Log.d("config", "Saved new Hostname: " + it.toString())
            }
        }

        portInput.doAfterTextChanged {
            with(sharedPreferences.edit()) {
                putInt(Config.CONF_HTTP_PORT, it.toString().toInt())
                apply()
                Log.d("config", "Saved new Port: " + it.toString())
            }
        }
    }

    private fun initConfig() {
        sharedPreferences = this.getPreferences(MODE_PRIVATE)

        with(sharedPreferences.edit()) {
            if (!sharedPreferences.contains(Config.CONF_HTTP_HOSTNAME)) {
                putString(Config.CONF_HTTP_HOSTNAME, Config.CONF_HTTP_HOSTNAME_DEFAULT)
            }

            if (!sharedPreferences.contains(Config.CONF_HTTP_PORT)) {
                putInt(Config.CONF_HTTP_PORT, Config.CONF_HTTP_PORT_DEFAULT)
            }
            apply()
        }
    }

    private fun startMeasure() {
        val sensorRegistered: Boolean = mSensorManager.registerListener(
            this,
            mHeartRateSensor,
            SensorManager.SENSOR_DELAY_FASTEST
        )
        Log.d("Sensor Status:", " Sensor registered: " + (if (sensorRegistered) "yes" else "no"))
    }

    private fun stopMeasure() {
        mSensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val mHeartRateFloat: Float = event!!.values[0]

        val mHeartRate: Int = mHeartRateFloat.roundToInt()
        textCurrentHr.text = resources.getString(R.string.text_current_hr, mHeartRate)
        Log.d("HR: ", mHeartRate.toString())

        sendHeartRate(mHeartRate)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // ignored
    }

    private fun sendHeartRate(heartrate: Int) {

        val httpUrl = "http://" +
                sharedPreferences.getString(Config.CONF_HTTP_HOSTNAME, Config.CONF_HTTP_HOSTNAME_DEFAULT) +
                ":" + sharedPreferences.getInt(Config.CONF_HTTP_PORT, Config.CONF_HTTP_PORT_DEFAULT).toString()

        val httpRequest = object : StringRequest(
            Method.POST,
            httpUrl,
            { response -> Log.d("HTTP Reponse: ", response) },
            { Log.e("HTTP Error", it.message.toString()) }
        ) {
            override fun getBodyContentType(): String {
                return "application/x-www-form-urlencoded; charset=UTF-8"
            }

//            override fun getParams(): MutableMap<String, String> {
//                return hashMapOf("rate" to heartrate.toString())
//            }

            override fun getBody(): ByteArray {
                return ("rate=$heartrate").toByteArray(Charsets.UTF_8)
            }
        }

        httpQueue.add(httpRequest)
    }
}
