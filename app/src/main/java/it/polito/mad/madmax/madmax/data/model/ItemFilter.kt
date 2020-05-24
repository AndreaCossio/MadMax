package it.polito.mad.madmax.madmax.data.model

import java.io.Serializable

data class ItemFilter(
    var minPrice: Double = -1.0,
    var maxPrice: Double = -1.0,
    var mainCategory: String = "",
    var subCategory: String = "",
    var userId: String = "",
    var text: String = "",
    var onlyFavourite: Boolean = false
) : Serializable