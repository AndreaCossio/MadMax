package it.polito.mad.madmax.madmax.data.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.ItemFilter
import it.polito.mad.madmax.madmax.data.repository.FirestoreRepository
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ItemViewModel: ViewModel() {

    //private val userId: String = Firebase.auth.currentUser!!.uid
    private val repo: FirestoreRepository = FirestoreRepository()

    private val item: MutableLiveData<Item> by lazy {
        MutableLiveData<Item>()
    }

    private val items: MutableLiveData<ArrayList<Item>> by lazy {
        MutableLiveData<ArrayList<Item>>()
    }

    fun getNewItemId(): String {
        return repo.getNewItemId()
    }

    fun getItemList():  MutableLiveData<ArrayList<Item>> {
        return items
    }

    fun getSingleItem(): MutableLiveData<Item> {
        return item
    }

    fun clearSingleItemData() {
        item.value = Item()
    }

    fun clearItemsData() {
        items.value?.clear()
    }

    fun updateItem(newItem: Item, photoChanged: Boolean): Task<Void> {
        return if (newItem.photo == "" || !photoChanged) {
            repo.writeItem(newItem.itemId, newItem).addOnFailureListener { e ->
                Log.e(TAG, "Failed to update item", e)
            }
        } else {
            repo.writeItemPhoto(newItem.itemId, Uri.parse(newItem.photo)).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    newItem.apply { photo = task.result.toString() }
                }
            }.continueWithTask {
                repo.writeItem(newItem.itemId, newItem).addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update item", e)
                }
            }
        }
    }

    fun listenSingleItem(itemId: String): ListenerRegistration {
        return repo.getItem(itemId).addSnapshotListener { value, e ->
            e?.also {
                Log.w(TAG, "Listen failed: ${it.message}")
                return@addSnapshotListener
            }
            item.value = value!!.toObject(Item::class.java)!!.apply {
                this.itemId = value.id
            }
        }
    }

    fun listenMyItems(userId: String): ListenerRegistration {
        return repo.getItems(true, userId).addSnapshotListener { snapshots, e ->
            e?.also {
                Log.w(TAG, "Listen failed: ${it.message}")
                return@addSnapshotListener
            }

            val newItems = ArrayList<Item>()
            for (doc in snapshots!!) {
                newItems.add(doc.toObject(Item::class.java).apply {
                    itemId = doc.id
                })
            }
            items.value = newItems
        }
    }

    fun listenOthersItems(itemFilter: ItemFilter): ListenerRegistration {
        var itemRef: Query = repo.getItems(false)
        if (itemFilter.minPrice != -1.0) {
            itemRef = itemRef.whereGreaterThan("price", itemFilter.minPrice)
        }
        if (itemFilter.maxPrice != -1.0) {
            itemRef = itemRef.whereLessThan("price", itemFilter.maxPrice)
        }
        if (itemFilter.mainCategory != "") {
            itemRef = itemRef.whereEqualTo("category_main", itemFilter.mainCategory)
        }
        if (itemFilter.subCategory != "") {
            itemRef = itemRef.whereEqualTo("category_sub", itemFilter.subCategory)
        }

        return itemRef.addSnapshotListener { snapshots, e ->
            e?.also {
                Log.w(TAG, "Listen failed: ${it.message}")
                return@addSnapshotListener
            }

            val newItems = ArrayList<Item>()
            for (doc in snapshots!!) {
                if (itemFilter.userId == "" || itemFilter.userId != doc["userId"]) {
                    if (itemFilter.text == "" || doc["title"].toString().contains(itemFilter.text) || doc["description"].toString().contains(itemFilter.text)) {
                        // This if is just to be sure to not display something unwanted
                        if (/*(itemFilter.mainCategory == "" || itemFilter.mainCategory == doc["category_main"].toString()) &&
                            (itemFilter.subCategory == "" || itemFilter.subCategory == doc["category_sub"].toString()) &&
                            (itemFilter.minPrice == -1.0 || itemFilter.minPrice < doc["price"].toString().toDouble()) &&
                            (itemFilter.maxPrice == -1.0 || itemFilter.maxPrice > doc["price"].toString().toDouble()) &&*/
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(doc["expiry"].toString())!! > Date()) {
                                newItems.add(doc.toObject(Item::class.java).apply {
                                    itemId = doc.id
                                })
                        }
                    }
                }
            }
            items.value = newItems
        }
    }

    fun notifyInterest(item: Item, userId: String): Task<Transaction> {
        return repo.notifyInterest(item, userId)
    }

    companion object {
        const val TAG = "MM_ITEM_VM"
    }
}
