package com.example.lostandfound

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.lostandfound.databinding.ActivityAdvertDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdvertDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "extra_advert_id"
    }

    private lateinit var binding: ActivityAdvertDetailBinding
    private lateinit var db: DatabaseHelper
    private var currentItem: AdvertItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdvertDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = DatabaseHelper(this)

        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id == -1L) { finish(); return }

        currentItem = db.getAdvertById(id)
        if (currentItem == null) {
            Toast.makeText(this, "Item not found.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        displayItem(currentItem!!)
        setupRemoveButton()
    }

    private fun displayItem(item: AdvertItem) {
        // Post type with colour
        val typeLabel = item.postType.uppercase()
        binding.tvPostType.text = typeLabel
        val color = if (item.postType == "Lost") {
            getColor(R.color.color_lost)
        } else {
            getColor(R.color.color_found)
        }
        binding.tvPostType.setTextColor(color)

        binding.tvName.text        = item.name
        binding.tvPhone.text       = item.phone
        binding.tvDescription.text = item.description
        binding.tvDate.text        = item.date
        binding.tvLocation.text    = item.location
        binding.tvCategory.text    = item.category

        // Full timestamp
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        binding.tvPostedAt.text = sdf.format(Date(item.timestamp))

        // Image
        if (item.imagePath != null) {
            try {
                binding.imgDetail.setImageURI(Uri.parse(item.imagePath))
            } catch (_: Exception) {
                binding.imgDetail.setImageResource(R.drawable.ic_image_placeholder)
            }
        } else {
            binding.imgDetail.setImageResource(R.drawable.ic_image_placeholder)
        }
    }

    private fun setupRemoveButton() {
        binding.btnRemove.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Remove Advert")
                .setMessage("Remove this advert? This means the item has been returned to its owner.")
                .setPositiveButton("Remove") { _, _ ->
                    currentItem?.let {
                        db.deleteAdvert(it.id)
                        Toast.makeText(this, "Advert removed.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
