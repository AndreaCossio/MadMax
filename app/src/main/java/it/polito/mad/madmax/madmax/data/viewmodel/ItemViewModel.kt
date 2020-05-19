package it.polito.mad.madmax.madmax.data.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.ItemKey
import it.polito.mad.madmax.madmax.data.repository.FirestoreRepository

class ItemViewModel(private val personal: Boolean, private val userId: String): ViewModel() {

    private val firestoreRepository: FirestoreRepository = FirestoreRepository()

    val items: MutableLiveData<ArrayList<ItemKey>> by lazy {
        MutableLiveData<ArrayList<ItemKey>>().also {
            it.value = ArrayList()
        }
    }

    fun loadItems() {
        val colRef = firestoreRepository.getItems(userId, personal)
        if (personal) {
            colRef.addSnapshotListener { value, e ->
                e?.also {
                    Log.w(TAG, "Listen failed: ${it.message}")
                } ?: run {
                    for (doc in value!!) {
                        items.value!!.add(ItemKey(doc.id, Gson().fromJson(Gson().toJson(doc.data.toString()), Item::class.java)))
                    }
                }
            }
        } else {
            colRef.addSnapshotListener { value, e ->
                e?.also {
                    Log.w(TAG, "Listen failed: ${it.message}")
                } ?: run {
                    val list: ArrayList<ItemKey> = ArrayList()
                    for (doc in value!!) {
                        val item = doc.toObject(Item::class.java)
                        if (item.userId != userId)
                            list.add(ItemKey(doc.id, item))
                    }
                    items.value = list
                }
            }
        }
    }

    companion object {
        const val TAG = "MM_ITEM_VM"
    }
}
