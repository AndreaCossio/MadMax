package it.polito.mad.madmax.madmax.ui.item

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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

    // Realtime listener
    private lateinit var listener: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_item_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userVM.userId.observe(viewLifecycleOwner, Observer {
            if (it != "") {
                val itemAdapter = ItemAdapter(item_list_rv, itemDetails, actionItem)

                item_list_rv.apply {
                    setHasFixedSize(false)
                    layoutManager = AutoFitGridLayoutManager(requireContext(), 300.toPx())
                    adapter = itemAdapter
                }

                itemAdapter.myCount.observe(viewLifecycleOwner, Observer {
                    item_list_empty.visibility = if (it == 0) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                })

                listener = itemsVM.listenOnItems(false, itemAdapter)

                requireActivity().main_fab_add_item.visibility = View.GONE
                requireActivity().main_progress.visibility = View.GONE
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener.remove()
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