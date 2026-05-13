package com.example.lostandfound

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var db: DatabaseHelper
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null
    private var allItems: List<AdvertItem> = emptyList()
    private var userLocation: LatLng? = null
    private var radiusCircle: Circle? = null
    private var currentRadiusKm: Int = 5

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 300
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = DatabaseHelper(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        allItems = db.getAllAdverts()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupRadiusSeekBar()
    }

    private fun setupRadiusSeekBar() {
        val tvRadiusLabel = findViewById<android.widget.TextView>(R.id.tv_radius_label)
        val seekBar = findViewById<SeekBar>(R.id.seekbar_radius)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                currentRadiusKm = if (progress < 1) 1 else progress
                tvRadiusLabel.text = "Radius: $currentRadiusKm km"
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}

            override fun onStopTrackingTouch(sb: SeekBar?) {
                filterAndShowMarkers()
            }
        })
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Try to get user location for radius filtering
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
            fetchUserLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            // Show all items without radius filter initially
            showAllMarkers()
        }
    }

    private fun fetchUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val cancellationToken = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = LatLng(location.latitude, location.longitude)
                    filterAndShowMarkers()
                } else {
                    // Fallback: show all markers
                    showAllMarkers()
                }
            }
            .addOnFailureListener {
                showAllMarkers()
            }
    }

    private fun filterAndShowMarkers() {
        val map = googleMap ?: return
        map.clear()
        radiusCircle = null

        val userLoc = userLocation
        val tvItemCount = findViewById<android.widget.TextView>(R.id.tv_item_count)

        if (userLoc == null) {
            showAllMarkers()
            return
        }

        // Draw radius circle
        radiusCircle = map.addCircle(
            CircleOptions()
                .center(userLoc)
                .radius((currentRadiusKm * 1000).toDouble())
                .strokeColor(Color.argb(180, 21, 101, 192))
                .fillColor(Color.argb(40, 21, 101, 192))
                .strokeWidth(3f)
        )

        // Filter items within radius
        val filteredItems = allItems.filter { item ->
            if (item.latitude == 0.0 && item.longitude == 0.0) return@filter false
            val results = FloatArray(1)
            Location.distanceBetween(
                userLoc.latitude, userLoc.longitude,
                item.latitude, item.longitude,
                results
            )
            results[0] / 1000 <= currentRadiusKm
        }

        tvItemCount.text = "Showing ${filteredItems.size} item(s) within $currentRadiusKm km"

        if (filteredItems.isEmpty()) {
            // Zoom to user location with radius
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLoc, getZoomLevel(currentRadiusKm)))
            return
        }

        val boundsBuilder = LatLngBounds.Builder()
        boundsBuilder.include(userLoc)

        for (item in filteredItems) {
            val pos = LatLng(item.latitude, item.longitude)
            boundsBuilder.include(pos)

            val markerColor = if (item.postType == "Lost")
                BitmapDescriptorFactory.HUE_RED
            else
                BitmapDescriptorFactory.HUE_GREEN

            map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title("${item.postType}: ${item.name}")
                    .snippet("${item.category} - ${item.location}")
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
            )
        }

        try {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
        } catch (e: Exception) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLoc, getZoomLevel(currentRadiusKm)))
        }
    }

    private fun showAllMarkers() {
        val map = googleMap ?: return
        map.clear()

        val tvItemCount = findViewById<android.widget.TextView>(R.id.tv_item_count)

        val itemsWithLocation = allItems.filter { it.latitude != 0.0 || it.longitude != 0.0 }
        tvItemCount.text = "Showing all ${itemsWithLocation.size} item(s)"

        if (itemsWithLocation.isEmpty()) {
            // Default to Melbourne if no items
            val defaultLoc = LatLng(-37.8136, 144.9631)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 10f))
            Toast.makeText(this, "No items with location data", Toast.LENGTH_SHORT).show()
            return
        }

        val boundsBuilder = LatLngBounds.Builder()

        for (item in itemsWithLocation) {
            val pos = LatLng(item.latitude, item.longitude)
            boundsBuilder.include(pos)

            val markerColor = if (item.postType == "Lost")
                BitmapDescriptorFactory.HUE_RED
            else
                BitmapDescriptorFactory.HUE_GREEN

            map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title("${item.postType}: ${item.name}")
                    .snippet("${item.category} - ${item.location}")
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
            )
        }

        try {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
        } catch (e: Exception) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(itemsWithLocation[0].latitude, itemsWithLocation[0].longitude), 12f
            ))
        }
    }

    private fun getZoomLevel(radiusKm: Int): Float {
        return when {
            radiusKm <= 1  -> 15f
            radiusKm <= 3  -> 13f
            radiusKm <= 5  -> 12f
            radiusKm <= 10 -> 11f
            radiusKm <= 20 -> 10f
            radiusKm <= 50 -> 8f
            else           -> 7f
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                googleMap?.let { map ->
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        map.isMyLocationEnabled = true
                    }
                }
                fetchUserLocation()
            } else {
                Toast.makeText(this, "Location permission denied. Showing all items.", Toast.LENGTH_SHORT).show()
                showAllMarkers()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
