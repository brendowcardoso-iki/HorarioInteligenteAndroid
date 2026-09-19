package com.example.data.model

enum class DriveItemType {
    FOLDER,
    GOOGLE_DOC,
    GOOGLE_SHEET,
    JSON_BACKUP,
    PDF,
    TEXT
}

data class DriveItem(
    val id: String,
    val name: String,
    val type: DriveItemType,
    val parentFolderId: String? = null,
    val sizeString: String = "14 KB",
    val modifiedDate: String = "Hoje, 10:30",
    val contentSeed: String? = null,
    val activitiesCount: Int = 0,
    val isRecentlyUploaded: Boolean = false
)
