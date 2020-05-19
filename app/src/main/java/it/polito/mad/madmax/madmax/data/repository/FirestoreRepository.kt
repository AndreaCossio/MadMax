package it.polito.mad.madmax.madmax.data.repository

import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.User

class FirestoreRepository {

    private val db = Firebase.firestore

    //
    // USER
    //
    fun getUser(user: FirebaseUser, destUser: MutableLiveData<User>) {
        db.document("users/${user.uid}").get().addOnSuccessListener { document ->
            destUser.value = document?.data?.let {
                Gson().fromJson(Gson().toJson(it), User::class.java)
            } ?: createUser(user)
            destUser.value!!.id  = document.id
        }
    }

    fun writeUser(userId: String, user: User) {
        db.document("users/$userId").set(user)
    }

    private fun createUser(newUser: FirebaseUser): User {
        val user: User = User(null).apply {
            name = newUser.displayName ?: ""
            email = newUser.email ?: ""
            phone = newUser.phoneNumber ?: ""
            //photo = newUser.photoUrl.toString() TODO maybe already get the photo?
        }
        writeUser(newUser.uid, user)
        return user
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
        db.collection("items").document(item.id!!).update("interestedUsers",FieldValue.arrayUnion(userId))
    }
    fun createItem(item: Item) {
        val documentReference = db.collection("items").document()
        documentReference.set(item)
            .addOnSuccessListener {
                db.collection("users")
                    .document(item.userId!!)
                    .update("items",FieldValue.arrayUnion(documentReference.id))
            }
    }
}