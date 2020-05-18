package it.polito.mad.madmax.madmax.data.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
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
        }
    }

    fun writeUser(userId: String, user: User) {
        db.document("users/$userId").set(user)
    }

    private fun createUser(newUser: FirebaseUser): User {
        val user: User = User().apply {
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
    fun getOnSaleItems(): Task<QuerySnapshot> {
        return db.collection("items").get()
    }

    /*fun getOnSaleItems():MutableLiveData<ArrayList<Item>>{
        val items = MutableLiveData<ArrayList<Item>>()
        db.collection("items")
            .get()
            .addOnSuccessListener {
                val itemArray = ArrayList<Item>()
                for (doc in it.documents){
                    itemArray.add(doc.toObject(Item::class.java)!!)
                }
                items.value = itemArray

            }.addOnFailureListener{
            }.addOnCompleteListener {  }
        return items
    }*/

    fun writeItem(item: Item) {
        val db = Firebase.firestore
        db.collection("items")
            .document()
            .set(
                item
            )
            .addOnSuccessListener {
                Log.d("XXX","Success")
            }.addOnFailureListener{
                Log.d("XXX","Error")
            }
    }
}