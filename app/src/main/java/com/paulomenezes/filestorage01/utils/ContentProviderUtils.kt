package com.paulomenezes.filestorage01.utils

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.paulomenezes.filestorage01.R

class ContentProviderUtils(private val context: AppCompatActivity) {
    fun loadImages() {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH
        )

        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%myImage%")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,//selection,
            null,//selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
            val displayNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val relativePathColumn = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn)
                val relativePath = cursor.getString(relativePathColumn)

                val imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                if (id == 36L) {
                    deleteImage(imageUri)
                }

                Log.d("CURSOR", "Media ID: $id, display name: $displayName, path: $relativePath")
            }

            // binding.imageView.setImageURI(imageUri)
        }
    }

    fun writeImage() {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "myImage.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/My Images/")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val imageUri = context.contentResolver.insert(collection, contentValues)

        imageUri?.let {
            context.contentResolver.openOutputStream(imageUri).use { outputStream ->
                val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.image)
                bmp.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(imageUri, contentValues, null, null)
        }
    }

    fun deleteImage(uri: Uri) {
        try {
            context.contentResolver.delete(
                uri,
                "${MediaStore.Images.Media._ID} = ?",
                arrayOf(ContentUris.parseId(uri).toString())
            )
        } catch (exception: SecurityException) {
            val recoverySecurityException = exception as? RecoverableSecurityException ?: throw exception
            val intentSender = recoverySecurityException.userAction.actionIntent.intentSender

            context.startIntentSenderForResult(intentSender, 1, null, 0, 0, 0, null)
        }
    }
}