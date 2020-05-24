package it.polito.mad.madmax.madmax.data.model

import java.io.Serializable

class ItemArg(
    var task: String = "",
    var item: Item = Item(),
    var owned: Boolean = false
) : Serializable