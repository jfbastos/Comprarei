package br.com.zamfir.comprarei.view.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.zamfir.comprarei.databinding.ProductItemBinding
import br.com.zamfir.comprarei.model.entity.Product
import br.com.zamfir.comprarei.util.FormatFrom

class ProductsAdapter : ListAdapter<Product, ProductsAdapter.ProductsViewHolder>(differCallback) {

    var selectionMode = false
    var selectedItems = mutableListOf<Product>()
    val differ = AsyncListDiffer(this, differCallback)

    inner class ProductsViewHolder(private val binding: ProductItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.productName.text = product.name

            binding.productQuantity.text =
                if (product.quantity < 10) "0${product.quantity}" else product.quantity.toString()

            binding.productValue.text = FormatFrom.doubleToMonetary("R$", product.price)
            binding.productTotal.text =
                FormatFrom.doubleToMonetary("R$", product.price * product.quantity)

            if(product.done){
                binding.productName.paintFlags = binding.productName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            }else{
                binding.productName.paintFlags = 0
            }

            binding.selectedBackground.visibility = if(selectedItems.contains(product)) View.VISIBLE else View.INVISIBLE

            binding.root.apply {
               setListeners(product)
            }
        }

        //Region Selection
        private fun ConstraintLayout.setListeners(product: Product) {
            this.setOnClickListener {
                onItemClickListener?.invoke(product, adapterPosition)
            }
        }
        //endregion
    }

    companion object {
        private val differCallback: DiffUtil.ItemCallback<Product> =
            object : DiffUtil.ItemCallback<Product>() {
                override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
                    return oldItem.id == newItem.id
                }
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductsViewHolder {
        val binding = ProductItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductsViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    private var onItemClickListener: ((Product, Int) -> Unit)? = null

    fun setOnItemClickListener(clickListener: (Product, Int) -> Unit) {
        onItemClickListener = clickListener
    }
}