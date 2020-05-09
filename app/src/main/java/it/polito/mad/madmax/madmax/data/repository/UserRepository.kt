package it.polito.mad.madmax.madmax.data.repository

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import it.polito.mad.madmax.madmax.data.model.User

class UserRepository {

    private val db = Firebase.firestore

    fun readUser(userId: String): User? {
        var user: User? = null
        db.document("users/$userId").get().addOnSuccessListener { document ->
            document?.data?.also {
                user = Gson().fromJson(Gson().toJson(it), User::class.java)
            }
        }
        return user
    }

    fun writeUser(userId: String, user: User) {
        db.runBatch { batch ->
            batch.set(db.document("users/$userId"), user)
        }
    }

    fun createUser(newUser: FirebaseUser): User {
        val user: User = User().apply {
            name = newUser.displayName ?: ""
            email = newUser.email ?: ""
            phone = newUser.phoneNumber ?: ""
            //photo = newUser.photoUrl.toString() TODO maybe already get the photo?
        }
        db.runBatch { batch ->
            batch.set(db.document("users/${newUser.uid}"), user)
        }
        return user
    }
}