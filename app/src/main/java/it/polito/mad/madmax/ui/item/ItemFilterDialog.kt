package it.polito.mad.madmax.ui.item

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import it.polito.mad.madmax.R
import it.polito.mad.madmax.data.model.ItemFilter
import it.polito.mad.madmax.data.viewmodel.FilterViewModel
import it.polito.mad.madmax.getFragmentSpaceSize
import it.polito.mad.madmax.getMainCategoryAdapter
import it.polito.mad.madmax.getSubCategoryAdapter
import kotlinx.android.synthetic.main.item_filter_dialog.*

class ItemFilterDialog : DialogFragment(), AdapterView.OnItemClickListener {

    // Filter
    private val filterVM: FilterViewModel by activityViewModels()
    private lateinit var filters: ItemFilter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load filters
        filters = filterVM.getItemFilter().value!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Resize the dialog when the keyboard opens
        dialog?.window?.apply {
            setLayout(getFragmentSpaceSize(requireContext()).x, getFragmentSpaceSize(requireContext()).y)
            setBackgroundDrawableResource(android.R.color.transparent)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        return inflater.inflate(R.layout.item_filter_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init price
        filters.minPrice?.also {
            filter_dialog_min_price.setText(String.format("%.2f", filters.minPrice))
        }
        filters.maxPrice?.also {
            filter_dialog_max_price.setText(String.format("%.2f", filters.maxPrice))
        }

        // Init categories
        filter_dialog_main_cat.setAdapter(getMainCategoryAdapter(requireContext()))
        filter_dialog_sub_cat.setAdapter(getSubCategoryAdapter(requireContext(), filters.mainCategory))
        filter_dialog_main_cat.setText(filters.mainCategory, false)
        filter_dialog_sub_cat.setText(filters.subCategory, false)

        // When focusing a drop down, close the keyboard if open
        val focusListener = View.OnFocusChangeListener { v: View, b: Boolean ->
            if (b) (v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(view.windowToken, 0)
        }
        filter_dialog_main_cat.onFocusChangeListener = focusListener
        filter_dialog_sub_cat.onFocusChangeListener = focusListener

        // Listen for main category change
        filter_dialog_main_cat.onItemClickListener = this

        // Init dialog actions
        filter_dialog_apply.setOnClickListener { applyFilters() }
        filter_dialog_clear.setOnClickListener { clearFilters() }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        filter_dialog_main_cat.setAdapter(getMainCategoryAdapter(requireContext()))
        filter_dialog_sub_cat.setAdapter(getSubCategoryAdapter(requireContext(), filter_dialog_main_cat.text.toString()))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_filter_item, menu)
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        filter_dialog_sub_cat.setAdapter(getSubCategoryAdapter(requireContext(), parent!!.getItemAtPosition(position).toString()))
        filter_dialog_sub_cat.setText("", false)
    }

    private fun clearFilters() {
        filter_dialog_max_price.setText("")
        filter_dialog_min_price.setText("")
        filter_dialog_main_cat.setText("")
        filter_dialog_sub_cat.setText("")
        filter_dialog_sub_cat.setAdapter(getSubCategoryAdapter(requireContext(), ""))
        dialog?.window?.currentFocus?.clearFocus()
    }

    private fun applyFilters() {
        filterVM.updateDialogFilters(
            ItemFilter(
                filter_dialog_min_price.text.toString().toDoubleOrNull(),
                filter_dialog_max_price.text.toString().toDoubleOrNull(),
                filter_dialog_main_cat.text.toString(),
                filter_dialog_sub_cat.text.toString()
            )
        )
        dismiss()
    }
}