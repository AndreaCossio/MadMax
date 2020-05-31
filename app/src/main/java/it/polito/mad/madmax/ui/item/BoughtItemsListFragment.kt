package it.polito.mad.madmax.ui.item

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.ListenerRegistration
import it.polito.mad.madmax.*
import it.polito.mad.madmax.data.model.Item
import it.polito.mad.madmax.data.viewmodel.ItemViewModel
import it.polito.mad.madmax.data.viewmodel.UserViewModel
import it.polito.mad.madmax.ui.AutoFitGridLayoutManager
import kotlinx.android.synthetic.main.fragment_item_list.*

class BoughtItemsListFragment : Fragment() {

    // Models
    private val userVM: UserViewModel by activityViewModels()
    private val itemsVM: ItemViewModel by activityViewModels()
    private lateinit var itemListener: ListenerRegistration

    // Adapter
    private lateinit var itemAdapter: ItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemAdapter = ItemAdapter(actionDetails, showAction = false)
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
        item_list_empty_tv.text = getString(R.string.message_empty_list_bought)

        // Observe the list of items and update the recycler view accordingly
        itemsVM.getItemList().observe(viewLifecycleOwner, Observer {
            itemAdapter.setItems(it)
            hideProgress(requireActivity())
            if (it.size == 0) {
                item_list_empty.animate().alpha(1F).startDelay = 300
            } else {
                item_list_empty.animate().alpha(0F).startDelay = 300
            }
        })

        itemListener = itemsVM.listenBoughtItems(userVM.getCurrentUserId())
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

    // Companion
    companion object {
        const val TAG = "MM_BOUGHT"
    }
}