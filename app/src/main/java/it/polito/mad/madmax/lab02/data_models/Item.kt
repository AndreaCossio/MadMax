package it.polito.mad.madmax.lab02.data_models

import java.io.Serializable

data class Item (
    var id: Int? = null,
    var title: String = "",
    var description: String = "",
    var category: String = "",
    var price: Double = 0.0,
    var location: String = "",
    var expiry: String = "",
    var stars:Double = Math.random()%3+2, //Random number for now, because the rating can't be set by the user itself
    var photo: String? = null
) : Serializable