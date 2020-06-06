package it.polito.mad.madmax.data.model

import com.google.firebase.firestore.Exclude
import java.io.Serializable

data class Item (
    @get:Exclude var itemId: String = "",
    var userId: String = "",
    var status: String = "",
    var boughtBy: String = "",
    var title: String = "",
    var description: String = "",
    var categoryMain: String = "",
    var categorySub: String = "",
    var price: Double = -1.0,
    var location: String = "",
    var expiry: String = "",
    var photo: String = "",
    var rating: String = "",
    var interestedUsers: MutableList<String> = mutableListOf<String>()
) : Serializable