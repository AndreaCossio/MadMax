package it.polito.mad.madmax.data.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Transaction
import it.polito.mad.madmax.R
import it.polito.mad.madmax.data.model.Item
import it.polito.mad.madmax.data.model.User
import it.polito.mad.madmax.data.repository.FirestoreRepository
import it.polito.mad.madmax.data.repository.MyFirebaseMessagingService.Companion.createNotification
import it.polito.mad.madmax.data.repository.MyFirebaseMessagingService.Companion.sendNotification
import org.json.JSONException

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

    private val interestedUsers: MutableLiveData<ArrayList<User>> by lazy {
        MutableLiveData<ArrayList<User>>().apply {
            value = ArrayList<User>()
        }
    }

    fun getCurrentUserData(): MutableLiveData<User> {
        return currentUser
    }

    fun getCurrentUserId(): String {
        return currentUser.value!!.userId
    }

    fun getOtherUserData(): MutableLiveData<User> {
        return otherUser
    }

    fun getInterestedUsersData() : MutableLiveData<ArrayList<User>> {
        return interestedUsers
    }

    fun clearOtherUserData() {
        otherUser.value = User()
    }

    fun clearInterestedUsersData() {
        interestedUsers.value?.clear()
    }

    fun updateUser(newUser: User): Task<Void> {
        return if (newUser.photo == "" || newUser.photo == currentUser.value?.photo) {
            repo.writeUser(currentUser.value!!.userId, newUser).addOnFailureListener { e ->
                Log.e(TAG, "Failed to update user", e)
            }
        } else {
            repo.writeUserPhoto(currentUser.value!!.userId, Uri.parse(newUser.photo)).addOnCompleteListener { task ->
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

    fun rateUser(context: Context, userId: String, rating: String): Task<Transaction> {
        return repo.rateUser(userId, rating).addOnSuccessListener {
            try {
                sendNotification(context, createNotification(userId, context.getString(R.string.app_name), "You have received a new rating."))
            } catch (e: JSONException) {
                Log.e(TAG, "Failed to send notification.", e)
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

    fun listenInterestedUsers(itemId: String): ListenerRegistration {
        return repo.getInterestedUsersList(itemId).addSnapshotListener { value, e ->
            if (e != null) {
                Log.e(TAG, "Listen failed.", e)
                return@addSnapshotListener
            }

            val tasks = value!!.toObject(Item::class.java)!!.interestedUsers.map { uid ->
                repo.getUser(uid).get()
            }
            Tasks.whenAllSuccess<DocumentSnapshot>(tasks).addOnSuccessListener { snapshot ->
                val list = snapshot.map { ds ->
                    ds.toObject(User::class.java)?.apply {
                        userId = ds.id
                    }
                } as ArrayList<User>
                interestedUsers.value = list
            }
        }
    }

    // Companion
    companion object {
        const val TAG = "MM_USER_VM"
    }
}
