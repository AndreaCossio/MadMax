package it.polito.mad.madmax.madmax

import android.net.Uri
import java.io.Serializable

data class User (
    val name: String,
    val nickname: String,
    val email: String,
    val location: String,
val uri:String?
) : Serializable