package it.polito.mad.madmax.madmax.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class Item (
    var userId: String? = null,
    @get:Exclude var id: String? = null,
    var title: String = "",
    var description: String = "",
    var category_main: String = "",
    var category_sub: String = "",
    var price: Double = 0.0,
    var location: String = "",
    var expiry: String = "",
    var stars: Double = 5.0,
    var photo: String = "",
    var interestedUsers: MutableList<String> = mutableListOf<String>()
) : Serializable