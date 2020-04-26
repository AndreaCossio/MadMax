package it.polito.mad.madmax.lab02.data_models

import java.io.Serializable

data class User (
    val name: String,
    val nickname: String,
    val email: String,
    val location: String,
    val phone: String,
    val uri: String?
) : Serializable