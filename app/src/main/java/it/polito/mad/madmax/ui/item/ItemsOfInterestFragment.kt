package it.polito.mad.madmax.ui.item

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import it.polito.mad.madmax.*
import it.polito.mad.madmax.data.model.Item
import it.polito.mad.madmax.data.repository.MyFirebaseMessagingService.Companion.createNotification
import it.polito.mad.madmax.data.repository.MyFirebaseMessagingService.Companion.sendNotification
import it.polito.mad.madmax.data.viewmodel.ItemViewModel
import it.polito.mad.madmax.data.viewmodel.UserViewModel
import it.polito.mad.madmax.ui.AutoFitGridLayoutManager
import kotlinx.android.synthetic.main.fragment_item_list.*
import org.json.JSONException

class ItemsOfInterestFragment : Fragment() {

    // Models
    private val userVM: UserViewModel by activityViewModels()
    private val itemsVM: ItemViewModel by activityViewModels()
    private lateinit var itemListener: ListenerRegistration

    // Adapter
    private lateinit var itemAdapter: ItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemAdapter = ItemAdapter(actionDetails, actionInterest)
        itemsVM.clearItems()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_item_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showProgress(requireActivity())
        hideFab(requireActivity())

        // Init recyclerview
        item_list_rv.apply {
            setHasFixedSize(false)
            layoutManager = AutoFitGridLayoutManager(requireContext(), 300.toPx())
            adapter = itemAdapter
        }
        item_list_empty_tv.text = getString(R.string.message_empty_list_favourite)

        // Observe the list of items and update the recycler view accordingly
        itemsVM.getItemsData().observe(viewLifecycleOwner, Observer {
            itemAdapter.setItems(it)
            hideProgress(requireActivity())
            if (it.size == 0) {
                item_list_empty.animate().alpha(1F).startDelay = 300
            } else {
                item_list_empty.animate().alpha(0F).startDelay = 300
            }
        })

        itemListener = itemsVM.listenItems(userId = userVM.getCurrentUserId(), interested = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::itemListener.isInitialized) {
            itemListener.remove()
        }
    }

    private var actionDetails = { item: Item ->
        showProgress(requireActivity())
        findNavController().navigate(MainNavigationDirections.actionGlobalDetailsItem(item))
    }

    private var actionInterest = { item: Item ->
        if (!item.interestedUsers.contains(userVM.getCurrentUserId())) {
            itemsVM.notifyInterest(item, userVM.getCurrentUserId()).addOnSuccessListener {
                FirebaseMessaging.getInstance().subscribeToTopic("/topics/${item.itemId}")
                try {
                    sendNotification(requireContext(), createNotification(item.userId, requireContext().getString(R.string.app_name), "Someone is interested in your article: ${item.title}"))
                } catch (e: JSONException) {
                    Log.e(TAG, "Failed to send notification.", e)
                }
                displayMessage(requireContext(), "Successfully showed interest")
            }.addOnFailureListener { displayMessage(requireContext(), "Failed to show interest") }
        } else {
            itemsVM.removeInterest(item, userVM.getCurrentUserId()).addOnSuccessListener {
                try {
                    sendNotification(requireContext(), createNotification(item.userId, requireContext().getString(R.string.app_name),"Someone is no more interested in your article: ${item.title}"))
                } catch (e: JSONException) {
                    Log.e(TAG, "Failed to send notification.", e)
                }
                FirebaseMessaging.getInstance().unsubscribeFromTopic("/topics/${item.itemId}")
                displayMessage(requireContext(), "Successfully removed interest")
            }.addOnFailureListener { displayMessage(requireContext(), "Failed to remove interest") }
        }
    }

    // Companion
    companion object {
        private const val TAG = "MM_OF_INTEREST"
    }
}