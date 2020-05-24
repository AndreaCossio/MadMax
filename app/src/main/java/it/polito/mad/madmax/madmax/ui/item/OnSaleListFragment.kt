package it.polito.mad.madmax.madmax.ui.item

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.ListenerRegistration
import it.polito.mad.madmax.madmax.*
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.model.ItemArg
import it.polito.mad.madmax.madmax.data.viewmodel.FilterViewModel
import it.polito.mad.madmax.madmax.data.viewmodel.ItemViewModel
import it.polito.mad.madmax.madmax.data.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_item_list.*

class OnSaleListFragment : Fragment() {

    // User
    private val userVM: UserViewModel by activityViewModels()

    // Items VM
    private val itemsVM: ItemViewModel by activityViewModels()
    private lateinit var itemAdapter: ItemAdapter

    // Filters
    private val filterVM: FilterViewModel by activityViewModels()

    // Item listener
    private lateinit var itemListener: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        itemAdapter = ItemAdapter(itemDetails, actionItem)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_item_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hideProgress(requireActivity())

        // Hide FAB because not used by this fragment
        hideFab(requireActivity())

        // Init recyclerview
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

        // Observe filters
        filterVM.getItemFilter().observe(viewLifecycleOwner, Observer {
            if (this::itemListener.isInitialized) {
                itemListener.remove()
            }
            itemListener = itemsVM.listenOthersItems(it)
        })

        // Observe userId
        userVM.getCurrentUserData().observe(viewLifecycleOwner, Observer {
            filterVM.setUserId(it.userId)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::itemListener.isInitialized) {
            itemListener.remove()
        }
        itemsVM.getItemList().value?.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        itemsVM.clearItemsData()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_filter_item, menu)

        val itemView = menu.findItem(R.id.menu_search)

        (itemView.actionView as SearchView).setOnQueryTextListener( object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.also { filterVM.setText(it) }
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
            setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_AppCompat_Dialog_Alert)
        }.show(requireFragmentManager(), TAG)
    }

    private var itemDetails = { item: Item ->
        requireActivity().main_progress.visibility = View.VISIBLE
        findNavController().navigate(OnSaleListFragmentDirections.actionDetailsItem(ItemArg("Details", item, item.userId == userVM.getCurrentUserData().value!!.userId)))
    }

    private var actionItem = { item: Item ->
        // TODO
        displayMessage(requireContext(), "Buy?")
    }

    // Companion
    companion object {
        const val TAG = "MM_ON_SALE"
    }
}