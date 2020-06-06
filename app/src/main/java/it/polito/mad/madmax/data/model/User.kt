package it.polito.mad.madmax.data.model

import com.google.firebase.firestore.Exclude
import java.io.Serializable

data class User (
    @get:Exclude var userId: String = "",
    var name: String = "",
    var nickname: String = "",
    var email: String = "",
    var location: String = "",
    var phone: String = "",
    var photo: String = "",
    var ratings: MutableList<String> = mutableListOf<String>()
) : Serializable