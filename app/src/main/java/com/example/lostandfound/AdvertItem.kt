package com.example.lostandfound

data class AdvertItem(
    val id: Long = 0,
    val postType: String,      // "Lost" or "Found"
    val name: String,
    val phone: String,
    val description: String,
    val date: String,
    val location: String,
    val category: String,
    val imagePath: String?,
    val timestamp: Long
)
