package it.polito.mad.madmax.madmax.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import it.polito.mad.madmax.madmax.data.model.User
import it.polito.mad.madmax.madmax.data.repository.FirestoreRepository

class UserViewModel: ViewModel() {

    private var userId: String = ""
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()

    val user: MutableLiveData<User> by lazy {
        MutableLiveData<User>()
    }

    fun loadUser(user: FirebaseUser) {
        this.userId = user.uid
        firestoreRepository.getUser(user, this.user)
    }

    fun updateUser(user: User) {
        this.user.value = user
        firestoreRepository.writeUser(userId, user)
    }
}
