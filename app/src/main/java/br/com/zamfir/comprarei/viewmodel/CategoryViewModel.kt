package br.com.zamfir.comprarei.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.model.entity.Category
import br.com.zamfir.comprarei.repositories.CategoryRepository
import br.com.zamfir.comprarei.viewmodel.states.DeleteState
import kotlinx.coroutines.launch

class CategoryViewModel(private val categoryRepository: CategoryRepository) : ViewModel() {

    private var _categories = MutableLiveData<List<Category>>()
    val categories : LiveData<List<Category>> get() = _categories

    private var _deleteState = MutableLiveData<DeleteState>()
    val deleteState : LiveData<DeleteState> get() = _deleteState


    fun getCategories() =  viewModelScope.launch {
        _categories.value = categoryRepository.getCategories()
    }

    fun saveCategory(category: Category) = viewModelScope.launch {
        categoryRepository.saveCategory(category)
    }

    fun updateCategory(category: Category) = viewModelScope.launch {
        categoryRepository.updateCategory(category)
    }

    fun deleteCategory(category: Category, deletionConfirmed : Boolean) = viewModelScope.launch {
        if(categoryRepository.hasCartWithCategorie(category.id) && !deletionConfirmed){
            _deleteState.value = DeleteState(canDelete = false, itemDeleted = category)
        }else{
            categoryRepository.deleteCategory(category)
            _deleteState.value = DeleteState(itemDeleted = category)
        }
    }

    fun saveCategories(categories : List<Category>) = viewModelScope.launch {
        categoryRepository.saveCategories(categories)
    }
}

