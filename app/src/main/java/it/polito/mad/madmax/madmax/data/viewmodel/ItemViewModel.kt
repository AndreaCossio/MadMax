package it.polito.mad.madmax.madmax.data.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.google.firebase.ktx.Firebase
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.ItemFilter
import it.polito.mad.madmax.madmax.data.repository.FirestoreRepository
import it.polito.mad.madmax.madmax.displayMessage
import org.json.JSONException
import org.json.JSONObject
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
                        if (SimpleDateFormat("dd MMM yyyy", Locale.UK).parse(doc["expiry"].toString())!! > Date()) {
                            if (!itemFilter.onlyFavourite || (doc["interestedUsers"] as ArrayList<String>).contains(Firebase.auth.currentUser!!.uid)) {
                                if (doc["status"].toString() == "Enabled") {
                                    newItems.add(doc.toObject(Item::class.java).apply {
                                        itemId = doc.id
                                    })
                                }
                            }
                        }
                    }
                }
            }
            items.value = newItems
        }
    }

    fun notifyInterest(context: Context, item: Item, userId: String): Task<Transaction> {
        return repo.notifyInterest(item, userId).also {
            val notification = createNotification(item)
            try {
                sendNotification(context, notification)
            } catch (e: JSONException) {
                Log.e("TAG", "onCreate: " + e.message)
            }
        }
    }

    fun removeInterest(item: Item, userId: String): Task<Transaction> {
        return repo.removeInterest(item, userId)
    }

    fun checkIfInterested(itemId: String, userId: String): Task<DocumentSnapshot> {
        return repo.checkIfInterested(itemId, userId).get()
    }

    fun enableItem(itemId: String, userId: String): Task<Transaction> {
        return repo.enableItem(itemId, userId)
    }

    fun disableItem(itemId: String, userId: String): Task<Transaction> {
        return repo.disableItem(itemId, userId)
    }

    fun buyItem(item: Item, userId: String): Task<Transaction> {
        return repo.buyItem(item, userId)
    }

    companion object {
        const val TAG = "MM_ITEM_VM"
    }

    //NOTIFICATION
    private fun sendNotification(context: Context, notification: JSONObject) {

        val FCM_API = context.getString(R.string.apiUrl)
        val serverKey = "key=" + context.getString(R.string.serverKey)
        val contentType = context.getString(R.string.cloudContentType)

        val jsonObjectRequest = object: JsonObjectRequest (
            FCM_API,
            notification,
            Response.Listener { response ->
                Log.i("TAG", "onResponse: $response")
            },
            Response.ErrorListener {
                displayMessage(context, "Error request ${it.message.toString()}")
                Log.i("TAG", "onErrorResponse: Didn't work")
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Authorization"] = serverKey
                params["Content-Type"] = contentType
                return params
            }
        }
        Volley.newRequestQueue(context).add(jsonObjectRequest)
    }

    private fun createNotification(item: Item): JSONObject {
        val topic = "/topics/${item.userId}"
        val notification = JSONObject()
        val notificationBody = JSONObject()
        notificationBody.put("title", "MadMax")
        notificationBody.put("message", "Someone is interested in your article ${item.title}")
        notification.put("to", topic)
        notification.put("data", notificationBody)

        return  notification
    }
}
