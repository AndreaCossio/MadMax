package it.polito.mad.madmax.ui.item

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.DialogFragment
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
import it.polito.mad.madmax.data.viewmodel.FilterViewModel
import it.polito.mad.madmax.data.viewmodel.ItemViewModel
import it.polito.mad.madmax.data.viewmodel.UserViewModel
import it.polito.mad.madmax.ui.AutoFitGridLayoutManager
import kotlinx.android.synthetic.main.fragment_item_list.*
import org.json.JSONException

class OnSaleListFragment : Fragment() {

    // Models
    private val userVM: UserViewModel by activityViewModels()
    private val itemsVM: ItemViewModel by activityViewModels()
    private val filterVM: FilterViewModel by activityViewModels()
    private lateinit var itemListener: ListenerRegistration

    // Adapter
    private lateinit var itemAdapter: ItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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
        item_list_empty_tv.text = getString(R.string.message_empty_list_others)

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

        // Observe filters
        filterVM.getItemFilter().observe(viewLifecycleOwner, Observer {
            if (this::itemListener.isInitialized) {
                itemListener.remove()
            }
            itemListener = itemsVM.listenItems(itemFilter = it)
        })

        // Observe userId
        userVM.getCurrentUserData().observe(viewLifecycleOwner, Observer {
            if (it.userId == "") {
                if (this::itemListener.isInitialized) {
                    itemListener.remove()
                }
                showProgress(requireActivity())
            } else {
                filterVM.updateUserId(it.userId)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::itemListener.isInitialized) {
            itemListener.remove()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_filter_item, menu)

        val searchView = (menu.findItem(R.id.menu_search).actionView as SearchView)

        searchView.setOnQueryTextListener( object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.also { filterVM.updateText(it) }
                return false
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.menu_search -> {
                true
            }
            R.id.menu_filter -> {
                openFilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openFilterDialog() {
        ItemFilterDialog().apply {
            setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_MadMax_Dialog)
        }.show(requireFragmentManager(), TAG)
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
        private const val TAG = "MM_ON_SALE"
    }
}