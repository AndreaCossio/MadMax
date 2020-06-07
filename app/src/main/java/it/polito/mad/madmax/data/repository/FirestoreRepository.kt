package it.polito.mad.madmax.data.repository

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import it.polito.mad.madmax.data.model.Item
import it.polito.mad.madmax.data.model.ItemFilter
import it.polito.mad.madmax.data.model.User

class FirestoreRepository {

    private val db = Firebase.firestore
    private val storage = Firebase.storage

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

    fun rateUser(userId: String, rating: String): Task<Transaction> {
        return db.runTransaction { transaction ->
            transaction.update(db.document("users/$userId"), "ratings", FieldValue.arrayUnion(rating))
            transaction.update(db.document("items/${rating.split("+/")[0]}"), "rating", rating)
            transaction.update(db.document("users/$userId/items/${rating.split("+/")[0]}"), "rating", rating)
        }
    }

    fun getItem(itemId: String): DocumentReference {
        return db.document("items/$itemId")
    }

    fun getItems(personal: Boolean, userId: String = "", itemFilter: ItemFilter? = null, interested: Boolean = false, bought: Boolean = false): Query {
        var itemRef: Query = if (personal) {
            db.collection("users/$userId/items")
        } else {
            db.collection("items")
        }
        itemFilter?.also {
            itemFilter.minPrice?.also {
                itemRef = itemRef.whereGreaterThan("price", it)
            }
            itemFilter.maxPrice?.also {
                itemRef = itemRef.whereLessThan("price", it)
            }
            if (itemFilter.mainCategory != "") {
                itemRef = itemRef.whereEqualTo("categoryMain", itemFilter.mainCategory)
            }
            if (itemFilter.subCategory != "") {
                itemRef = itemRef.whereEqualTo("categorySub", itemFilter.subCategory)
            }
        }
        if (interested) {
            itemRef = itemRef.whereArrayContains("interestedUsers", userId)
        }
        if (bought) {
            itemRef = itemRef.whereEqualTo("boughtBy", userId)
        }
        return itemRef
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

    fun deleteItem(itemId: String, userId: String): Task<Transaction> {
        return db.runTransaction { transaction ->
            transaction.delete(db.document("items/$itemId"))
            transaction.delete(db.document("users/$userId/items/$itemId"))
        }
    }

    fun notifyInterest(item: Item, userId: String): Task<Transaction> {
        return db.runTransaction { transaction ->
            transaction.update(db.document("items/${item.itemId}"), "interestedUsers", FieldValue.arrayUnion(userId))
            transaction.update(db.document("users/${item.userId}/items/${item.itemId}"), "interestedUsers", FieldValue.arrayUnion(userId))
        }
    }

    fun removeInterest(item: Item, userId: String): Task<Transaction> {
        return db.runTransaction { transaction ->
            transaction.update(db.document("items/${item.itemId}"), "interestedUsers", FieldValue.arrayRemove(userId))
            transaction.update(db.document("users/${item.userId}/items/${item.itemId}"), "interestedUsers", FieldValue.arrayRemove(userId))
        }
    }

    fun getInterestedUsersList(itemId: String): DocumentReference {
        return db.document("items/$itemId")
    }

    fun enableItem(itemId: String, userId: String): Task<Transaction> {
        return db.runTransaction { transaction ->
            transaction.update(db.document("items/$itemId"), "status", "Enabled")
            transaction.update(db.document("users/$userId/items/$itemId"), "status", "Enabled")
        }
    }

    fun disableItem(itemId: String, userId: String): Task<Transaction> {
        return db.runTransaction { transaction ->
            transaction.update(db.document("items/$itemId"), "status", "Disabled")
            transaction.update(db.document("users/$userId/items/$itemId"), "status", "Disabled")
        }
    }

    fun sellItem(item: Item, userId: String): Task<Transaction> {
        return db.runTransaction { transaction ->
            transaction.update(db.document("items/${item.itemId}"), "status", "Bought")
            transaction.update(db.document("users/${item.userId}/items/${item.itemId}"), "status", "Bought")
            transaction.update(db.document("items/${item.itemId}"), "boughtBy", userId)
            transaction.update(db.document("users/${item.userId}/items/${item.itemId}"), "boughtBy", userId)
        }
    }
}