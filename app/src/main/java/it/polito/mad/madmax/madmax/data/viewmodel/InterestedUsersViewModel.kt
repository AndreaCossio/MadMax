package it.polito.mad.madmax.madmax.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import it.polito.mad.madmax.madmax.FirestoreRepository
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.User

class InterestedUsersViewModel(val itemId: String): ViewModel() {

    private val firestoreRepository: FirestoreRepository = FirestoreRepository()

    private val users: MutableLiveData<ArrayList<User>> by lazy {
        MutableLiveData<ArrayList<User>>().also {
            loadUsers(itemId)
        }
    }

    fun getUsersList() : LiveData<ArrayList<User>> {
        return users
    }

    private fun loadUsers(itemId: String) {
        val gson = Gson()
        /*firestoreRepository.getInterestedUsersList(itemId)
            .addOnSuccessListener {
                val item = gson.fromJson(gson.toJson(it.data),Item::class.java)

                val tasks = item.interestedUsers.map {
                    uid -> Firebase.firestore.collection("users")
                    .document(uid).get()
                }

                Tasks.whenAllSuccess<DocumentSnapshot>(tasks).addOnSuccessListener {
                    docsnap ->
                    val list = docsnap.map { ds -> gson.fromJson(gson.toJson(ds.data),User::class.java) } as ArrayList
                    users.value = list
                }
            }*/
            /*.addSnapshotListener { value, e ->
                if (e != null) {
                    Log.e("ERR", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val usersList = ArrayList<User>()
                for (doc in value!!.documents) {
                    usersList.add( Gson().fromJson(Gson().toJson(doc.data), User::class.java))
                }
                users.value = usersList
            }*/


    }
}
