package com.paulomenezes.filestorage01

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.paulomenezes.filestorage01.adapters.FilesAdapter
import com.paulomenezes.filestorage01.databinding.ActivityMainBinding
import com.paulomenezes.filestorage01.databinding.DialogCreateFileBinding
import com.paulomenezes.filestorage01.interfaces.RecyclerViewFilterCallback
import com.paulomenezes.filestorage01.models.FileStorage
import com.paulomenezes.filestorage01.models.FileStorageType
import com.paulomenezes.filestorage01.utils.ContentProviderUtils
import com.paulomenezes.filestorage01.utils.FileUtils
import java.util.*

class MainActivity : AppCompatActivity(), RecyclerViewFilterCallback {
    companion object {
        const val REQUEST_PERMISSION_CODE = 1
    }

    private lateinit var binding: ActivityMainBinding

    private val fileUtils = FileUtils(this)
    private val adapter: FilesAdapter by lazy { FilesAdapter(this, list) { file -> onRemoveFile(file) } }
    private val list = mutableListOf<FileStorage>()
    private var fileFilter = FileStorageType.INTERNAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermission()
        loadAllFiles()

        val contentProviderUtils = ContentProviderUtils(this)
        contentProviderUtils.writeImage()
        contentProviderUtils.loadImages()

        binding.apply {
            recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
            recyclerView.adapter = adapter

            buttonAdd.setOnClickListener {
                val view = layoutInflater.inflate(R.layout.dialog_create_file, null);
                val modalBinding = DialogCreateFileBinding.bind(view)

                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setView(view)
                builder.setTitle(R.string.modal_title)

                val modal = builder.create()
                modal.show()

                modalBinding.buttonCancel.setOnClickListener {
                    modal.dismiss()
                }

                modalBinding.buttonCreate.setOnClickListener {
                    if (onCreateFile(modalBinding)) {
                        modal.dismiss()
                    }
                }
            }
        }
    }

    override fun onFilter() {
        checkEmptyListContent()
    }

    private fun loadAllFiles() {
        val internalFiles = fileUtils.readFilesInternal().map { file -> FileStorage(file.name, file.readText(), FileStorageType.INTERNAL, false) }
        val externalFiles = fileUtils.readFilesExternal().map { file -> FileStorage(file.name, file.readText(), FileStorageType.EXTERNAL, false) }

        list.addAll(internalFiles)
        list.addAll(externalFiles)

        adapter.filter.filter(FileStorageType.INTERNAL.toString())
    }

    private fun onCreateFile(modalBinding: DialogCreateFileBinding): Boolean {
        modalBinding.apply {
            val fileName = inputFileName.text.toString()
            val fileContent = inputFileContent.text.toString()

            if (fileName.isNotEmpty() && fileContent.isNotEmpty()) {
                val type: FileStorageType
                val fileNameWithExtension = "$fileName.txt"
                val isEncrypted = switchEncrypted.isChecked

                if (radioButtonInternal.isChecked) {
                    type = FileStorageType.INTERNAL

                    if (switchEncrypted.isChecked) {
                        fileUtils.createSafeInternalFile(fileNameWithExtension, fileContent)
                    } else {
                        fileUtils.createInternalFile(fileNameWithExtension, fileContent)
                    }
                } else {
                    type = FileStorageType.EXTERNAL

                    if (switchEncrypted.isChecked) {
                        fileUtils.createSafeExternalFile(fileNameWithExtension, fileContent)
                    } else {
                        fileUtils.createExternalFile(fileNameWithExtension, fileContent)
                    }
                }

                val item = FileStorage(fileNameWithExtension, fileContent, type, isEncrypted)

                list.add(item)
                adapter.originalList.add(item)
                adapter.notifyDataSetChanged()
                adapter.filter.filter(fileFilter.toString())

                checkEmptyListContent()

                showMessage(R.string.add_success)

                return true
            } else {
                showMessage(R.string.empty_fields)

                return false
            }
        }
    }

    private fun checkEmptyListContent() {
        if (list.isEmpty()) {
            binding.layoutEmptyList.visibility = View.VISIBLE
        } else {
            binding.layoutEmptyList.visibility = View.INVISIBLE
        }
    }

    private fun showMessage(@StringRes resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }

    private fun onRemoveFile(file: FileStorage) {
        list.remove(file)
        adapter.originalList.remove(file)
        adapter.notifyDataSetChanged()

        val deletedFile = if (file.type == FileStorageType.INTERNAL) fileUtils.removeInternalFile(file) else fileUtils.removeExternalFile(file)

        if (deletedFile) {
            showMessage(R.string.delete_success)
        } else {
            showMessage(R.string.delete_error)
        }

        checkEmptyListContent()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_filter, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.radioFilterInternal -> {
            adapter.filter.filter(FileStorageType.INTERNAL.toString())
            item.isChecked = true

            fileFilter = FileStorageType.INTERNAL

            true
        }
        R.id.radioFilterExternal -> {
            adapter.filter.filter(FileStorageType.EXTERNAL.toString())
            item.isChecked = true

            fileFilter = FileStorageType.EXTERNAL

            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            REQUEST_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                showMessage(R.string.permission_denied)
            } else {
                showMessage(R.string.permission_granted)
            }
        }
    }
}