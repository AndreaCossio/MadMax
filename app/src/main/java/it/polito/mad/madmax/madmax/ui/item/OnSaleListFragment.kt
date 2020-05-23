package it.polito.mad.madmax.madmax.ui.item

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ListenerRegistration
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.data.viewmodel.ItemViewModel
import it.polito.mad.madmax.madmax.data.viewmodel.UserViewModel
import it.polito.mad.madmax.madmax.displayMessage
import it.polito.mad.madmax.madmax.toPx
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_item_list.*

class OnSaleListFragment : Fragment() {

    // Items VM
    private val userVM: UserViewModel by activityViewModels()
    private val itemsVM: ItemViewModel by activityViewModels()
    private var itemAdapter: ItemAdapter? = null

    // Realtime listener
    private lateinit var listener: ListenerRegistration

    // Filters
    var minPrice: Double? = null
    var maxPrice: Double? = null
    var mainCategory: String? = null
    var subCategory: String? = null

    companion object {
        const val TAG = "MM_ON_SALE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        setFragmentResultListener("item_filter") { _, bundle ->
            minPrice = if(bundle.containsKey("minPrice")) bundle.getDouble("minPrice") else null
            maxPrice = if(bundle.containsKey("maxPrice")) bundle.getDouble("maxPrice") else null
            mainCategory = bundle.getString("mainCategory")
            subCategory = bundle.getString("subCategory")

            itemAdapter?.setFilters(minPrice, maxPrice, mainCategory, subCategory)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_item_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userVM.getCurrentUserData().observe(viewLifecycleOwner, Observer {
            if (it.userId != "") {
                itemAdapter = ItemAdapter(item_list_rv, itemDetails, actionItem).apply {
                    setFilters(minPrice, maxPrice, mainCategory, subCategory)
                }

                item_list_rv.apply {
                    setHasFixedSize(false)
                    layoutManager = AutoFitGridLayoutManager(requireContext(), 300.toPx())
                    adapter = itemAdapter
                }

                itemAdapter?.myCount?.observe(viewLifecycleOwner, Observer { count ->
                    item_list_empty.visibility = if (count == 0) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                    (item_list_empty.getChildAt(0) as TextView).text = getString(R.string.message_empty_list_others)
                })

                listener = itemsVM.listenOnItems(false, itemAdapter)

                requireActivity().main_fab_add_item?.visibility = View.GONE
                requireActivity().main_progress.visibility = View.GONE
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::listener.isInitialized)
        listener.remove()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_filter_item, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.menu_filter -> {
                openfilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        minPrice?.also { outState.putDouble("filter_minprice", it) }
        maxPrice?.also { outState.putDouble("filter_maxprice", it) }
        mainCategory?.also { outState.putString("filter_maincat", it) }
        subCategory?.also { outState.putString("filter_subcat", it) }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.also {
            if (it.containsKey("filter_minprice")) minPrice = it.getDouble("filter_minprice")
            if (it.containsKey("filter_maxprice")) maxPrice = it.getDouble("filter_maxprice")
            if (it.containsKey("filter_maincat")) mainCategory = it.getString("filter_maincat")
            if (it.containsKey("filter_subcat")) subCategory = it.getString("filter_subcat")
        }
    }

    private fun openfilterDialog() {
        val filterDialog = FilterDialog()
        val bundle = Bundle().apply {
            this.putString("minPrice", minPrice?.toString() ?: "")
            this.putString("maxPrice", maxPrice?.toString() ?: "")
            this.putString("mainCategory", mainCategory?: "")
            this.putString("subCategory", subCategory?: "")
        }

        filterDialog.arguments = bundle
        filterDialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_AppCompat_Dialog_Alert)
        filterDialog.show(requireFragmentManager(), TAG)
    }

    private var itemDetails = { itemId: String ->
        requireActivity().main_progress.visibility = View.VISIBLE
        findNavController().navigate(OnSaleListFragmentDirections.actionDetailsItem("N-details-$itemId"))
    }

    private var actionItem = { itemId: String ->
        displayMessage(requireContext(), "Hey")
    }

    class AutoFitGridLayoutManager(context: Context?, columnWidth: Int) : GridLayoutManager(context, 1) {

        private var columnWidth = 0
        private var columnWidthChanged = true

        private fun setColumnWidth(newColumnWidth: Int) {
            if (newColumnWidth > 0 && newColumnWidth != columnWidth) {
                columnWidth = newColumnWidth
                columnWidthChanged = true
            }
        }

        override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
            if (columnWidthChanged && columnWidth > 0) {
                val totalSpace = if (orientation == LinearLayoutManager.VERTICAL) {
                    width - paddingRight - paddingLeft
                } else {
                    height - paddingTop - paddingBottom
                }
                val spanCount = 1.coerceAtLeast(totalSpace / columnWidth)
                setSpanCount(spanCount)
                columnWidthChanged = false
            }
            super.onLayoutChildren(recycler, state)
        }

        init {
            setColumnWidth(columnWidth)
        }
    }
}