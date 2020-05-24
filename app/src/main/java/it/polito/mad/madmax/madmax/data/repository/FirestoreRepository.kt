package it.polito.mad.madmax.madmax.data.repository

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
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
    fun getItem(itemId: String): DocumentReference {
        return db.document("items/$itemId")
    }

    fun getItems(personal: Boolean, userId: String = ""): Query {
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

    fun notifyInterest(item: Item, userId: String): Task<Transaction> {
        return db.runTransaction { transaction ->
            transaction.update(db.document("items/${item.itemId}"), "interestedUsers", FieldValue.arrayUnion(userId))
            transaction.update(db.document("users/${item.userId}/items/${item.itemId}"), "interestedUsers", FieldValue.arrayUnion(userId))
        }
    }

    fun getInterestedUsersList(itemId: String): Task<DocumentSnapshot> {
        return db.collection("items").document(itemId).get()
    }
}