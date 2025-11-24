package com.aplicaciones_android.aplicacionrocola_projectofinal

import android.net.Uri
import android.util.Log
import com.aplicaciones_android.aplicacionrocola_projectofinal.model.MenuDish
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MenuRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val dishesCollection = firestore.collection("menu_dishes")

    suspend fun fetchDishes(): List<MenuDish> {
        return try {
            val snapshot = dishesCollection
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(MenuDish::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e("MenuRepository", "Error fetching dishes", e)
            emptyList()
        }
    }

    suspend fun upsertDish(dish: MenuDish, imageUri: Uri?): Boolean {
        return try {
            val docRef = if (dish.id.isEmpty()) dishesCollection.document() else dishesCollection.document(dish.id)
            val finalImage = if (imageUri != null) uploadImage(imageUri, dish.imagePath) else dish.imageUrl
            val finalPath = if (imageUri != null) lastUploadedPath else dish.imagePath
            val data = dish.copy(
                id = docRef.id,
                imageUrl = finalImage ?: dish.imageUrl,
                imagePath = finalPath ?: dish.imagePath,
                createdAt = dish.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis()
            )
            docRef.set(data).await()
            true
        } catch (e: Exception) {
            Log.e("MenuRepository", "Error saving dish", e)
            false
        }
    }

    suspend fun updateDishFields(id: String, fields: Map<String, Any>): Boolean {
        return try {
            dishesCollection.document(id).update(fields).await()
            true
        } catch (e: Exception) {
            Log.e("MenuRepository", "Error updating dish", e)
            false
        }
    }

    suspend fun deleteDish(dish: MenuDish): Boolean {
        return try {
            if (dish.imagePath.isNotEmpty()) {
                storage.reference.child(dish.imagePath).delete().await()
            }
            dishesCollection.document(dish.id).delete().await()
            true
        } catch (e: Exception) {
            Log.e("MenuRepository", "Error deleting dish", e)
            false
        }
    }

    private var lastUploadedPath: String? = null

    private suspend fun uploadImage(uri: Uri, existingPath: String): String? {
        return try {
            val path = existingPath.takeIf { it.isNotEmpty() } ?: "menu_dishes/${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child(path)
            ref.putFile(uri).await()
            lastUploadedPath = path
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("MenuRepository", "Error uploading image", e)
            null
        }
    }
}
