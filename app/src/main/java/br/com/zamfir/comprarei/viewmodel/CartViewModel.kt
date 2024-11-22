package br.com.zamfir.comprarei.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.data.model.entity.ByCategory
import br.com.zamfir.comprarei.data.model.entity.ByDate
import br.com.zamfir.comprarei.data.model.entity.ByValue
import br.com.zamfir.comprarei.data.model.entity.Cart
import br.com.zamfir.comprarei.data.model.entity.Filter
import br.com.zamfir.comprarei.data.repositories.CartRepository
import br.com.zamfir.comprarei.data.repositories.ConfigRepository
import br.com.zamfir.comprarei.data.repositories.ProductRepository
import br.com.zamfir.comprarei.data.repositories.UserRepository
import br.com.zamfir.comprarei.util.Constants
import br.com.zamfir.comprarei.util.convertMonetaryToDouble
import br.com.zamfir.comprarei.viewmodel.states.CartsState
import br.com.zamfir.comprarei.viewmodel.states.DeleteState
import br.com.zamfir.comprarei.viewmodel.states.SaveState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CartViewModel(private val repository: CartRepository, private val productsRepository: ProductRepository, private val configRepository: ConfigRepository, private val userRepository: UserRepository) : ViewModel() {

    private var _cartsState = MutableLiveData<CartsState>()
    val cartsState : LiveData<CartsState> get() = _cartsState

    private var _deleteState = MutableLiveData<DeleteState>()
    val deleteState : LiveData<DeleteState> get() = _deleteState

    private var _saveState = MutableLiveData<SaveState>()
    val saveState : LiveData<SaveState> get() = _saveState

    fun saveCart(cart: Cart) = viewModelScope.launch {
        val id = repository.saveCart(cart)
        _saveState.value = SaveState(savedId = id, savedItem = cart)
    }

    fun updateAllData() = viewModelScope.launch {
        _cartsState.value = CartsState(loading = true)

        val carts = repository.getCarts().onEach {
            it.category = repository.getCategoryById(it.categoryId)
        }

        val user = userRepository.getUserFromDb()

        val categories = repository.getCategories()

        _cartsState.value = CartsState(loading = false, carts = carts, categories = categories, isShowTotal = configRepository.getIsToShowTotalCart(), loggedUser = user)
    }

    fun deleteCarts(carts : List<Int>) = viewModelScope.launch{
        try{
            repository.deleteCarts(carts)
            carts.forEach {
                productsRepository.deleteProducts(it)
            }
        }catch (e : Exception){
            _deleteState.value = DeleteState(error = e)
        }
    }

    fun updateTotal(total: String, id: Int) {
        viewModelScope.launch {
            repository.updateTotal(total, id)
        }
    }

    fun updateOrder(reorderedList : List<Cart>) {
        viewModelScope.launch {
            reorderedList.forEachIndexed { index, cart ->
                cart.position = index
            }
            repository.updatePosition(reorderedList)
        }
    }

    fun updateCart(cart: Cart) {
        viewModelScope.launch {
            repository.updateCart(cart)
        }
    }

    private fun sortList(option: String, list: List<Cart>): List<Cart> {
        return when (option) {
            Constants.FILTER_NAME -> {
                list.sortedBy { it.name }
            }
            Constants.FILTER_DATE -> {
                list.sortedBy { it.data }
            }
            Constants.FILTER_VALUE_HIGH ->{
                list.sortedBy { it.total.convertMonetaryToDouble() }.reversed()
            }
            Constants.FILTER_VALUE_LOW ->{
                list.sortedBy { it.total.convertMonetaryToDouble() }
            }
            Constants.FILTER_CATEGORY -> {
                list.sortedBy { it.category?.description }
            }
            else -> list
        }
    }

    private fun filterByDate(byDate: ByDate, list: List<Cart>) : List<Cart>{
        return list.filter {
            compareDate(it.data, byDate)
        }
    }

    private fun compareDate(cartDate: String, byDate: ByDate) : Boolean{
        val localCartDate = LocalDate.parse(cartDate, DateTimeFormatter.ofPattern(Constants.DATE_PATTERN))

        return (localCartDate > byDate.startDate && localCartDate < byDate.endDate) || (localCartDate == byDate.startDate || localCartDate == byDate.endDate)
    }

    private fun filterByValue(value: ByValue, originalList: MutableList<Cart>): List<Cart> {
        return when(value.operator){
            Constants.OPERATOR_EQUAL -> {originalList.filter { it.total.convertMonetaryToDouble() == value.valueMin }}
            Constants.OPERATOR_GRATER_THAN -> {originalList.filter { it.total.convertMonetaryToDouble() > value.valueMin }}
            Constants.OPERATOR_LESS_THAN -> {originalList.filter { it.total.convertMonetaryToDouble() < value.valueMin }}
            Constants.OPERATOR_RANGE -> {originalList.filter { it.total.convertMonetaryToDouble() >= value.valueMin && it.total.convertMonetaryToDouble() <= value.valueMax }}
            else -> { originalList }
        }
    }

    private fun filterByCategory(category: ByCategory, originalList: MutableList<Cart>): List<Cart> {
        return originalList.filter{ it.category == category.category }
    }

    fun filter(filter: Filter, originalList: MutableList<Cart>): List<Cart> {
        var tempList = originalList

        filter.sortOption?.let {
            tempList = sortList(filter.sortOption ?: "", tempList).toMutableList()
        }

        filter.byDate?.let {
            tempList = filterByDate(it, tempList).toMutableList()
        }

        filter.byValue?.let {
            if(it.valueMin != 0.0 && it.valueMax != 0.0) tempList = filterByValue(it, tempList).toMutableList()
        }

        filter.byCategory?.let {
            tempList = filterByCategory(it, tempList).toMutableList()
        }

        return tempList
    }
}