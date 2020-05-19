package it.polito.mad.madmax.madmax.ui.item

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.data.model.Item
import it.polito.mad.madmax.madmax.data.viewmodel.ItemViewModel
import it.polito.mad.madmax.madmax.data.viewmodel.ItemViewModelFactory
import it.polito.mad.madmax.madmax.data.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.fragment_item_list.*

class OnSaleListFragment : Fragment() {

    // Other's items
    private lateinit var othersItemsVM: ItemViewModel
    lateinit var itemsAdapter:ItemAdapter
    var minPrice: Double? = null
    var maxPrice: Double? = null
    var mainCategory: String? = null
    var subCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        setFragmentResultListener("searchFilters"){
                _, bundle ->
            minPrice = if(bundle.containsKey("minPrice")) bundle.getDouble("minPrice") else null
            maxPrice = if(bundle.containsKey("maxPrice")) bundle.getDouble("maxPrice") else null
            mainCategory = bundle.getString("mainCategory")
            subCategory = bundle.getString("subCategory")

            val othersItemsVM = activity?.run {
                ViewModelProvider(this).get(ItemViewModel::class.java)
            }

            val filteredItems = othersItemsVM!!.getOnSaleItems().value!!.filter {
                it.price in (minPrice?: 0.0)..(maxPrice?: Double.MAX_VALUE) &&
                it.category_main.contains(mainCategory!!) &&
                it.category_sub.contains(subCategory!!)
            } as ArrayList<Item>
            itemsAdapter.setItems(filteredItems)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)


        val userVM = activity?.run { ViewModelProvider(this).get(UserViewModel::class.java) }?.getID()
        othersItemsVM = ViewModelProvider(this,ItemViewModelFactory(userVM!!)).get(ItemViewModel::class.java)
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
                val filteredItems = othersItemsVM.getOnSaleItems().value!!.filter { it -> it.title.contains(p0!!) } as ArrayList<Item>
                //itemsAdapter.filter.filter(p0)
                itemsAdapter.setItems(filteredItems)
                return false
            }
        })

    }


    private fun showDialog(){
        val filterDialog = FilterDialog()
        val bundle = Bundle().apply {
            this.putString("minPrice",minPrice?.toString() ?: "")
            this.putString("maxPrice",maxPrice?.toString() ?: "")
            this.putString("mainCategory",mainCategory?: "")
            this.putString("subCategory",subCategory?: "")
        }


        filterDialog.arguments = bundle
        filterDialog.show(parentFragmentManager,"FilterDialog")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.menu_search ->{

                return true
            }
            R.id.menu_filter -> {
                showDialog()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        itemsAdapter = ItemAdapter(ArrayList<Item>(),item_list_rv,false)

            item_list_rv.apply {
                this.setHasFixedSize(true)
                layoutManager = LinearLayoutManager(this.context)
                adapter = itemsAdapter
                itemAnimator = DefaultItemAnimator()
            }


        othersItemsVM.getOnSaleItems().observe(this.requireActivity(), Observer {
            if(it.isEmpty()){
                item_list_rv.visibility = View.GONE
                empty_view.visibility = View.VISIBLE
            }
            else{
                item_list_rv.visibility = View.VISIBLE
                empty_view.visibility = View.GONE
                item_list_rv.apply {
                    itemsAdapter.setItems(it)
                }
            }
        })

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