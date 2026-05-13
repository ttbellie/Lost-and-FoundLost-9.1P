package com.example.lostandfound

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lostandfound.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCreateAdvert.setOnClickListener {
            startActivity(Intent(this, CreateAdvertActivity::class.java))
        }

        binding.btnShowAllItems.setOnClickListener {
            startActivity(Intent(this, AdvertListActivity::class.java))
        }

        binding.btnShowOnMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
    }
}
