package it.polito.mad.madmax.madmax.data.model

import java.io.Serializable

data class ItemKey (
    var itemId: String,
    var item: Item
) : Serializable