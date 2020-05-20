package it.polito.mad.madmax.madmax.data.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.User
import it.polito.mad.madmax.madmax.data.repository.FirestoreRepository

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
        firestoreRepository.getInterestedUsersList(itemId)
            .addSnapshotListener { value, e ->
                if (e != null) {
                    Log.e("ERR", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val usersList = ArrayList<User>()
                for (doc in value!!.documents) {
                    usersList.add( Gson().fromJson(Gson().toJson(doc.data), User::class.java))
                }
                users.value = usersList
            }


    }
}
