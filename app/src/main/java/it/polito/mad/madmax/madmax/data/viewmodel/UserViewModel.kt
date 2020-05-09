package it.polito.mad.madmax.madmax.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import it.polito.mad.madmax.madmax.data.model.User
import it.polito.mad.madmax.madmax.data.repository.UserRepository

class UserViewModel(private var userId:String): ViewModel() {

    private val userRepository: UserRepository = UserRepository()

    val user: MutableLiveData<User> by lazy {
        MutableLiveData<User>().apply {
            value = userRepository.readUser(userId)
        }
    }

    fun changeUser(userId: String) {
        this.userId = userId
        this.user.value = userRepository.readUser(userId)
    }

    fun updateUser(user: User) {
        this.user.value = user
        userRepository.writeUser(userId, user)
    }

    fun createUser(newUser: FirebaseUser) {
        this.userId = newUser.uid
        this.user.value = userRepository.createUser(newUser)
    }
}
