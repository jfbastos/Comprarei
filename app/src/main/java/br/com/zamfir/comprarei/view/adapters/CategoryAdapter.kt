package br.com.zamfir.comprarei.view.adapters

import android.content.DialogInterface.OnClickListener
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import br.com.zamfir.comprarei.databinding.CategoryItemBinding
import br.com.zamfir.comprarei.data.model.entity.Cart
import br.com.zamfir.comprarei.data.model.entity.Category

class CategoryAdapter : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(differCallback) {

    val differ = AsyncListDiffer(this, differCallback)

    inner class CategoryViewHolder(private val binding : CategoryItemBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(category: Category){
            binding.categoryColor.setColorFilter(category.color, android.graphics.PorterDuff.Mode.MULTIPLY)
            binding.categoryName.text = category.description

            binding.root.setOnClickListener {
                onItemClickListener?.invoke(category)
            }

            binding.deleteButton.setOnClickListener {
                onDeleteClickListener?.invoke(category)
            }
        }
    }

    companion object {
        private val differCallback: DiffUtil.ItemCallback<Category> = object : DiffUtil.ItemCallback<Category>() {
            override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = CategoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return  CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    private var onItemClickListener: ((Category) -> Unit)? = null

    fun setOnItemClickListener(clickListener: (Category) -> Unit) {
        onItemClickListener = clickListener
    }

    private var onDeleteClickListener : ((Category) -> Unit)? = null

    fun setOnDeleteClickListener(deleteClickListener: (Category) -> Unit){
        onDeleteClickListener = deleteClickListener
    }

}