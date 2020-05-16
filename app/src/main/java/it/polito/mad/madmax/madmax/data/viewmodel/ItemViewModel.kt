package it.polito.mad.madmax.madmax.data.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.repository.FirestoreRepository

class ItemViewModel: ViewModel() {

    private val itemRepository = FirestoreRepository()

    private val items: MutableLiveData<ArrayList<Item>> by lazy {
        MutableLiveData<ArrayList<Item>>().also {
           loadOnSaleItems()
        }
    }

    fun getOnSaleItems() : LiveData<ArrayList<Item>>{
        return items
    }

    private fun loadOnSaleItems(){
        itemRepository.getOnSaleItems()

           /* .addOnSuccessListener {
                val itemArray = ArrayList<Item>()
                for (doc in it.documents){
                    itemArray.add(doc.toObject(Item::class.java)!!)
                }
                items.value = itemArray
            }.addOnFailureListener {

            }*/
        //val items = MutableLiveData<ArrayList<Item>>()
            .addSnapshotListener{value,e ->
                if (e!= null){
                    Log.e("ERR","Listen failed")
                    return@addSnapshotListener
                }
                val itemArray = ArrayList<Item>()
                for (doc in value!!.documents){
                    itemArray.add(doc.toObject(Item::class.java)!!)
                }
                items.value = itemArray

            }

    }
}
