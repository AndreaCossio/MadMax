package it.polito.mad.madmax.madmax.data.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import it.polito.mad.madmax.madmax.data.model.User
import it.polito.mad.madmax.madmax.data.repository.FirestoreRepository

class UserViewModel: ViewModel() {

    val userId: MutableLiveData<String> by lazy { MutableLiveData<String>("") }
    private val repo: FirestoreRepository = FirestoreRepository()

    val user: MutableLiveData<User> by lazy {
        MutableLiveData<User>()
    }

    fun updateUser(newUser: User): Task<Void> {
        return if (newUser.photo == "" || newUser.photo == user.value?.photo) {
            repo.writeUser(userId.value!!, newUser).addOnFailureListener { e ->
                Log.e(TAG, "Failed to update user", e)
            }
        } else {
            repo.writeUserPhoto(userId.value!!, Uri.parse(newUser.photo)).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    newUser.apply { photo = task.result.toString() }
                }
            }.continueWithTask {
                repo.writeUser(userId.value!!, newUser).addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update user", e)
                }
            }
        }
    }

    fun loginOrCreateUser(fUser: FirebaseUser) {
        // Retrieve user from db and add real time updates
        repo.getUser(fUser.uid).addSnapshotListener { value, e ->
            e?.also {
                Log.w(TAG, "Listen failed: ${it.message}")
            } ?: run {
                // Update VM data
                user.value = value?.toObject(User::class.java) ?: User().apply {
                    name = fUser.displayName ?: ""
                    email = fUser.email ?: ""
                    phone = fUser.phoneNumber ?: ""
                }.also {
                    // Since there was no user in the db we need to create it
                    repo.writeUser(fUser.uid, it).addOnFailureListener { ee ->
                        Log.e(TAG, "Failed to create user", ee)
                    }
                }
                userId.value = fUser.uid
            }
        }
    }

    fun getUser(userId: String, listener: (DocumentSnapshot) -> Unit): ListenerRegistration {
        return repo.getUser(userId).addSnapshotListener {value, e ->
            e?.also {
                Log.w(TAG, "Listen failed: ${it.message}")
            } ?: listener(value!!)
        }
    }

    companion object {
        const val TAG = "MM_USER_VM"
    }
}
