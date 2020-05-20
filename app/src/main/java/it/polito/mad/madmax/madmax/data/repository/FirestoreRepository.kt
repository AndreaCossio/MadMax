package it.polito.mad.madmax.madmax.data.repository

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.gson.Gson
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

    fun getOnSaleItems(userId: String): Task<List<QuerySnapshot>>{
        val greaterThanQuery = db.collection("items")
            .whereGreaterThan("userId",userId).get()
        val lessThanQuery = db.collection("items").whereLessThan("userId",userId).get()
        return Tasks.whenAllSuccess<QuerySnapshot>(greaterThanQuery,lessThanQuery)
    }

    fun notifyUserOfInterest(item: Item,userId: String){
        db.collection("items").document(item.id!!).update("interestedUsers", FieldValue.arrayUnion(userId))
    }

    fun createItem(item: Item) {
        val documentReference = db.collection("items").document()
        documentReference.set(item)
            .addOnSuccessListener {
                db.collection("users")
                    .document(item.userId!!)
                    .collection("items")
                    .document(documentReference.id)
                    .set(item)
            }
    }

    fun getInterestedUsersList(itemId: String): Task<DocumentSnapshot>{
        return db.collection("items")
            .document(itemId)
            .get()

    }
}