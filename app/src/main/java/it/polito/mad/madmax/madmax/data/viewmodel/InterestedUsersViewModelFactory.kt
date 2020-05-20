package it.polito.mad.madmax.madmax.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class InterestedUsersViewModelFactory(private val itemId: String): ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return InterestedUsersViewModel(itemId) as T
    }
}