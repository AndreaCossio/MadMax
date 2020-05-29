package it.polito.mad.madmax.data.model

import java.io.Serializable

data class ItemFilter(
    var minPrice: Double? = null,
    var maxPrice: Double? = null,
    var mainCategory: String = "",
    var subCategory: String = "",
    var userId: String = "",
    var text: String = ""
) : Serializable