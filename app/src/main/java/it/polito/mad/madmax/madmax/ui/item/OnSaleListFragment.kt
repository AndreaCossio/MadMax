package it.polito.mad.madmax.madmax.ui.item

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
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
import kotlinx.android.synthetic.main.fragment_item_list.*

class OnSaleListFragment : Fragment() {

    private lateinit var itemVM: ItemViewModel
    lateinit var itemsAdapter:ItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        super.onCreateView(inflater, container, savedInstanceState)


        itemVM = ViewModelProvider(this).get(ItemViewModel::class.java)



        // TODO BINDING???
      /*  itemVM.getOnSaleItems().observe(this.requireActivity(), Observer {
            item_list_rv.apply {
                adapter = ItemAdapter(it?: ArrayList<Item>(),this)
            }
        })
*/
        return inflater.inflate(R.layout.fragment_item_list, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_search_item,menu)

        val searchItem = menu.findItem(R.id.menu_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                val filteredItems = itemVM.getOnSaleItems().value!!.filter { it -> it.title.contains(p0!!) } as ArrayList<Item>
                //itemsAdapter.filter.filter(p0)
                itemsAdapter.setItems(filteredItems)
                return false
            }
        })

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.menu_search ->{

                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        itemsAdapter = ItemAdapter(ArrayList<Item>(),item_list_rv)

            item_list_rv.apply {
                this.setHasFixedSize(true)
                layoutManager = LinearLayoutManager(this.context)
                adapter = itemsAdapter
                itemAnimator = DefaultItemAnimator()
            }


        itemVM.getOnSaleItems().observe(this.requireActivity(), Observer {
            item_list_rv.apply {
                itemsAdapter.setItems(it)
            }
        })

        /*ArrayList<Item>().also {
            item_list_rv.apply {
                //check
                this.setHasFixedSize(true)
                layoutManager = AutoFitGridLayoutManager(requireContext(), 300.toPx())
                adapter = ItemAdapter(it, this)
                itemAnimator = DefaultItemAnimator()
            }
        }*/

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