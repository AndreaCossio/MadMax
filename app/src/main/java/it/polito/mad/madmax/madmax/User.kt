package it.polito.mad.madmax.madmax

import java.io.Serializable

data class User (
    val name: String,
    val nickname: String,
    val email: String,
    val location: String
) : Serializable