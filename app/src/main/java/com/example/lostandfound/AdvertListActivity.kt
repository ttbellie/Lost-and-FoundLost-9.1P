package com.example.lostandfound

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lostandfound.databinding.ActivityAdvertListBinding

class AdvertListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdvertListBinding
    private lateinit var db: DatabaseHelper
    private lateinit var adapter: AdvertAdapter

    private val filterOptions = listOf(
        "All Categories", "Electronics", "Pets", "Wallets",
        "Documents", "Keys", "Jacket", "Umbrella", "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdvertListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = DatabaseHelper(this)

        setupRecyclerView()
        setupFilterSpinner()
    }

    override fun onResume() {
        super.onResume()
        // Refresh list when coming back from detail (after possible delete)
        val selectedPos = binding.spinnerFilter.selectedItemPosition
        loadAdverts(selectedPos)
    }

    private fun setupRecyclerView() {
        adapter = AdvertAdapter { item ->
            val intent = Intent(this, AdvertDetailActivity::class.java)
            intent.putExtra(AdvertDetailActivity.EXTRA_ID, item.id)
            startActivity(intent)
        }
        binding.rvAdverts.layoutManager = LinearLayoutManager(this)
        binding.rvAdverts.adapter = adapter
    }

    private fun setupFilterSpinner() {
        val spinnerAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, filterOptions
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFilter.adapter = spinnerAdapter

        binding.spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                loadAdverts(pos)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadAdverts(filterPos: Int) {
        val items = if (filterPos == 0) {
            db.getAllAdverts()
        } else {
            db.getAdvertsByCategory(filterOptions[filterPos])
        }
        adapter.submitList(items)
        binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
