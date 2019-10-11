import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.OrientationEventListener
import android.widget.Toast
import androidx.core.app.NotificationCompat


class SensorTrackingService : Service(),SensorEventListener {
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.getType() == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] == 0.0f) {
                // near
                showToast("Near")
            } else {
                // far
                showToast("Far")

            }
        }
    }

    val TAG = SensorTrackingService::class.java.simpleName

    private val binder = LocalBinder()
    private var serviceCallbacks: ServiceCallbacks? = null

    inner class LocalBinder : Binder() {
        fun getService(): SensorTrackingService = this@SensorTrackingService
    }
    override fun onBind(intent: Intent): IBinder = binder
    interface ServiceCallbacks {
        fun unlockDoors()
    }

    fun setCallbacks(callbacks: ServiceCallbacks) {
        serviceCallbacks = callbacks
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "Service ended")
        mSensorManager.unregisterListener(this);
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "tempChannel"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Channel title",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("").build()

            startForeground(1, notification)
        }

        checkForSensorAvailabilty()
        checkForProximitySensor()
    }

    lateinit var mSensorManager: SensorManager
    lateinit var mProximity: Sensor

    private fun checkForProximitySensor() {

        mSensorManager =  getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);


    }


    private var isTrackingStarted = false

    private fun checkForSensorAvailabilty() {

        val mOrientationListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {


                when (orientation) {
                    in 365..379, in 0..15 -> {
                        isTrackingStarted = true
                    }

                    in 240..270 -> {
                        if (isTrackingStarted) {
                            isTrackingStarted = false
                            serviceCallbacks?.unlockDoors()
                        }

                    }
                    in 16..180 -> {
                        isTrackingStarted = false
                    }
                }
            }
        }
        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        } else {
            showToast("Rotational sensors not available for this device")
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

}
