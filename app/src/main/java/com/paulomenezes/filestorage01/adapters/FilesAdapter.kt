package com.paulomenezes.filestorage01.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.paulomenezes.filestorage01.models.FileStorage
import com.paulomenezes.filestorage01.models.FileStorageType
import com.paulomenezes.filestorage01.R
import com.paulomenezes.filestorage01.interfaces.RecyclerViewFilterCallback
import com.paulomenezes.filestorage01.databinding.ListItemBinding

class FilesAdapter(
    val context: RecyclerViewFilterCallback,
    val list: MutableList<FileStorage>,
    private val onRemove: (FileStorage) -> Unit
) : RecyclerView.Adapter<FilesAdapter.ViewHolder>(), Filterable {
    val originalList: MutableList<FileStorage> = mutableListOf()

    init {
        originalList.addAll(list)
    }

    class ViewHolder(private val listItem: ListItemBinding) : RecyclerView.ViewHolder(listItem.root) {
        fun bind(file: FileStorage, onRemove: (FileStorage) -> Unit) {
            listItem.textName.text = file.name
            listItem.buttonRemove.setOnClickListener {
                onRemove(file)
            }
            listItem.imageEncrypted.visibility = if (file.isEncrypted) View.VISIBLE else View.INVISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        val binding = ListItemBinding.bind(view)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = list[position]

        holder.bind(file, onRemove)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun publishResults(constraint: CharSequence, results: FilterResults) {
                list.clear()
                list.addAll(results.values as List<FileStorage>)
                notifyDataSetChanged()

                context.onFilter()
            }

            override fun performFiltering(constraint: CharSequence): FilterResults {
                val filteredResults: List<FileStorage> = getFilteredResults(FileStorageType.valueOf(constraint.toString()))

                val results = FilterResults()
                results.values = filteredResults
                return results
            }

            private fun getFilteredResults(type: FileStorageType): List<FileStorage> {
                return originalList.filter { it.type == type }
            }
        }
    }
}