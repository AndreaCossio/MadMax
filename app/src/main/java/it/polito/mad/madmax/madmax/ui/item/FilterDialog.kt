package it.polito.mad.madmax.madmax.ui.item

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import it.polito.mad.madmax.madmax.R
import kotlinx.android.synthetic.main.filter_layout.*


class FilterDialog: DialogFragment(), AdapterView.OnItemSelectedListener{

    interface OnFiltersApplied {
        fun sendInput(input: String?)
    }

    var onFiltersApplied: OnFiltersApplied? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("FILTERS","LDJLJDLJALDJDLJ")
        return inflater.inflate(R.layout.filter_layout,container,false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("FILTERS","LDJLJDLJALDJDLJ")
        apply_filter.setOnClickListener {
            applyFilters()
        }
        cancel_filter.setOnClickListener{ dismiss() }

        val categories = resources.getStringArray(R.array.item_categories_main)
        val list = categories.toMutableList<String>()
        list.add(0,"- - - - -")
        val dataAdapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, list)
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dataAdapter.notifyDataSetChanged()
        item_edit_category_main.adapter = dataAdapter
        item_edit_category_main.onItemSelectedListener = this
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (parent?.id == R.id.item_edit_category_main) {
            val text: String = parent.getItemAtPosition(position).toString()
            val secondList: Int = when (text) {
                "Arts & Crafts" -> { R.array.item_categories_sub_art_and_crafts }
                "Sports & Hobby" -> { R.array.item_categories_sub_sports_and_hobby }
                "Baby" -> { R.array.item_categories_sub_baby }
                "Women\'s fashion" -> { R.array.item_categories_sub_womens_fashion }
                "Men\'s fashion" -> { R.array.item_categories_sub_mens_fashion }
                "Electronics" -> { R.array.item_categories_sub_electronics }
                "Games & Videogames" -> { R.array.item_categories_sub_games_and_videogames }
                "Automotive" -> { R.array.item_categories_sub_automotive }
                "- - - - -"->{R.string.empty_filter}

                else -> throw Exception()
            }

            lateinit var elements:Array<String>
            if(secondList == R.string.empty_filter){
                elements = arrayOf(getString(secondList))
            }else{
                elements = resources.getStringArray(secondList)

            }
            //val elements = resources.getStringArray(secondList)
            val dataAdapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, elements)
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dataAdapter.notifyDataSetChanged()
            item_edit_category_sub.adapter = dataAdapter
        }
    }

    private fun applyFilters(){
        val minPrice = item_edit_minPrice.text?.toString()?.toDoubleOrNull() ?: 0.0
        val maxPrice = item_edit_maxPrice.text?.toString()?.toDoubleOrNull() ?: Double.MAX_VALUE
        val mainCategory = if(item_edit_category_main.selectedItem?.toString() == getString(R.string.empty_filter)) "" else  item_edit_category_main.selectedItem?.toString()
        val subCategory = if(item_edit_category_sub.selectedItem?.toString() == getString(R.string.empty_filter)) "" else  item_edit_category_sub.selectedItem?.toString()

        val bundle = Bundle()
        bundle.putDouble("minPrice",minPrice)
        bundle.putDouble("maxPrice",maxPrice)
        bundle.putString("mainCategory",mainCategory)
        bundle.putString("subCategory",subCategory)

        setFragmentResult("searchFilters", bundle)
        dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putDouble("minPrice",4.5)
    }

}