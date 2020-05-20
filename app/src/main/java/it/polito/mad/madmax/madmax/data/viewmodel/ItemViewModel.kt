package it.polito.mad.madmax.madmax.data.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.ItemKey
import it.polito.mad.madmax.madmax.data.repository.FirestoreRepository
import it.polito.mad.madmax.madmax.ui.item.ItemAdapter

class ItemViewModel: ViewModel() {

    private val userId: String = Firebase.auth.currentUser!!.uid
    private val repo: FirestoreRepository = FirestoreRepository()

    val othersItems: ArrayList<MutableLiveData<ItemKey>> by lazy {
        ArrayList<MutableLiveData<ItemKey>>()
    }

    val myItems: ArrayList<MutableLiveData<ItemKey>> by lazy {
        ArrayList<MutableLiveData<ItemKey>>()
    }

    fun getNewItemId(): String {
        return repo.getNewItemId()
    }

    fun updateItem(newItem: ItemKey, photoChanged: Boolean): Task<Void> {
        return if (newItem.item.photo == "" || !photoChanged) {
            repo.writeItem(newItem.itemId, newItem.item).addOnFailureListener { e ->
                Log.e(TAG, "Failed to update item", e)
            }
        } else {
            repo.writeItemPhoto(newItem.itemId, Uri.parse(newItem.item.photo)).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    newItem.item.apply { photo = task.result.toString() }
                }
            }.continueWithTask {
                repo.writeItem(newItem.itemId, newItem.item).addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update item", e)
                }
            }
        }
    }

    fun createItem(newId: String, newItem: Item): Task<Void> {
        return if (newItem.photo == "") {
            myItems.add(MutableLiveData(ItemKey(newId, newItem)))
            repo.writeItem(newId, newItem).addOnFailureListener { e ->
                Log.e(TAG, "Failed to update item", e)
            }
        } else {
            repo.writeItemPhoto(newId, Uri.parse(newItem.photo)).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    newItem.apply { photo = task.result.toString() }
                }
            }.continueWithTask {
                myItems.add(MutableLiveData(ItemKey(newId, newItem)))
                repo.writeItem(newId, newItem).addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update item", e)
                }
            }
        }
    }

    fun listenOnItems(personal: Boolean, itemAdapter: ItemAdapter?): ListenerRegistration {
        return repo.getItems(userId, personal).addSnapshotListener { snapshots, e ->
            e?.also {
                Log.w(TAG, "Listen failed: ${it.message}")
                return@addSnapshotListener
            }

            for (dc in snapshots!!.documentChanges) {
                val itemKey = ItemKey(dc.document.id, dc.document.toObject(Item::class.java))
                if (personal || (!personal && itemKey.item.userId != userId)) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            var alreadyIn = false
                            itemAdapter?.addItem(itemKey)
                            if (personal) {
                                for (i in myItems) {
                                    if (i.value!!.itemId == itemKey.itemId) {
                                        i.value = itemKey
                                        alreadyIn = true
                                        break
                                    }
                                }
                                if (!alreadyIn) {
                                    myItems.add(MutableLiveData(itemKey))
                                }
                            } else {
                                for (i in othersItems) {
                                    if (i.value!!.itemId == itemKey.itemId) {
                                        i.value = itemKey
                                        alreadyIn = true
                                        break
                                    }
                                }
                                if (!alreadyIn) {
                                    othersItems.add(MutableLiveData(itemKey))
                                }
                            }
                        }
                        DocumentChange.Type.MODIFIED -> {
                            itemAdapter?.changeItem(itemKey)
                            if (personal) {
                                for (i in myItems) {
                                    if (i.value!!.itemId == itemKey.itemId) {
                                        i.value = itemKey
                                        break
                                    }
                                }
                            } else {
                                for (i in othersItems) {
                                    if (i.value!!.itemId == itemKey.itemId) {
                                        i.value = itemKey
                                        break
                                    }
                                }
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            itemAdapter?.removeItem(itemKey)
                            if (personal) {
                                for (i in myItems) {
                                    if (i.value!!.itemId == itemKey.itemId) {
                                        myItems.remove(i)
                                        break
                                    }
                                }
                            } else {
                                for (i in othersItems) {
                                    if (i.value!!.itemId == itemKey.itemId) {
                                        myItems.remove(i)
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun notifyInterest(itemId: String) {
        repo.notifyInterest(itemId, userId).addOnSuccessListener {
            Log.d(TAG, "Added user of interest")
        }
    }

    companion object {
        const val TAG = "MM_ITEM_VM"
    }
}
