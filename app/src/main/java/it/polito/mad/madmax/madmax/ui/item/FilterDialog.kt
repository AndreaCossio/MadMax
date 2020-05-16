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
        return inflater.inflate(R.layout.filter_layout,container,false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        retrieveFilters(requireArguments())
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun retrieveFilters(bundle: Bundle){

        val minPrice = bundle.getString("minPrice")
        val maxPrice = bundle.getString("maxPrice")
        val mainCat = bundle.getString("mainCategory")?: getString(R.string.empty_filter)
        val subCat = bundle.getString("subCategory")?: getString(R.string.empty_filter)

        item_edit_minPrice.setText(minPrice)
        item_edit_maxPrice.setText(maxPrice)
        if(mainCat.isNotEmpty()) {
            val selectedMainIndex = resources.getStringArray(R.array.item_categories_main).indexOf(mainCat)
            item_edit_category_main.setSelection(selectedMainIndex+1)

            /*val subCatElements = mapMainCategoryToSubCategory(mainCat)
            adaptSubCategoryElements(subCatElements)*/

            if(subCat.isNotEmpty()){
                val selectedSubIndex = resources.getStringArray(mapMainCategoryToSubCategory(mainCat)).indexOf(subCat)
                item_edit_category_sub.setSelection(selectedSubIndex+1)
            }
        }
        else item_edit_category_main.setSelection(0)

    }

    private fun mapMainCategoryToSubCategory(text: String):Int{
        return when (text) {
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
    }

    private fun adaptSubCategoryElements(secondList: Int){
        lateinit var elements:List<String>

        if(secondList == R.string.empty_filter){
            elements = listOf(getString(secondList))
        }else{
            val categories = resources.getStringArray(secondList)
            val list = categories.toMutableList<String>()
            list.add(0,"- - - - -")
            elements = list

        }
        //val elements = resources.getStringArray(secondList)
        val dataAdapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, elements)
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dataAdapter.notifyDataSetChanged()
        item_edit_category_sub.adapter = dataAdapter
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (parent?.id == R.id.item_edit_category_main) {
            val text: String = parent.getItemAtPosition(position).toString()
            val secondList = mapMainCategoryToSubCategory(text)
            adaptSubCategoryElements(secondList)
        }

    }

    private fun applyFilters(){
        val minPrice = item_edit_minPrice.text?.toString()?.toDoubleOrNull()
        val maxPrice = item_edit_maxPrice.text?.toString()?.toDoubleOrNull()
        val mainCategory = if(item_edit_category_main.selectedItem?.toString() == getString(R.string.empty_filter)) "" else  item_edit_category_main.selectedItem?.toString()
        val subCategory = if(item_edit_category_sub.selectedItem?.toString() == getString(R.string.empty_filter)) "" else  item_edit_category_sub.selectedItem?.toString()

        val bundle = Bundle()
        bundle.apply {
            if(minPrice != null) this.putDouble("minPrice",minPrice)
            if(maxPrice != null) this.putDouble("maxPrice",maxPrice)
            this.putString("mainCategory",mainCategory)
            this.putString("subCategory",subCategory)
        }


        setFragmentResult("searchFilters", bundle)
        dismiss()
    }


}