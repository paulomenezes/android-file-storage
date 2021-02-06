package com.paulomenezes.filestorage01.models

data class FileStorage(
    var name: String,
    var content: String,
    var type: FileStorageType,
    var isEncrypted: Boolean)

enum class FileStorageType {
    INTERNAL,
    EXTERNAL
}