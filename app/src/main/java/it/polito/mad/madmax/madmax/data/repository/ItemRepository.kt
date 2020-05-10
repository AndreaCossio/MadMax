package it.polito.mad.madmax.madmax.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.User

class ItemRepository {

    private val db = Firebase.firestore

    fun getOnSaleItems(): ArrayList<Item> {
        val items = ArrayList<Item>()
        db.collection("items")
            .get()
            .addOnSuccessListener {
                for (doc in it.documents){
                    items.add(doc.toObject(Item::class.java)!!)
                }
            }.addOnFailureListener{
            }
        return items
    }

    fun writeItem(item: Item) {
        val db = Firebase.firestore
        db.collection("items")
            .document()
            .set(
                item
            )
            .addOnSuccessListener {
                Log.d("XXX","Success")
            }.addOnFailureListener{
                Log.d("XXX","Error")
            }
    }

}