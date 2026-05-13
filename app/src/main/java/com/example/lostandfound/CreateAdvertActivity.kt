package com.example.lostandfound

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.lostandfound.databinding.ActivityCreateAdvertBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateAdvertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateAdvertBinding
    private lateinit var db: DatabaseHelper
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedImageUri: Uri? = null
    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0

    private val categories = listOf(
        "Electronics", "Pets", "Wallets", "Documents", "Keys", "Jacket", "Umbrella", "Other"
    )

    companion object {
        private const val AUTOCOMPLETE_REQUEST_CODE = 100
        private const val LOCATION_PERMISSION_REQUEST_CODE = 200
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) { }
                    selectedImageUri = uri
                    binding.imgPreview.setImageURI(uri)
                    binding.tvImageStatus.text = getString(R.string.image_selected)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAdvertBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = DatabaseHelper(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getApiKey())
        }

        setupCategorySpinner()
        setupDateDefault()
        setupImagePicker()
        setupSaveButton()
        setupLocationAutocomplete()
        setupGetCurrentLocation()
    }

    private fun getApiKey(): String {
        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        return appInfo.metaData.getString("com.google.android.geo.API_KEY") ?: ""
    }

    private fun setupLocationAutocomplete() {
        binding.etLocation.isFocusable = false
        binding.etLocation.isClickable = true
        binding.etLocation.setOnClickListener {
            launchAutocomplete()
        }
    }

    private fun launchAutocomplete() {
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .build(this)
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

    private fun setupGetCurrentLocation() {
        binding.btnGetCurrentLocation.setOnClickListener {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show()

        val cancellationToken = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    selectedLatitude = location.latitude
                    selectedLongitude = location.longitude
                    // Reverse geocode to get address
                    try {
                        val geocoder = android.location.Geocoder(this, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0].getAddressLine(0)
                            binding.etLocation.setText(address)
                        } else {
                            binding.etLocation.setText("${location.latitude}, ${location.longitude}")
                        }
                    } catch (e: Exception) {
                        binding.etLocation.setText("${location.latitude}, ${location.longitude}")
                    }
                } else {
                    Toast.makeText(this, "Unable to get location. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Location error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Use Activity Result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    data?.let {
                        val place = Autocomplete.getPlaceFromIntent(it)
                        binding.etLocation.setText(place.address ?: place.name)
                        place.latLng?.let { latLng ->
                            selectedLatitude = latLng.latitude
                            selectedLongitude = latLng.longitude
                        }
                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    data?.let {
                        val status = Autocomplete.getStatusFromIntent(it)
                        Toast.makeText(this, "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupDateDefault() {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        binding.etDate.setText(sdf.format(Date()))
    }

    private fun setupImagePicker() {
        binding.btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            }
            pickImageLauncher.launch(intent)
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveAdvert()
        }
    }

    private fun saveAdvert() {
        val name        = binding.etName.text.toString().trim()
        val phone       = binding.etPhone.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val date        = binding.etDate.text.toString().trim()
        val location    = binding.etLocation.text.toString().trim()
        val category    = binding.spinnerCategory.selectedItem?.toString() ?: ""
        val postType    = if (binding.rbLost.isChecked) "Lost" else "Found"

        // Validation
        if (name.isBlank()) { binding.etName.error = "Name is required"; return }
        if (phone.isBlank()) { binding.etPhone.error = "Phone is required"; return }
        if (description.isBlank()) { binding.etDescription.error = "Description is required"; return }
        if (date.isBlank()) { binding.etDate.error = "Date is required"; return }
        if (location.isBlank()) { binding.etLocation.error = "Location is required"; return }

        val item = AdvertItem(
            postType    = postType,
            name        = name,
            phone       = phone,
            description = description,
            date        = date,
            location    = location,
            category    = category,
            imagePath   = selectedImageUri?.toString(),
            timestamp   = System.currentTimeMillis(),
            latitude    = selectedLatitude,
            longitude   = selectedLongitude
        )

        val rowId = db.insertAdvert(item)
        if (rowId > 0) {
            Toast.makeText(this, "Advert saved!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Failed to save. Try again.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
