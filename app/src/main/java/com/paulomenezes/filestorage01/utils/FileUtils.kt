package com.paulomenezes.filestorage01.utils

import android.content.Context
import android.os.Environment
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.paulomenezes.filestorage01.models.FileStorage
import java.io.File

class FileUtils(private val context: Context) {
    fun readFilesInternal(): List<File> {
        return readFilesFromDir(getInternalDir())
    }

    fun readFilesExternal(): List<File> {
        return readFilesFromDir(getExternalDir())
    }

    private fun readFilesFromDir(dir: File?): List<File> {
        return dir?.listFiles()?.toList() ?: emptyList()
    }

    fun createInternalFile(fileName: String, fileContent: String) {
        createFile(getInternalDir(), fileName, fileContent, false)
    }

    fun createExternalFile(fileName: String, fileContent: String) {
        createFile(getExternalDir(), fileName, fileContent, true)
    }

    fun createSafeInternalFile(fileName: String, fileContent: String) {
        createSafeFile(getInternalDir(), fileName, fileContent)
    }

    fun createSafeExternalFile(fileName: String, fileContent: String) {
        createSafeFile(getExternalDir(), fileName, fileContent)
    }

    private fun createFile(parent: File?, fileName: String, fileContent: String, isExternal: Boolean) {
        // Write
        val file = File(parent, fileName)

        if (!isExternal || (isExternal && isExternalStorageWritable())) {
            file.writeText(fileContent)
        }
    }

    fun removeInternalFile(file: FileStorage): Boolean {
        return removeFile(getInternalDir(), file.name)
    }

    fun removeExternalFile(file: FileStorage): Boolean {
        return removeFile(getExternalDir(), file.name)
    }

    private fun removeFile(parent: File?, fileName: String): Boolean {
        val file = File(parent, fileName)

        return file.delete()
    }

    private fun getInternalDir(): File = context.filesDir
    private fun getExternalDir() = context.getExternalFilesDir(null)

    private fun isExternalStorageWritable(): Boolean = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    private fun isExternalStorageReadable(): Boolean = Environment.getExternalStorageState() in setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)

    private fun createSafeFile(dir: File?, fileName: String, fileContent: String) {
        val file = File(dir, fileName)
        if (file.exists()) {
            file.delete()
        }

        val encryptedFile = encryptedFile(file)

        encryptedFile.openFileOutput().use { writer ->
            writer.write(fileContent.toByteArray())
        }
    }

    private fun readSafeFile(dir: File?, fileName: String): String? {
        val file = File(dir, fileName)

        if (file.exists()) {
            val encryptedFile = encryptedFile(file)

            var result = ""

            encryptedFile.openFileInput().use { inputStream ->
                result = inputStream.readBytes().decodeToString()
            }

            return result
        }

        return null
    }

    private fun encryptedFile(file: File): EncryptedFile {
        val mainKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

        return EncryptedFile.Builder(
            context,
            file,
            mainKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }
}