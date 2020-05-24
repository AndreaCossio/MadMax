package it.polito.mad.madmax.madmax.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import it.polito.mad.madmax.madmax.data.model.ItemFilter

class FilterViewModel: ViewModel() {

    private val filter: MutableLiveData<ItemFilter> by lazy {
        MutableLiveData<ItemFilter>().apply {
            value = ItemFilter()
        }
    }

    fun getItemFilter(): MutableLiveData<ItemFilter> {
        return filter
    }

    fun setFiltersNoUserId(itemFilter: ItemFilter) {
        filter.value = itemFilter.apply {
            filter.value?.also {
                userId = it.userId
                text = it.text
            }
        }
    }

    fun setText(text: String) {
        filter.value = filter.value?.apply {
            this.text = text
        } ?: ItemFilter(text = text)
    }

    fun setUserId(userId: String) {
        filter.value = filter.value?.apply {
            this.userId = userId
        } ?: ItemFilter(userId = userId)
    }
}