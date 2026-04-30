package com.example.lostandfound

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "lost_and_found.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_ADVERTS = "adverts"
        const val COL_ID = "id"
        const val COL_POST_TYPE = "post_type"
        const val COL_NAME = "name"
        const val COL_PHONE = "phone"
        const val COL_DESCRIPTION = "description"
        const val COL_DATE = "date"
        const val COL_LOCATION = "location"
        const val COL_CATEGORY = "category"
        const val COL_IMAGE_PATH = "image_path"
        const val COL_TIMESTAMP = "timestamp"

        private const val SQL_CREATE = """
            CREATE TABLE $TABLE_ADVERTS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_POST_TYPE TEXT NOT NULL,
                $COL_NAME TEXT NOT NULL,
                $COL_PHONE TEXT NOT NULL,
                $COL_DESCRIPTION TEXT NOT NULL,
                $COL_DATE TEXT NOT NULL,
                $COL_LOCATION TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_IMAGE_PATH TEXT,
                $COL_TIMESTAMP INTEGER NOT NULL
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ADVERTS")
        onCreate(db)
    }

    // ---------- INSERT ----------
    fun insertAdvert(item: AdvertItem): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_POST_TYPE, item.postType)
            put(COL_NAME, item.name)
            put(COL_PHONE, item.phone)
            put(COL_DESCRIPTION, item.description)
            put(COL_DATE, item.date)
            put(COL_LOCATION, item.location)
            put(COL_CATEGORY, item.category)
            put(COL_IMAGE_PATH, item.imagePath)
            put(COL_TIMESTAMP, item.timestamp)
        }
        return db.insert(TABLE_ADVERTS, null, cv)
    }

    // ---------- GET ALL ----------
    fun getAllAdverts(): List<AdvertItem> {
        val list = mutableListOf<AdvertItem>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ADVERTS, null, null, null, null, null,
            "$COL_TIMESTAMP DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToItem(it))
            }
        }
        return list
    }

    // ---------- GET BY CATEGORY ----------
    fun getAdvertsByCategory(category: String): List<AdvertItem> {
        val list = mutableListOf<AdvertItem>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ADVERTS, null,
            "$COL_CATEGORY = ?", arrayOf(category),
            null, null, "$COL_TIMESTAMP DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToItem(it))
            }
        }
        return list
    }

    // ---------- GET BY ID ----------
    fun getAdvertById(id: Long): AdvertItem? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ADVERTS, null,
            "$COL_ID = ?", arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) cursorToItem(it) else null
        }
    }

    // ---------- DELETE ----------
    fun deleteAdvert(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_ADVERTS, "$COL_ID = ?", arrayOf(id.toString()))
    }

    // ---------- HELPER ----------
    private fun cursorToItem(cursor: android.database.Cursor): AdvertItem {
        return AdvertItem(
            id          = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            postType    = cursor.getString(cursor.getColumnIndexOrThrow(COL_POST_TYPE)),
            name        = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
            phone       = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)),
            date        = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)),
            location    = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCATION)),
            category    = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
            imagePath   = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE_PATH)),
            timestamp   = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP))
        )
    }
}
