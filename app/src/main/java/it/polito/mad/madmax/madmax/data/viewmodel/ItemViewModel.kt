package it.polito.mad.madmax.madmax.data.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.repository.FirestoreRepository

class ItemViewModel(public val personal: Boolean): ViewModel() {

    private val firestoreRepository: FirestoreRepository = FirestoreRepository()

    val items: MutableLiveData<ArrayList<Item>> by lazy {
        MutableLiveData<ArrayList<Item>>().also {
           loadItems()
        }
    }

    fun getOnSaleItems() : LiveData<ArrayList<Item>> {
        return items
    }

    private fun loadItems() {
        firestoreRepository.getItems("3ZLAl6PEj5a0Cm8ZyMJrLv72l992", personal)

           /* .addOnSuccessListener {
                val itemArray = ArrayList<Item>()
                for (doc in it.documents){
                    itemArray.add(doc.toObject(Item::class.java)!!)
                }
                items.value = itemArray
            }.addOnFailureListener {

            }*/
        //val items = MutableLiveData<ArrayList<Item>>()
            /*.addSnapshotListener{value,e ->
                if (e!= null){
                    Log.e("ERR","Listen failed")
                    return@addSnapshotListener
                }
                val itemArray = ArrayList<Item>()
                for (doc in value!!.documents){
                    itemArray.add(doc.toObject(Item::class.java)!!)
                }
                items.value = itemArray

            }*/

    }
}
