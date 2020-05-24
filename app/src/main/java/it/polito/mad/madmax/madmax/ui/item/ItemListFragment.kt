package it.polito.mad.madmax.madmax.ui.item

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.ListenerRegistration
import it.polito.mad.madmax.madmax.*
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.ItemArg
import it.polito.mad.madmax.madmax.data.viewmodel.ItemViewModel
import it.polito.mad.madmax.madmax.data.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_item_list.*

class ItemListFragment : Fragment() {

    // User
    private val userVM: UserViewModel by activityViewModels()

    // Items VM
    private val itemsVM: ItemViewModel by activityViewModels()
    private lateinit var itemAdapter: ItemAdapter

    // Item listener
    private lateinit var itemListener: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemAdapter = ItemAdapter(itemDetails, actionEditItem, {null})
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_item_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hideProgress(requireActivity())

        item_list_rv.apply {
            setHasFixedSize(false)
            layoutManager = AutoFitGridLayoutManager(requireContext(), 300.toPx())
            adapter = itemAdapter
        }

        // Observe the list of items
        itemsVM.getItemList().observe(viewLifecycleOwner, Observer {
            itemAdapter.setItems(it)
            if (itemAdapter.itemCount == 0) {
                item_list_empty.visibility = View.VISIBLE
            } else {
                item_list_empty.visibility = View.GONE
                // TODO check (better layout text? padding?)
                (item_list_empty.getChildAt(0) as TextView).text = getString(R.string.message_empty_list_others)
            }
        })

        // Observe userId
        userVM.getCurrentUserData().observe(viewLifecycleOwner, Observer {
            if (it.userId == "") {
                if (this::itemListener.isInitialized) {
                    itemListener.remove()
                }
            } else {
                itemListener = itemsVM.listenMyItems(it.userId)
            }
        })

        // Init FAB
        requireActivity().main_fab_add_item.setOnClickListener { findNavController().navigate(ItemListFragmentDirections.actionEditOrCreateItem(ItemArg("Create", Item(userId = userVM.getCurrentUserData().value!!.userId), true))) }
        requireActivity().main_fab_add_item.setImageDrawable(requireContext().getDrawable(R.drawable.ic_add))
        showFab(requireActivity())
        hideProgress(requireActivity())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::itemListener.isInitialized) {
            itemListener.remove()
        }
        requireActivity().main_fab_add_item.setOnClickListener(null)
        itemsVM.getItemList().value?.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        itemsVM.clearItemsData()
    }

    private var itemDetails = { item: Item ->
        showProgress(requireActivity())
        findNavController().navigate(ItemListFragmentDirections.actionEditOrCreateItem(ItemArg("Details", item, item.userId == userVM.getCurrentUserData().value!!.userId)))
    }

    private var actionEditItem = { item: Item ->
        showProgress(requireActivity())
        findNavController().navigate(ItemListFragmentDirections.actionEditOrCreateItem(ItemArg("Edit", item, item.userId == userVM.getCurrentUserData().value!!.userId)))
    }
}