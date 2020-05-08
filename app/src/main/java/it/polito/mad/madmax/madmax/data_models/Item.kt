package it.polito.mad.madmax.madmax.data_models

import java.io.Serializable

data class Item (
    var id: Int? = null,
    var title: String = "",
    var description: String = "",
    var category_main: String = "",
    var category_sub: String = "",
    var price: Double = 0.0,
    var location: String = "",
    var expiry: String = "",
    var stars: Double = 5.0,
    var photo: String? = null
) : Serializable