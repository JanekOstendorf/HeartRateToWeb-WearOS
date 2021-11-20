package io.ostendorf.heartratetoweb

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.android.volley.RequestQueue
import com.android.volley.request.StringRequest
import com.android.volley.toolbox.Volley
import io.ostendorf.heartratetoweb.databinding.ActivityMainBinding
import kotlin.math.roundToInt

class MainActivity : Activity(), SensorEventListener {

    object Config {
        const val HTTP_POST_URL = "http://192.168.178.182:6547/"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var mSensorManager: SensorManager
    private lateinit var mHeartRateSensor: Sensor
    private lateinit var textView: TextView

    private lateinit var httpQueue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        textView = findViewById(R.id.text)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), 100)
        }

        httpQueue = Volley.newRequestQueue(this)

        startMeasure()

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
        textView.text = mHeartRate.toString()
        Log.d("HR: ", mHeartRate.toString());

        sendHeartRate(mHeartRate)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // ignored
    }

    private fun sendHeartRate(heartrate: Int) {

        val httpRequest = object : StringRequest(
            Method.POST,
            Config.HTTP_POST_URL,
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
