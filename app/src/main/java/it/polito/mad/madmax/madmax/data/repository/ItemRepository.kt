package it.polito.mad.madmax.madmax.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.User

class ItemRepository {

    private val db = Firebase.firestore


    fun getOnSaleItems(): MutableLiveData<ArrayList<Item>> {
        val items = MutableLiveData<ArrayList<Item>>()
        db.collection("items")
            .get()
            .addOnSuccessListener {
                val itemArray = ArrayList<Item>()
                for (doc in it.documents){
                    itemArray.add(doc.toObject(Item::class.java)!!)
                }
                items.value = itemArray

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