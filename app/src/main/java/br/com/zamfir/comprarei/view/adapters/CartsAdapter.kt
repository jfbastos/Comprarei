package br.com.zamfir.comprarei.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.zamfir.comprarei.databinding.CartItemBinding
import br.com.zamfir.comprarei.model.entity.Cart
import br.com.zamfir.comprarei.util.setVisibility

class CartsAdapter : ListAdapter<Cart, CartsAdapter.CartsViewHolder>(differCallback) {

    var selectedItems = mutableListOf<Cart>()
    val differ = AsyncListDiffer(this, differCallback)

    inner class CartsViewHolder(private val binding: CartItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(cart: Cart) {
            binding.cartName.text = cart.name
            binding.cartDate.text = cart.data
            binding.totalCart.text = cart.total
            binding.selectedBackground.visibility = if(selectedItems.contains(cart)) View.VISIBLE else View.INVISIBLE
            cart.category?.let {
                binding.categoryPlaceHolder.setVisibility(true)
                binding.categoryColor.setColorFilter(it.color, android.graphics.PorterDuff.Mode.MULTIPLY)
            } ?: run {
                binding.categoryPlaceHolder.setVisibility(false)
            }

            binding.root.apply {
                this.setOnClickListener {
                    onItemClickListener?.invoke(cart)
                }
            }
        }
    }

    companion object {
        private val differCallback: DiffUtil.ItemCallback<Cart> = object : DiffUtil.ItemCallback<Cart>() {
            override fun areItemsTheSame(oldItem: Cart, newItem: Cart): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Cart, newItem: Cart): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartsViewHolder {
        val binding = CartItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CartsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartsViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    private var onItemClickListener: ((Cart) -> Unit)? = null

    fun setOnItemClickListener(clickListener: (Cart) -> Unit) {
        onItemClickListener = clickListener
    }
}