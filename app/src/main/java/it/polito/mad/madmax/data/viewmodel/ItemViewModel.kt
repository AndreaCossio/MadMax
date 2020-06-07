package it.polito.mad.madmax.data.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Transaction
import it.polito.mad.madmax.R
import it.polito.mad.madmax.data.model.Item
import it.polito.mad.madmax.data.model.ItemFilter
import it.polito.mad.madmax.data.model.User
import it.polito.mad.madmax.data.repository.FirestoreRepository
import it.polito.mad.madmax.data.repository.MyFirebaseMessagingService.Companion.createNotification
import it.polito.mad.madmax.data.repository.MyFirebaseMessagingService.Companion.sendNotification
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ItemViewModel: ViewModel() {

    private val repo: FirestoreRepository = FirestoreRepository()

    private val item: MutableLiveData<Item> by lazy {
        MutableLiveData<Item>()
    }

    private val items: MutableLiveData<ArrayList<Item>> by lazy {
        MutableLiveData<ArrayList<Item>>()
    }

    fun getItemData(): MutableLiveData<Item> {
        return item
    }

    fun getItemsData():  MutableLiveData<ArrayList<Item>> {
        return items
    }

    fun clearItem() {
        item.value = Item()
    }

    fun clearItems() {
        items.value?.clear()
    }

    fun getNewItemId(): String {
        return repo.getNewItemId()
    }

    fun listenItem(itemId: String): ListenerRegistration {
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

    fun listenItems(personal: Boolean = false, userId: String = "", itemFilter: ItemFilter? = null, interested: Boolean = false, bought: Boolean = false): ListenerRegistration {
        // Personal -> get the items directly from current user's items
        //
        return repo.getItems(personal, userId, itemFilter, interested, bought).addSnapshotListener { snapshots, e ->
            e?.also {
                Log.w(TAG, "Listen failed: ${it.message}")
                return@addSnapshotListener
            }

            val newItems = ArrayList<Item>()
            for (doc in snapshots!!) {
                // Exclude current user's items from the list (this filter is always applied except for "My Items" view)
                if (itemFilter == null || itemFilter.userId == "" || itemFilter.userId != doc["userId"]) {
                    // Filter for text locally (this filter can be applied only in "On Sale Items")
                    if (itemFilter == null || itemFilter.text == "" || doc["title"].toString().contains(itemFilter.text) || doc["description"].toString().contains(itemFilter.text)) {
                        // Exclude expired items in "On Sale Items" and "Items of Interest" and show only enabled items in those views
                        if (personal || bought || (SimpleDateFormat("dd MMM yyyy", Locale.UK).parse(doc["expiry"].toString())!! > Date() && doc["status"].toString() == "Enabled")) {
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

    fun updateItem(newItem: Item, photoChanged: Boolean): Task<Void> {
        return if (newItem.photo == "" || !photoChanged) {
            repo.writeItem(newItem.itemId, newItem).addOnFailureListener { e ->
                Log.e(TAG, "Failed to update item", e)
            }
        } else {
            repo.writeItemPhoto(newItem.itemId, Uri.parse(newItem.photo)).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    newItem.apply { photo = task.result.toString() }
                } else {
                    Log.e(TAG, "Failed to upload photo", task.exception)
                }
            }.continueWithTask {
                repo.writeItem(newItem.itemId, newItem).addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update item", e)
                }
            }
        }
    }

    fun enableItem(itemId: String, userId: String): Task<Transaction> {
        return repo.enableItem(itemId, userId).addOnFailureListener {
            Log.e(TAG, "Failed to enable item.", it)
        }
    }

    fun disableItem(itemId: String, userId: String): Task<Transaction> {
        return repo.disableItem(itemId, userId).addOnFailureListener {
            Log.e(TAG, "Failed to disable item.", it)
        }
    }

    fun deleteItem(itemId: String, userId: String): Task<Transaction> {
        return repo.deleteItem(itemId, userId).addOnFailureListener {
            Log.e(TAG, "Failed to delete item.", it)
        }
    }

    fun notifyInterest(item: Item, userId: String): Task<Transaction> {
        return repo.notifyInterest(item, userId).addOnFailureListener {
            Log.e(TAG, "Failed to show interest.", it)
        }
    }

    fun removeInterest(item: Item, userId: String): Task<Transaction> {
        return repo.removeInterest(item, userId).addOnFailureListener {
            Log.e(TAG, "Failed to remove interest.", it)
        }
    }

    fun sellItem(context: Context, item: Item, user: User): Task<Transaction> {
        return repo.sellItem(item, user.userId).addOnSuccessListener {
            try {
                sendNotification(context, createNotification(item.itemId, context.getString(R.string.app_name), "The item \"${item.title}\" has been sold to ${user.name}"))
            } catch (e: JSONException) {
                Log.e(TAG, "Failed to send notification.", e)
            }
        }
    }

    // Companion
    companion object {
        private const val TAG = "MM_ITEM_VM"
    }
}
