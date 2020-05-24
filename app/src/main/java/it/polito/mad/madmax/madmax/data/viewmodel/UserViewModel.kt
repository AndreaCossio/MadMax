package it.polito.mad.madmax.madmax.data.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import it.polito.mad.madmax.madmax.data.model.User
import it.polito.mad.madmax.madmax.data.repository.FirestoreRepository

class UserViewModel : ViewModel() {

    private val repo: FirestoreRepository = FirestoreRepository()

    private val currentUser: MutableLiveData<User> by lazy {
        MutableLiveData<User>().apply {
            value = User()
        }
    }

    private val otherUser: MutableLiveData<User> by lazy {
        MutableLiveData<User>().apply {
            value = User()
        }
    }

    fun getCurrentUserData(): MutableLiveData<User> {
        return currentUser
    }

    fun getOtherUserData(): MutableLiveData<User> {
        return otherUser
    }

    fun clearOtherUserData() {
        otherUser.value = User()
    }

    fun updateUser(newUser: User): Task<Void> {
        return if (newUser.photo == "" || newUser.photo == currentUser.value?.photo) {
            repo.writeUser(currentUser.value!!.userId, newUser).addOnFailureListener { e ->
                Log.e(TAG, "Failed to update user", e)
            }
        } else {
            repo.writeUserPhoto(currentUser.value!!.userId, Uri.parse(newUser.photo))
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        newUser.apply { photo = task.result.toString() }
                    }
                }.continueWithTask {
                repo.writeUser(currentUser.value!!.userId, newUser).addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update user", e)
                }
            }
        }
    }

    fun listenCurrentUser(firebaseUser: FirebaseUser): ListenerRegistration {
        return repo.getUser(firebaseUser.uid).addSnapshotListener { document, e ->
            e?.also {
                Log.w(TAG, "Listen failed", e)
                return@addSnapshotListener
            }

            // User data already exist in the db
            if (document != null && document.exists()) {
                // Change live data with new user data
                document.toObject(User::class.java)?.apply {
                    userId = firebaseUser.uid
                }?.also {
                    currentUser.value = it
                }
            }
            // Create user data
            else {
                // Prepare new user data to be written in the db
                // No need to change live data value because it will be updated after writing in the db
                User().apply {
                    name = firebaseUser.displayName ?: ""
                    email = firebaseUser.email ?: ""
                    phone = firebaseUser.phoneNumber ?: ""
                }.also {
                    // Since there was no user in the db we need to create it
                    repo.writeUser(firebaseUser.uid, it).addOnFailureListener { ee ->
                        Log.e(TAG, "Failed to create user", ee)
                    }
                }
            }
        }
    }

    fun listenOtherUser(userId: String): ListenerRegistration {
        return repo.getUser(userId).addSnapshotListener { document, e ->
            e?.also {
                Log.w(TAG, "Listen failed", e)
                return@addSnapshotListener
            }

            // User data already exist
            if (document != null && document.exists()) {
                // Change live data with new user data
                document.toObject(User::class.java)?.apply {
                    this.userId = userId
                }?.also {
                    otherUser.value = it
                }
            }
            // The user does not exist
            // Shouldn't happen
            else {
                Log.d(TAG, "No user found with this id $userId")
            }
        }
    }

    // Companion
    companion object {
        const val TAG = "MM_USER_VM"
    }
}
