package br.com.iesb.comprarei.view.adapters

import android.text.Layout
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.iesb.comprarei.databinding.BottomSheetItemRvBinding
import kotlinx.coroutines.selects.whileSelect

class BottomSheetAdapter : ListAdapter<String, BottomSheetAdapter.BottomSheetViewHolder>(differCallback) {

    val differ = AsyncListDiffer(this, differCallback)

    inner class BottomSheetViewHolder(private val binding : BottomSheetItemRvBinding) : RecyclerView.ViewHolder(binding.root){

        fun bind(filter : String){
            binding.filterItem.text = filter
            binding.root.setOnClickListener {
                onClickLister?.invoke(filter)
            }
        }

    }

    companion object{
        private val differCallback: DiffUtil.ItemCallback<String> = object : DiffUtil.ItemCallback<String>(){
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return  oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BottomSheetViewHolder {
        val binding = BottomSheetItemRvBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BottomSheetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BottomSheetViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    private var onClickLister : ((String) -> Unit)? = null

    fun setOnClickLister(clickListener : (String)-> Unit){
        onClickLister = clickListener
    }


}