package it.polito.mad.madmax.lab02.data_models

import java.io.Serializable

data class Item (
    val id: Int? = null,
    var title: String = "",
    var description: String = "",
    var category: String = "",
    var price: Double = 0.0,
    var location: String = "",
    var expiry: String = "",
    var stars:Double = 0.0,
    var photo: String? = null
) : Serializable