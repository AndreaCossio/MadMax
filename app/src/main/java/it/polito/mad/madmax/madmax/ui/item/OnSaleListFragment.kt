package it.polito.mad.madmax.madmax.ui.item

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.google.android.material.floatingactionbutton.FloatingActionButton
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.viewmodel.ItemViewModel
import it.polito.mad.madmax.madmax.toPx
import kotlinx.android.synthetic.main.fragment_item_list.*

class OnSaleListFragment : Fragment() {

    private lateinit var itemVM: ItemViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        super.onCreateView(inflater, container, savedInstanceState)

        itemVM = ViewModelProvider(this).get(ItemViewModel::class.java)7

        // TODO BINDING???
        itemVM.getOnSaleItems().observe(this.requireActivity(), Observer {
            item_list_rv.apply {
                adapter = ItemAdapter(it?: ArrayList<Item>(),this)
            }
        })

        return inflater.inflate(R.layout.empty_item_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        itemVM.getOnSaleItems().value!!.also {
            item_list_rv.apply {
                //check
                this.setHasFixedSize(true)
                layoutManager = AutoFitGridLayoutManager(requireContext(), 300.toPx())
                adapter = ItemAdapter(it, this)
                itemAnimator = DefaultItemAnimator()
            }
        }

        activity?.findViewById<FloatingActionButton>(R.id.main_fab_add_item)?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.findViewById<FloatingActionButton>(R.id.main_fab_add_item)?.visibility = View.GONE
    }

    class AutoFitGridLayoutManager(context: Context?, columnWidth: Int) : GridLayoutManager(context, 1) {

        private var columnWidth = 0
        private var columnWidthChanged = true

        fun setColumnWidth(newColumnWidth: Int) {
            if (newColumnWidth > 0 && newColumnWidth != columnWidth) {
                columnWidth = newColumnWidth
                columnWidthChanged = true
            }
        }

        override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
            if (columnWidthChanged && columnWidth > 0) {
                val totalSpace = if (orientation == LinearLayoutManager.VERTICAL) {
                    width - paddingRight - paddingLeft
                } else {
                    height - paddingTop - paddingBottom
                }
                val spanCount = Math.max(1, totalSpace / columnWidth)
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