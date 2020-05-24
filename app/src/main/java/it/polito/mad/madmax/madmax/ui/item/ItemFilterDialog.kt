package it.polito.mad.madmax.madmax.ui.item

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import it.polito.mad.madmax.madmax.*
import it.polito.mad.madmax.madmax.data.model.ItemFilter
import it.polito.mad.madmax.madmax.data.viewmodel.FilterViewModel
import kotlinx.android.synthetic.main.filter_layout.*


class ItemFilterDialog: DialogFragment(), AdapterView.OnItemSelectedListener {

    // Filter values
    private val filterVM: FilterViewModel by activityViewModels()
    private lateinit var filters: ItemFilter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filters = filterVM.getItemFilter().value!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.filter_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Create adapter for the main category
        filter_dialog_main_cat.adapter = getMainCategoryAdapter(requireContext())

        // Init views
        if (filters.minPrice != -1.0) {
            filter_dialog_min_price.setText(String.format("%.2f", filters.minPrice))
        }
        if (filters.maxPrice != -1.0) {
            filter_dialog_max_price.setText(String.format("%.2f", filters.maxPrice))
        }
        filter_dialog_main_cat.setSelection(getMainCategories(requireContext()).indexOf(filters.mainCategory))
        filter_dialog_favourites.isChecked = filters.onlyFavourite

        // Init listeners
        filter_dialog_main_cat.onItemSelectedListener = this
        filter_dialog_apply.setOnClickListener { applyFilters() }
        filter_dialog_cancel.setOnClickListener { dismiss() }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        filter_dialog_main_cat.setSelection(0)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (parent?.id == R.id.filter_dialog_main_cat) {
            filter_dialog_sub_cat.adapter = getSubCategoryAdapter(requireContext(), parent.getItemAtPosition(position).toString())

            if (filters.subCategory != "") {
                filter_dialog_sub_cat.setSelection(getSubcategories(requireContext(), parent.getItemAtPosition(position).toString()).indexOf(filters.subCategory))
            }
        }
    }

    private fun applyFilters() {
        filterVM.setFiltersNoUserId(ItemFilter(
            filter_dialog_min_price.text.toString().toDoubleOrNull() ?: -1.0,
            filter_dialog_max_price.text.toString().toDoubleOrNull() ?: -1.0,
            filter_dialog_main_cat.selectedItem.toString(),
            filter_dialog_sub_cat.selectedItem.toString(),
            onlyFavourite = filter_dialog_favourites.isChecked
        ))
        dismiss()
    }
}