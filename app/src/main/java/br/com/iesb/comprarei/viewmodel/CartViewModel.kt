package br.com.iesb.comprarei.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.iesb.comprarei.interfaces.TaskListener
import br.com.iesb.comprarei.model.Cart
import br.com.iesb.comprarei.model.CartRepository
import br.com.iesb.comprarei.model.ProductRepository
import kotlinx.coroutines.launch

class CartViewModel(private val repository: CartRepository, private val productsRepository: ProductRepository) : ViewModel() {

    lateinit var listOfCarts : LiveData<List<Cart>>

    fun getCarts() {
        repository.getCarts(object : TaskListener{
            override fun onSuccess(`object`: Any?) {
                Log.d("CartViewModel", "Carts retrieved successfully")
                listOfCarts = `object` as LiveData<List<Cart>>
            }

            override fun onError(`object`: Any?) {
                Log.e("CartRepository", "Error : ${(`object` as Exception).message}")
            }
        })
    }

    fun saveCart(name : String, date : String) {
        viewModelScope.launch {
            repository.saveCart(name, date, object : TaskListener{
                override fun onSuccess(`object`: Any?) {
                    Log.d("CartViewModel", "Cart $name save successfully")
                }

                override fun onError(`object`: Any?) {
                    Log.e("CartRepository", "Error : ${(`object` as Exception).message}")
                }
            })
        }
    }

    fun deleteCart(cart : Cart){
        viewModelScope.launch{
            repository.deleteCart(cart.id, object : TaskListener{
                override fun onSuccess(`object`: Any?) {
                    Log.d("CartViewModel", "Cart ${cart.name} successfully deleted")
                    viewModelScope.launch {
                        productsRepository.deleteProducts(cart.id)
                    }
                }

                override fun onError(`object`: Any?) {
                    Log.e("CartRepository", "Error : ${(`object` as Exception).message}")
                }
            })
        }
    }

    fun updateTotal(total : String, id : String){
        viewModelScope.launch {
            repository.updateTotal(total, id, object : TaskListener{
                override fun onSuccess(`object`: Any?) {
                    Log.d("CartViewModel", "Cart total successfully update to $total")
                }

                override fun onError(`object`: Any?) {
                    Log.e("CartRepository", "Error : ${(`object` as Exception).message}")
                }
            })
        }

    }

    fun sortList(option : Int, list : List<Cart>) : List<Cart>{
        return when (option) {
            0 -> {
                list.sortedBy { it.name }
            }
            1 -> {
                list.sortedBy { it.data }
            }
            else -> {
                list
            }
        }
    }

}