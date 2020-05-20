package it.polito.mad.madmax.madmax.data.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.User
import it.polito.mad.madmax.madmax.data.repository.FirestoreRepository

class ItemViewModel(val userId: String): ViewModel() {

    private val firestoreRepository: FirestoreRepository = FirestoreRepository()

    val otherItems: MutableLiveData<ArrayList<Item>> by lazy {
        MutableLiveData<ArrayList<Item>>().also {
           loadItems()
        }
    }

    fun getOnSaleItems() : LiveData<ArrayList<Item>> {
        return otherItems
    }

    private fun loadItems() {
        firestoreRepository.getOnSaleItems(userId)
            .addOnSuccessListener {
                val documents = it.map { it -> it.documents }.flatten()
                val itemArray = ArrayList<Item>()
                for (doc in documents){
                    itemArray.add(Gson().fromJson(Gson().toJson(doc.data), Item::class.java).also {
                        item -> item.id = doc.id
                    })
                }
                otherItems.value = itemArray
            }.addOnFailureListener{
                throw (it)
            }


    }
}
