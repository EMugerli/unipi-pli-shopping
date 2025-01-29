package com.unipi.mobdev.unipiplishopping

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import com.unipi.mobdev.unipiplishopping.data.LoginDataSource
import com.unipi.mobdev.unipiplishopping.data.LoginRepository
import com.unipi.mobdev.unipiplishopping.data.model.Product
import com.unipi.mobdev.unipiplishopping.ui.product.ProductDetailActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    private val loginRepository = LoginRepository(LoginDataSource())

    override fun attachBaseContext(newBase: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(newBase)
        val languageCode = prefs.getString("language_preference", "en") ?: "en"
        val context = updateBaseContextLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }

    private fun updateBaseContextLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean("location_notifications", false)) {
            startLocationService()
        }

        applyTheme()
        applyTextSizePreference()
    }

    fun startLocationService() {
        // Start your location service/updates here
        Log.d("LocationService", "Starting location service")
        checkLocationForNotifications()
    }

    fun stopLocationService() {
        // Stop location updates here
        LocationServices.getFusedLocationProviderClient(this)
            .removeLocationUpdates(locationCallback)
    }

    // Add the location callback from previous implementation
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.lastLocation?.let { location ->
                checkNearbyStores(location)
            }
        }
    }

    /**
     * Check the user's location and show a notification if they are near a store
     */
    private fun checkLocationForNotifications() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, // Use Priority enum
            10000 // Interval in milliseconds
        ).apply {
            setMinUpdateIntervalMillis(5000) // Minimum interval
            setWaitForAccurateLocation(true)
        }.build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    checkNearbyStores(location)
                }
            }
        }, Looper.getMainLooper())
    }

    /**
     * Check if the user is near any stores and show a notification if they are
     */
    private fun checkNearbyStores(currentLocation: Location) {
        FirebaseFirestore.getInstance().collection("products")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val product = Product(
                        document.id,
                        document["name"] as String,
                        document.getDouble("price") as Double,
                        document["description"] as String,
                        document["release_date"] as com.google.firebase.Timestamp,
                        document["store_location"] as com.google.firebase.firestore.GeoPoint,
                        (document["store_id"] as Long).toInt()
                    )

                    val storeLocation = Location("").apply {
                        latitude = product.storeLocation.latitude
                        longitude = product.storeLocation.longitude
                    }
                    Log.d("LocationService", "Checking distance to ${product.name}, ${currentLocation.distanceTo(storeLocation)}")

                    if (currentLocation.distanceTo(storeLocation) < 200) {
                        showNotification(product.code, product.name)
                    }
                }
            }
    }

    /**
     * Create a notification channel for location notifications
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_channel",
                "Location Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for nearby stores"
                enableLights(true)
                lightColor = Color.RED
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(productId: String, productTitle: String) {
        createNotificationChannel()

        val intent = Intent(this, ProductDetailActivity::class.java).apply {
            putExtra("PRODUCT_CODE", productId)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("Nearby Product!")
            .setContentText("You're near $productTitle")
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        Log.d("LocationService", "Showing notification for $productTitle")

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            Log.d("LocationService", "No notification permission")

            // Run the permission check in a coroutine
            CoroutineScope(Dispatchers.Main).launch {
                ActivityCompat.requestPermissions(
                    this@BaseActivity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
            }
            return
        }
        NotificationManagerCompat.from(this).notify(productId.hashCode(), notification)
    }

    /**
     * Apply the theme based on the user's preference
     */
    private fun applyTheme() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val nightMode = sharedPreferences.getBoolean("night_mode", false)

        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when {
            nightMode && currentNightMode != Configuration.UI_MODE_NIGHT_YES -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                delegate.applyDayNight()
            }
            !nightMode && currentNightMode != Configuration.UI_MODE_NIGHT_NO -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                delegate.applyDayNight()
            }
            // else do nothing as the mode is already set
        }
    }
    fun updateTheme(){
        applyTheme()
        recreate()
    }

    fun applyTextSizePreference() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val textSize = prefs.getString("text_size", "medium") ?: "medium"
        val scaleFactor = when (textSize) {
            "small" -> 0.8f
            "large" -> 1.2f
            else -> 1.0f // medium
        }
        applyTextScaling(scaleFactor)
    }

    private fun applyTextScaling(scaleFactor: Float) {
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        applyScaleToChildren(rootView, scaleFactor)
    }

    private fun applyScaleToChildren(viewGroup: ViewGroup, scaleFactor: Float) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            when (child) {
                is ViewGroup -> applyScaleToChildren(child, scaleFactor)
                is TextView -> {
                    child.textSize = child.textSize * scaleFactor
                }
                is Button -> {
                    child.textSize = child.textSize * scaleFactor
                }
                // Add other text components if needed
            }
        }
    }

    fun updateTextSize() {
        applyTextSizePreference()
    }
}