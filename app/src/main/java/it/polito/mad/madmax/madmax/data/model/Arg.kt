package it.polito.mad.madmax.madmax.data.model

import java.io.Serializable

data class Arg (
    var message: String,
    var obj: Serializable? = null
)