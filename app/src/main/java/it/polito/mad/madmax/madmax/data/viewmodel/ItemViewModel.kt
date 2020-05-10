package it.polito.mad.madmax.madmax.data.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.repository.ItemRepository

class ItemViewModel(): ViewModel() {

    private val itemRepository = ItemRepository()

    private val items: MutableLiveData<ArrayList<Item>> by lazy {
        MutableLiveData<ArrayList<Item>>().apply {
            value = itemRepository.getOnSaleItems()
        }
    }

    fun getOnSaleItems() : LiveData<ArrayList<Item>>{
        return items
    }

    private fun loadOnSaleItems(){
        val db = Firebase.firestore
        db.collection("items")
            .get()
            .addOnSuccessListener {
                val arr = ArrayList<Item>()
                for (doc in it.documents){
                    arr.add(doc.toObject(Item::class.java)!!)
                }

                items.value = arr
            }.addOnFailureListener{
                Log.e("XXX",it.toString())
            }
    }
}
