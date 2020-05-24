package it.polito.mad.madmax.madmax.data.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.google.firebase.ktx.Firebase
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

    fun deleteItem(item: Item): Task<Transaction> {
        return repo.deleteItem(item)
    }

    fun listenSingleItem(itemId: String): ListenerRegistration {
        return repo.getItem(itemId).addSnapshotListener { value, e ->
            e?.also {
                Log.w(TAG, "Listen failed: ${it.message}")
                return@addSnapshotListener
            }
            item.value = value!!.toObject(Item::class.java)?.apply {
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
                        // If not expired
                        if (SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(doc["expiry"].toString())!! > Date()) {
                            if (!itemFilter.onlyFavourite || (doc["interestedUsers"] as ArrayList<String>).contains(Firebase.auth.currentUser!!.uid)) {
                                newItems.add(doc.toObject(Item::class.java).apply {
                                    itemId = doc.id
                                })
                            }
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

    fun removeInterest(item: Item, userId: String): Task<Transaction> {
        return repo.removeInterest(item, userId)
    }

    fun checkIfInterested(itemId: String, userId: String): Task<DocumentSnapshot> {
        return repo.checkIfInterested(itemId, userId).get()
    }

    companion object {
        const val TAG = "MM_ITEM_VM"
    }
}
