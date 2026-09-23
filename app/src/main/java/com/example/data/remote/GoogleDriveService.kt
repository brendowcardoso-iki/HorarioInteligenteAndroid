package com.example.data.remote

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.content.Context
import com.example.data.model.DriveItem
import com.example.data.model.DriveItemType
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

class GoogleDriveService(private val context: Context) {

    private var driveClient: Drive? = null
    private var currentAccount: GoogleSignInAccount? = null
    private var currentAccountEmail: String? = null

    val scopes = listOf(
        DriveScopes.DRIVE_FILE,
        DriveScopes.DRIVE_APPDATA,
        DriveScopes.DRIVE_READONLY,
        "https://www.googleapis.com/auth/documents.readonly"
    )

    val signInOptions: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(
                Scope(DriveScopes.DRIVE_FILE),
                Scope(DriveScopes.DRIVE_APPDATA),
                Scope(DriveScopes.DRIVE_READONLY)
            )
            .build()
    }

    val googleSignInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(context, signInOptions)
    }

    fun getChooseAccountIntent(): Intent {
        val credential = GoogleAccountCredential.usingOAuth2(context, scopes)
        return credential.newChooseAccountIntent()
    }

    /**
     * Initializes Drive API client using direct Google account email.
     */
    fun initializeDriveWithEmail(accountEmail: String): Drive {
        currentAccountEmail = accountEmail
        val credential = GoogleAccountCredential.usingOAuth2(context, scopes).apply {
            selectedAccountName = accountEmail
        }

        val drive = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Horario Inteligente")
            .build()

        this.driveClient = drive
        return drive
    }

    /**
     * Initializes Drive API client after successful Google Sign-In.
     */
    fun initializeDriveWithAccount(account: GoogleSignInAccount): Drive {
        currentAccount = account
        currentAccountEmail = account.email
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            scopes
        ).apply {
            selectedAccount = account.account
        }

        val drive = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Horario Inteligente")
            .build()

        this.driveClient = drive
        return drive
    }

    /**
     * Checks if user is already signed in with required permissions.
     */
    fun getSignedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))) {
            currentAccount = account
            initializeDriveWithAccount(account)
            return account
        }
        return null
    }

    /**
     * Disconnects / signs out the user from Google Drive.
     */
    suspend fun signOut(): Unit = withContext(Dispatchers.IO) {
        googleSignInClient.signOut()
        googleSignInClient.revokeAccess()
        driveClient = null
        currentAccount = null
    }

    /**
     * Lists files and folders inside a given parent folder (or root 'root' / null).
     */
    suspend fun listFolderItems(parentFolderId: String? = null): List<DriveItem> = withContext(Dispatchers.IO) {
        val client = driveClient ?: return@withContext emptyList()

        val parentQuery = if (parentFolderId.isNullOrBlank()) {
            "'root' in parents and trashed = false"
        } else {
            "'$parentFolderId' in parents and trashed = false"
        }

        try {
            val result: FileList = client.files().list()
                .setQ(parentQuery)
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name, mimeType, modifiedTime, size, parents)")
                .setOrderBy("folder, modifiedTime desc")
                .setPageSize(50)
                .execute()

            val files = result.files ?: emptyList()
            files.map { file ->
                val type = mapMimeTypeToDriveType(file.mimeType, file.name)
                val formattedDate = file.modifiedTime?.let {
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    sdf.format(Date(it.value))
                } ?: "Hoje"

                val sizeDisplay = if (type == DriveItemType.FOLDER) {
                    "Pasta"
                } else {
                    val bytes = file.getSize() ?: 0L
                    formatFileSize(bytes)
                }

                DriveItem(
                    id = file.id ?: "",
                    name = file.name ?: "Sem título",
                    type = type,
                    parentFolderId = parentFolderId,
                    sizeString = sizeDisplay,
                    modifiedDate = formattedDate
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Creates a new folder in Google Drive.
     */
    suspend fun createFolder(folderName: String, parentFolderId: String? = null): DriveItem? = withContext(Dispatchers.IO) {
        val client = driveClient ?: return@withContext null

        try {
            val folderMetadata = File().apply {
                name = folderName
                mimeType = "application/vnd.google-apps.folder"
                if (!parentFolderId.isNullOrBlank()) {
                    parents = Collections.singletonList(parentFolderId)
                }
            }

            val createdFile = client.files().create(folderMetadata)
                .setFields("id, name, mimeType, modifiedTime, parents")
                .execute()

            DriveItem(
                id = createdFile.id,
                name = createdFile.name,
                type = DriveItemType.FOLDER,
                parentFolderId = parentFolderId,
                sizeString = "Pasta",
                modifiedDate = "Agora",
                isRecentlyUploaded = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Uploads content (JSON / Text / Routine Backup) to Google Drive.
     */
    suspend fun uploadFile(
        fileName: String,
        content: String,
        mimeType: String,
        parentFolderId: String? = null
    ): DriveItem? = withContext(Dispatchers.IO) {
        val client = driveClient ?: return@withContext null

        try {
            val fileMetadata = File().apply {
                name = fileName
                if (!parentFolderId.isNullOrBlank()) {
                    parents = Collections.singletonList(parentFolderId)
                }
            }

            val mediaContent = com.google.api.client.http.ByteArrayContent.fromString(mimeType, content)

            val uploadedFile = client.files().create(fileMetadata, mediaContent)
                .setFields("id, name, mimeType, modifiedTime, size, parents")
                .execute()

            val driveType = mapMimeTypeToDriveType(uploadedFile.mimeType ?: mimeType, fileName)
            val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

            DriveItem(
                id = uploadedFile.id,
                name = uploadedFile.name ?: fileName,
                type = driveType,
                parentFolderId = parentFolderId,
                sizeString = formatFileSize(content.toByteArray().size.toLong()),
                modifiedDate = formattedDate,
                contentSeed = content,
                isRecentlyUploaded = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Reads text content of a file from Google Drive.
     */
    suspend fun readFileContent(fileId: String, mimeType: String? = null): String? = withContext(Dispatchers.IO) {
        val client = driveClient ?: return@withContext null

        try {
            val outputStream = ByteArrayOutputStream()
            if (mimeType == "application/vnd.google-apps.document") {
                // Export Google Doc as plain text
                client.files().export(fileId, "text/plain").executeMediaAndDownloadTo(outputStream)
            } else {
                client.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            }
            outputStream.toString(Charsets.UTF_8.name())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun mapMimeTypeToDriveType(mimeType: String?, fileName: String): DriveItemType {
        return when {
            mimeType == "application/vnd.google-apps.folder" -> DriveItemType.FOLDER
            mimeType == "application/vnd.google-apps.document" || fileName.endsWith(".gdoc", ignoreCase = true) -> DriveItemType.GOOGLE_DOC
            mimeType == "application/vnd.google-apps.spreadsheet" || fileName.endsWith(".gsheet", ignoreCase = true) -> DriveItemType.GOOGLE_SHEET
            mimeType == "application/json" || fileName.endsWith(".json", ignoreCase = true) -> DriveItemType.JSON_BACKUP
            mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true) -> DriveItemType.PDF
            else -> DriveItemType.TEXT
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes <= 0 -> "0 B"
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
            else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
