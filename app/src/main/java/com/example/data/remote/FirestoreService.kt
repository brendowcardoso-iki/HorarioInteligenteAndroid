package com.example.data.remote

import android.content.Context
import com.example.data.model.ScheduleItem
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreService(private val context: Context? = null) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (context != null) {
                FirebaseAuthService.ensureFirebaseApp(context)
            }
            if (FirebaseApp.getApps(context ?: FirebaseApp.getInstance().applicationContext).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun userScheduleCollection(userId: String) =
        firestore?.collection("users")?.document(userId)?.collection("schedule_items")

    private fun userDoc(userId: String) =
        firestore?.collection("users")?.document(userId)

    /**
     * Saves or updates a list of schedule items in Firestore for a user.
     */
    suspend fun saveScheduleItems(userId: String, items: List<ScheduleItem>): Result<Unit> {
        val fs = firestore ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
        val collection = userScheduleCollection(userId) ?: return Result.failure(IllegalStateException("Coleção não encontrada"))
        return try {
            val batch = fs.batch()

            for (item in items) {
                val docRef = collection.document(item.id.toString())
                val data = hashMapOf(
                    "id" to item.id,
                    "startTime" to item.startTime,
                    "endTime" to item.endTime,
                    "activity" to item.activity,
                    "instructions" to item.instructions,
                    "category" to item.category,
                    "sourceFile" to item.sourceFile,
                    "isCompleted" to item.isCompleted,
                    "updatedAt" to System.currentTimeMillis()
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Saves a single schedule item.
     */
    suspend fun saveScheduleItem(userId: String, item: ScheduleItem): Result<Unit> {
        val collection = userScheduleCollection(userId) ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
        return try {
            val docRef = collection.document(item.id.toString())
            val data = hashMapOf(
                "id" to item.id,
                "startTime" to item.startTime,
                "endTime" to item.endTime,
                "activity" to item.activity,
                "instructions" to item.instructions,
                "category" to item.category,
                "sourceFile" to item.sourceFile,
                "isCompleted" to item.isCompleted,
                "updatedAt" to System.currentTimeMillis()
            )
            docRef.set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Deletes a schedule item from Firestore.
     */
    suspend fun deleteScheduleItem(userId: String, itemId: Long): Result<Unit> {
        val collection = userScheduleCollection(userId) ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
        return try {
            collection.document(itemId.toString()).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Fetches all schedule items for a user once from Firestore.
     */
    suspend fun fetchScheduleItems(userId: String): Result<List<ScheduleItem>> {
        val collection = userScheduleCollection(userId) ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
        return try {
            val snapshot = collection.get().await()
            val items = snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L
                    val startTime = doc.getString("startTime") ?: "08:00"
                    val endTime = doc.getString("endTime") ?: "09:00"
                    val activity = doc.getString("activity") ?: ""
                    val instructions = doc.getString("instructions") ?: ""
                    val category = doc.getString("category") ?: "Outros"
                    val sourceFile = doc.getString("sourceFile") ?: "Nuvem Firestore"
                    val isCompleted = doc.getBoolean("isCompleted") ?: false

                    if (activity.isNotBlank()) {
                        ScheduleItem(
                            id = id,
                            startTime = startTime,
                            endTime = endTime,
                            activity = activity,
                            instructions = instructions,
                            category = category,
                            sourceFile = sourceFile,
                            isCompleted = isCompleted
                        )
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(items)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Realtime Flow observing schedule items in Firestore for the given user.
     */
    fun observeScheduleItems(userId: String): Flow<List<ScheduleItem>> = callbackFlow {
        val collection = userScheduleCollection(userId)
        if (collection == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L
                        val startTime = doc.getString("startTime") ?: "08:00"
                        val endTime = doc.getString("endTime") ?: "09:00"
                        val activity = doc.getString("activity") ?: ""
                        val instructions = doc.getString("instructions") ?: ""
                        val category = doc.getString("category") ?: "Outros"
                        val sourceFile = doc.getString("sourceFile") ?: "Nuvem Firestore"
                        val isCompleted = doc.getBoolean("isCompleted") ?: false

                        if (activity.isNotBlank()) {
                            ScheduleItem(
                                id = id,
                                startTime = startTime,
                                endTime = endTime,
                                activity = activity,
                                instructions = instructions,
                                category = category,
                                sourceFile = sourceFile,
                                isCompleted = isCompleted
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(items)
            }
        }
        awaitClose {
            listener.remove()
        }
    }

    /**
     * Updates user profile data in Firestore.
     */
    suspend fun saveUserProfile(
        userId: String,
        email: String,
        displayName: String,
        photoUrl: String? = null
    ): Result<Unit> {
        val doc = userDoc(userId) ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
        return try {
            val data = hashMapOf(
                "email" to email,
                "displayName" to displayName,
                "photoUrl" to (photoUrl ?: ""),
                "lastActive" to System.currentTimeMillis()
            )
            doc.set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
