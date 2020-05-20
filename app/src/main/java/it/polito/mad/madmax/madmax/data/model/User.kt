package it.polito.mad.madmax.madmax.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class User (
    @get:Exclude var id: String?,
    var name: String = "",
    var nickname: String = "",
    var email: String = "",
    var location: String = "",
    var phone: String = "",
    var photo: String = ""
) : Serializable