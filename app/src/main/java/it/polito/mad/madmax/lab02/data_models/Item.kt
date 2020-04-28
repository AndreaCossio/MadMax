package it.polito.mad.madmax.lab02.data_models

import java.io.Serializable

// TODO define some required fields
data class Item (
    val photo: String?,
    val title: String?,
    val description: String?,
    val price: Double?,
    val category: String?,
    val location: String?,
    val expiry: String?,
    val stars:Float?,
    val id:Int?
) : Serializable