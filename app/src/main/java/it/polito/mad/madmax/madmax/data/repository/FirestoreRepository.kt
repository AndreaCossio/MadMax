package it.polito.mad.madmax.madmax.data.repository

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
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
    fun getItems(userId: String, personal: Boolean, destItem: MutableLiveData<ArrayList<Item>>) {
        if (personal) {
            db.collection("users/$userId/items")
                .get().addOnSuccessListener { collection ->
                    val list: ArrayList<Item> = ArrayList()
                    for (doc in collection) {
                        list.add(doc.toObject(Item::class.java))
                    }
                    destItem.value = list
                }
        } else {
            db.collection("items")
                .whereLessThan("userId", userId)
                .whereGreaterThan("userId", userId)
                .get().addOnSuccessListener { collection ->
                    val list: ArrayList<Item> = ArrayList()
                    for (doc in collection) {
                        list.add(doc.toObject(Item::class.java))
                    }
                    destItem.value = list
                }
        }
    }

    fun writeItem(itemId: String, item: Item) {
        db.document("items/$itemId").set(item)
    }

    fun createItem(item: Item) {
        db.collection("items").document().set(item)
    }
}