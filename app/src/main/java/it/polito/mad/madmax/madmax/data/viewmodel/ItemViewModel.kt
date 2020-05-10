package it.polito.mad.madmax.madmax.data.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.repository.ItemRepository

class ItemViewModel(): ViewModel() {

    private val itemRepository = ItemRepository()

    private val items: MutableLiveData<ArrayList<Item>> by lazy {
        MutableLiveData<ArrayList<Item>>().also {
           loadOnSaleItems()
        }
    }

    fun getOnSaleItems() : MutableLiveData<ArrayList<Item>>{
        return items
    }

    private fun loadOnSaleItems(){
        // TODO ??????
        /*itemRepository.getOnSaleItems().observe(, Observer {

        })*/
        items.value = itemRepository.getOnSaleItems().value
    }
}
