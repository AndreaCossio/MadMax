package it.polito.mad.madmax.data.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Transaction
import com.google.firebase.messaging.FirebaseMessaging
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

    fun enableItem(context: Context, item: Item, userId: String): Task<Transaction> {
        return repo.enableItem(item.itemId, userId).addOnSuccessListener {
            try {
                sendNotification(context, createNotification(item.itemId, context.getString(R.string.app_name), "The item you were interested in, is available again: ${item.title}"))
            } catch (e: JSONException) {
                Log.e(TAG, "Failed to send notification.", e)
            }
        }
    }

    fun disableItem(context: Context, item: Item, userId: String): Task<Transaction> {
        return repo.disableItem(item.itemId, userId).addOnSuccessListener {
            try {
                sendNotification(context, createNotification(item.itemId, context.getString(R.string.app_name), "The item you were interested in, is no longer available: ${item.title}"))
            } catch (e: JSONException) {
                Log.e(TAG, "Failed to send notification.", e)
            }
        }
    }

    fun deleteItem(context: Context, item: Item): Task<Transaction> {
        return repo.deleteItem(item).addOnSuccessListener {
            try {
                sendNotification(context, createNotification(item.itemId, context.getString(R.string.app_name), "The item you were interested in, is no longer available: ${item.title}"))
            } catch (e: JSONException) {
                Log.e(TAG, "Failed to send notification.", e)
            }
        }
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
        return repo.getItems(personal, userId, itemFilter, interested, bought).addSnapshotListener { snapshots, e ->
            e?.also {
                Log.w(TAG, "Listen failed: ${it.message}")
                return@addSnapshotListener
            }

            val newItems = ArrayList<Item>()
            for (doc in snapshots!!) {
                if (itemFilter == null || itemFilter.userId == "" || itemFilter.userId != doc["userId"]) {
                    if (itemFilter == null || itemFilter.text == "" || doc["title"].toString().contains(itemFilter.text) || doc["description"].toString().contains(itemFilter.text)) {
                        if (bought || interested || personal || SimpleDateFormat("dd MMM yyyy", Locale.UK).parse(doc["expiry"].toString())!! > Date()) {
                            if (bought || interested || personal || doc["status"].toString() == "Enabled") {
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

    fun notifyInterest(context: Context, item: Item, userId: String): Task<Transaction> {
        return repo.notifyInterest(item, userId).addOnSuccessListener {
            FirebaseMessaging.getInstance().subscribeToTopic("/topics/${item.itemId}")
            try {
                sendNotification(context, createNotification(item.userId, context.getString(R.string.app_name), "Someone is interested in your article: ${item.title}"))
            } catch (e: JSONException) {
                Log.e(TAG, "Failed to send notification.", e)
            }
        }
    }

    fun removeInterest(context: Context, item: Item, userId: String): Task<Transaction> {
        return repo.removeInterest(item, userId).addOnSuccessListener {
            try {
                sendNotification(context, createNotification(item.userId, context.getString(R.string.app_name),"Someone is no more interested in your article: ${item.title}"))
            } catch (e: JSONException) {
                Log.e(TAG, "Failed to send notification.", e)
            }
            FirebaseMessaging.getInstance().unsubscribeFromTopic("/topics/${item.itemId}")
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

    companion object {
        private const val TAG = "MM_ITEM_VM"
    }
}
