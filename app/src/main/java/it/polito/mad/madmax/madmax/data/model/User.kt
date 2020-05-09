package it.polito.mad.madmax.madmax.data.model

import java.io.Serializable

data class User (
    var name: String = "",
    var nickname: String = "",
    var email: String = "",
    var location: String = "",
    var phone: String = "",
    var photo: String = ""
) : Serializable