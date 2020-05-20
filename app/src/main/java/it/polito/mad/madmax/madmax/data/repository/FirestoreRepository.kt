package it.polito.mad.madmax.madmax.data.repository

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.User

class FirestoreRepository {

    private val db = Firebase.firestore
    private val storage = Firebase.storage

    //
    // USER
    //
    fun getUser(userId: String): DocumentReference {
        return db.document("users/$userId")
    }

    fun writeUser(userId: String, user: User): Task<Void> {
        return db.document("users/$userId").set(user)
    }

    fun writeUserPhoto(userId: String, uri: Uri): Task<Uri> {
        storage.reference.child("images/$userId/profile.jpg").also { ref ->
            return ref.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                ref.downloadUrl
            }
        }
    }

    //
    // ITEM
    //
    fun getItems(userId: String, personal: Boolean): Query {
        return if (personal) {
            db.collection("users/$userId/items")
        } else {
            db.collection("items")
        }
    }

    fun getNewItemId(): String {
        return db.collection("items").document().id
    }

    fun writeItem(itemId: String, item: Item): Task<Void> {
        return db.runBatch { batch ->
            batch.set(db.document("items/$itemId"), item)
            batch.set(db.document("users/${item.userId}/items/$itemId"), item)
        }
    }

    fun writeItemPhoto(itemId: String, uri: Uri): Task<Uri> {
        storage.reference.child("images/$itemId.jpg").also { ref ->
            return ref.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                ref.downloadUrl
            }
        }
    }
}