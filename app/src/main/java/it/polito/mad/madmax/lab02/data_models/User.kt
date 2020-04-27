package it.polito.mad.madmax.lab02.data_models

import java.io.Serializable

data class User (
    var name: String = "",
    var nickname: String = "",
    var email: String = "",
    var location: String = "",
    var phone: String = "",
    var uri: String? = null
) : Serializable