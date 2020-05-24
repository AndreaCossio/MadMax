package it.polito.mad.madmax.madmax.data.model

import com.google.firebase.firestore.Exclude
import java.io.Serializable

data class Item (
    @get:Exclude var itemId: String = "",
    var userId: String = "",
    var title: String = "",
    var description: String = "",
    var category_main: String = "",
    var category_sub: String = "",
    var price: Double = -1.0,
    var location: String = "",
    var expiry: String = "",
    var stars: Double = -1.0,
    var photo: String = "",
    var interestedUsers: MutableList<String> = mutableListOf<String>()
) : Serializable