package com.burhan.gps

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    // The entry point to the Fused Location Provider.
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var mLastKnownLocation: Location? = null
    private var mLocationPermissionGranted = false
    private var requestingLocationUpdates = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        updateValuesfromBundle(savedInstanceState)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    mLastKnownLocation = location
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            LatLng(location.latitude, location.longitude), mMap.cameraPosition.zoom))
                    Log.d(TAG, "LocationCallback() Location is ${location.latitude} and ${location.longitude}")
                }
            }
        }
    }

    private fun updateValuesfromBundle(savedInstanceState: Bundle?) {
        savedInstanceState ?: return
        if (savedInstanceState.containsKey(ARG_REQUEST_LOCATION_UPDATES)) {
            requestingLocationUpdates = savedInstanceState.getBoolean(ARG_REQUEST_LOCATION_UPDATES)
        }
    }

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) {
            requestLocationUpdates()
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isZoomGesturesEnabled = true

        // Add a marker in Sydney and move the camera
        val perth = LatLng(-31.952854, 115.857342)
        val sydney = LatLng(-33.87365, 151.20689)
        val brisbane = LatLng(-27.47093, 153.0235)
        val melbourne = LatLng(-37.813, 144.962)

        mMap.addMarker(MarkerOptions().position(perth).title("Marker in perth"))
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.addMarker(MarkerOptions().position(brisbane).title("Marker in brisbane"))
        mMap.addMarker(MarkerOptions()
                .position(melbourne)
                .title("Melbourne")
                .snippet("Population: 4,137,400")
                .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_btn_speak_now)))
        mMap.animateCamera(CameraUpdateFactory.newLatLng(sydney))

        mMap.setOnMarkerClickListener { marker ->
            Log.d(TAG, marker.title)
            marker.alpha = 0.5f
            false
        }

        getLocationPermission()
        // Turn on the My Location layer and the related control on the map.
        updateLocationUI()

        // Get the current location of the device and set the position of the map.
        getDeviceLocation()

    }


    private fun requestLocationUpdates() {
        Log.d(TAG, "requestLocationUpdates()")
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    private fun getLocationPermission() {
        /*
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true
            openGps()
        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION, REQUEST_CHECK_SETTINGS -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true
                    openGps()
                }
            }
        }
        updateLocationUI()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        mLocationPermissionGranted = true
                        requestLocationUpdates()
                    }
                    Activity.RESULT_CANCELED -> {
                        mLocationPermissionGranted = false
                        Log.d(TAG, "User has denied to open GPS.")
                    }
                }

            }
        }
    }

    private fun openGps() {
        Log.d(TAG, "openGps()")
        locationRequest = LocationRequest().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        var task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsResponse: LocationSettingsResponse? ->
            Log.d(TAG, "addOnSuccessListener: " + locationSettingsResponse.toString())
            requestingLocationUpdates = true
            requestLocationUpdates()
        }
        task.addOnFailureListener { exception ->
            Log.e(TAG, "addOnFailureListener: " + exception.message)
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(this,
                            REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }

    }

    private fun updateLocationUI() {
        try {
            if (mLocationPermissionGranted) {
                mMap.isMyLocationEnabled = true
                mMap.uiSettings.isMyLocationButtonEnabled = true
            } else {
                mMap.isMyLocationEnabled = false
                mMap.uiSettings.isMyLocationButtonEnabled = false
                mLastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message)
        }

    }

    private fun getDeviceLocation() {
        /*
     * Get the best and most recent location of the device, which may be null in rare
     * cases when a location is not available.
     */

        try {
            if (mLocationPermissionGranted) {
                mFusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        mLastKnownLocation = location
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(location.latitude, location.longitude), DEFAULT_ZOOM))
                        Log.d(TAG, "Location is ${location.latitude} and ${location.longitude}")
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message)
        }

    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putBoolean(ARG_REQUEST_LOCATION_UPDATES, requestingLocationUpdates)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onPause()
    }

    companion object {
        val TAG = MapsActivity::class.java.simpleName
        val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100
        val REQUEST_CHECK_SETTINGS = 101
        val DEFAULT_ZOOM = 13f
        val mDefaultLocation = LatLng(-33.8523341, 151.2106085)
        val ARG_REQUEST_LOCATION_UPDATES = "ARG_REQUEST_LOCATION_UPDATES"
    }
}
