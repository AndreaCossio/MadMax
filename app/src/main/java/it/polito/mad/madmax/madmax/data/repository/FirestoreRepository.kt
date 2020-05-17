package it.polito.mad.madmax.madmax.data.repository

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseUser
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
    fun getItems(userId: String, personal: Boolean) {
        if (personal) {

        } else {

        }
    }

    fun writeItem(itemId: String, item: Item) {
        db.document("items/$itemId").set(item)
    }

    fun createItem(item: Item) {
        db.collection("items").document().set(item)
    }
}