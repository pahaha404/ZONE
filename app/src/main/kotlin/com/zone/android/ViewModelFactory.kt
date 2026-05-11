package com.zone.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModelProvider

/**
 * Simple ViewModel factory for constructor-injected view models.
 */
class SingleViewModelFactory<VM : ViewModel>(
    private val creator: () -> VM,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = creator() as T
}
