package it.polito.mad.madmax.madmax.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import it.polito.mad.madmax.madmax.data.model.Item

class ItemRepository {

    private val db = Firebase.firestore


    fun getOnSaleItems(): CollectionReference {
        return db.collection("items")
    }

    /*fun getOnSaleItems():MutableLiveData<ArrayList<Item>>{
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
            }.addOnCompleteListener {  }
        return items
    }*/

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