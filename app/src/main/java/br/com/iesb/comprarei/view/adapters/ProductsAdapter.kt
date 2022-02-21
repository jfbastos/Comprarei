package br.com.iesb.comprarei.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.iesb.comprarei.databinding.ProductItemBinding
import br.com.iesb.comprarei.model.Product
import br.com.iesb.comprarei.util.FormatFrom

class ProductsAdapter : ListAdapter<Product, ProductsAdapter.ProductsViewHolder>(differCallback) {

    var selectionMode = false
    private var selectedItems = mutableListOf<Product>()
    val differ = AsyncListDiffer(this, differCallback)

    inner class ProductsViewHolder(val binding: ProductItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.productName.text = product.name
            binding.productQuantity.text =
                if (product.quantity < 10) "0${product.quantity}" else product.quantity.toString()
            binding.productValue.text = FormatFrom.doubleToMonetary("R$", product.price)
            binding.productTotal.text =
                FormatFrom.doubleToMonetary("R$", product.price * product.quantity)

            checkSelectionMode()

            binding.checkForDelete.setOnClickListener {
                setCheckBox(product)
            }

            binding.root.apply {
                this.setOnClickListener {
                    onItemClickListener?.invoke(product, adapterPosition)
                }

                if (selectionMode) {
                    this.setOnClickListener {
                        binding.checkForDelete.isChecked = !binding.checkForDelete.isChecked
                        setCheckBox(product)
                    }
                }

                setOnLongClickListener {
                    binding.checkForDelete.isChecked = !binding.checkForDelete.isChecked
                    setCheckBox(product)
                    onLongItemClickListener.invoke(product)
                }
            }
        }

        private fun checkSelectionMode() {
            if (selectionMode) {
                binding.checkForDelete.visibility = View.VISIBLE
            } else {
                binding.checkForDelete.isChecked = false
                binding.checkForDelete.visibility = View.INVISIBLE
            }
        }
    }

    private fun ProductsViewHolder.setCheckBox(product: Product) {
        if (binding.checkForDelete.isChecked) {
            selectedItems.add(product)
        } else if (selectedItems.contains(product)) {
            selectedItems.remove(product)
        }
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

    fun getSelectedItems(): List<Product> {
        return selectedItems
    }

    fun clearSelectedItems() {
        selectedItems.clear()
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    private var onItemClickListener: ((Product, Int) -> Unit)? = null

    fun setOnItemClickListener(clickListener: (Product, Int) -> Unit) {
        onItemClickListener = clickListener
    }

    private lateinit var onLongItemClickListener: ((Product) -> Boolean)

    fun setOnLongItemClickListener(longClickListener: (Product) -> Boolean) {
        onLongItemClickListener = longClickListener
    }

}