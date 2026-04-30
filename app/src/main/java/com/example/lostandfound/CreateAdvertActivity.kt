package com.example.lostandfound

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.lostandfound.databinding.ActivityCreateAdvertBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateAdvertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateAdvertBinding
    private lateinit var db: DatabaseHelper
    private var selectedImageUri: Uri? = null

    private val categories = listOf(
        "Electronics", "Pets", "Wallets", "Documents", "Keys", "Jacket", "Umbrella", "Other"
    )

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    // Persist read permission so the URI survives app restarts
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
        setupCategorySpinner()
        setupDateDefault()
        setupImagePicker()
        setupSaveButton()
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
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        val item = AdvertItem(
            postType    = postType,
            name        = name,
            phone       = phone,
            description = description,
            date        = date,
            location    = location,
            category    = category,
            imagePath   = selectedImageUri.toString(),
            timestamp   = System.currentTimeMillis()
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
