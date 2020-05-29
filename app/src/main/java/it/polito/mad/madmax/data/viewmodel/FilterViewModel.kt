package it.polito.mad.madmax.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import it.polito.mad.madmax.data.model.ItemFilter

class FilterViewModel: ViewModel() {

    private val filter: MutableLiveData<ItemFilter> by lazy {
        MutableLiveData<ItemFilter>().apply {
            value = ItemFilter()
        }
    }

    fun getItemFilter(): MutableLiveData<ItemFilter> {
        return filter
    }

    fun updateDialogFilters(itemFilter: ItemFilter) {
        filter.value = filter.value?.apply {
            this.minPrice = itemFilter.minPrice
            this.maxPrice = itemFilter.maxPrice
            this.mainCategory = itemFilter.mainCategory
            this.subCategory = itemFilter.subCategory
        } ?: itemFilter
    }

    fun updateText(text: String) {
        filter.value = filter.value?.apply {
            this.text = text
        } ?: ItemFilter(text = text)
    }

    fun updateUserId(userId: String) {
        filter.value = filter.value?.apply {
            this.userId = userId
        } ?: ItemFilter(userId = userId)
    }
}