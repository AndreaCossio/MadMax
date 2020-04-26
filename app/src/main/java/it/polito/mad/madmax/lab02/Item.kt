package it.polito.mad.madmax.lab02

import java.io.Serializable
import java.net.URL
import java.util.*

class Item(
    val photo: String?,
    val title: String?,
    val description: String?,
    val price: Double?,
    val category: String?,
    val location: String?,
    val expiry: String?,
    val stars:Float?
) : Serializable