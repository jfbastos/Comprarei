package br.com.zamfir.comprarei.view.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.FragmentCategoryBinding
import br.com.zamfir.comprarei.data.model.entity.Category
import br.com.zamfir.comprarei.util.Constants
import br.com.zamfir.comprarei.util.isVisible
import br.com.zamfir.comprarei.util.show
import br.com.zamfir.comprarei.view.adapters.CategoryAdapter
import br.com.zamfir.comprarei.view.dialog.NewCategoryDialog
import br.com.zamfir.comprarei.view.listeners.InfoUpdateListener
import br.com.zamfir.comprarei.viewmodel.CategoryViewModel
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.koin.androidx.viewmodel.ext.android.viewModel

class CategoryFragment : Fragment() {
    private var _binding : FragmentCategoryBinding? = null
    private val binding : FragmentCategoryBinding get() = _binding!!
    private val categoriesList = mutableListOf<Category>()
    private lateinit var categoryAdapter : CategoryAdapter

    private lateinit var searchMenu: MenuItem

    private val viewModel : CategoryViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoryBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenuItems()

        categoryAdapter = CategoryAdapter()
        binding.categoriesRcv.adapter = categoryAdapter

        viewModel.getCategories()

        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            showCategories(categories)
        }

        viewModel.deleteState.observe(viewLifecycleOwner) {
            if(it.canDelete){
                categoryAdapter.notifyItemRemoved(categoriesList.indexOf(it.itemDeleted))
                categoriesList.remove(it.itemDeleted)
                categoryAdapter.differ.submitList(categoriesList)
                InfoUpdateListener.infoUpdateListener.infoUpdated()
            }else{
                if(it.itemDeleted != null && it.itemDeleted is Category){
                    AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.be_careful_title))
                        .setMessage(getString(R.string.carts_with_category))
                        .setPositiveButton(R.string.positive_confirmation){ dialog, _ ->
                            deleteCategory(it.itemDeleted, true)
                            dialog.dismiss()
                        }
                        .setNegativeButton(R.string.negative_confirmation){ dialog, _ ->
                            dialog.dismiss()
                        }.show()
                }
            }
        }

        binding.newCategoryBtn.setOnClickListener {
            NewCategoryDialog(categoriesList){ newCategory, _ ->
                saveCategory(newCategory)
            }.show(parentFragmentManager, Constants.EMPTY_STRING)
        }

        categoryAdapter.setOnItemClickListener {
            NewCategoryDialog(categoriesList,it){editedCategory, isDeletion ->
                if(isDeletion) deleteCategory(editedCategory)
                else updateCategory(editedCategory)
            }.show(parentFragmentManager, Constants.EMPTY_STRING)
        }

        categoryAdapter.setOnDeleteClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_confirmation)
                .setMessage(getString(R.string.message_delete_confirmation) + it.description + getString(R.string.space_interrogation))
                .setPositiveButton(R.string.positive_confirmation){ dialog, _ ->
                    deleteCategory(it)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.negative_confirmation){dialog, _ ->
                    dialog.dismiss()
                }.show()
        }

        binding.toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        binding.toolbar.setNavigationOnClickListener {
            navigateBack()
        }
    }

    private fun navigateBack() {
        this.parentFragmentManager.popBackStack(
            null, FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        findNavController().navigateUp()
    }

    private fun showCategories(categories : List<Category>){
        if(categories.isEmpty()){
            showEmptyMessage(true)
        }else{
            showEmptyMessage(false)
            categoriesList.addAll(categories)
            categoryAdapter.differ.submitList(categoriesList)
        }
        categoryAdapter.notifyDataSetChanged()
    }

    private fun saveCategory(category: Category){
        viewModel.saveCategory(category)
        categoriesList.add(category)
        showEmptyMessage(categoriesList.isEmpty())
        categoryAdapter.differ.submitList(categoriesList)
        categoryAdapter.notifyItemInserted(categoryAdapter.itemCount)
    }

    private fun updateCategory(category: Category){
        viewModel.updateCategory(category)
        val index = categoriesList.indexOf(categoriesList.find { it.id == category.id })
        categoriesList[index] = category
        categoryAdapter.differ.submitList(categoriesList)
        categoryAdapter.notifyItemChanged(categoriesList.indexOf(category))
        InfoUpdateListener.infoUpdateListener.infoUpdated()
    }

    private fun deleteCategory(category: Category, deletionConfirmed : Boolean = false){
        viewModel.deleteCategory(category, deletionConfirmed)
    }

    private fun showEmptyMessage(visibility: Boolean) {
        binding.categoriesRcv.isVisible(!visibility)
        binding.emptyListPlaceholder.isVisible(visibility)
    }

    private fun setupMenuItems() {
        binding.toolbar.menu.findItem(R.id.delete_menu).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            this.isVisible = false
        }
        binding.toolbar.menu.findItem(R.id.sort_menu).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            this.isVisible = false
        }
        binding.toolbar.menu.findItem(R.id.select_all).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            this.isVisible = false
        }

        searchMenu = binding.toolbar.menu.findItem(R.id.search_menu)

        searchMenu.setOnMenuItemClickListener {
            doSearch()
            true
        }
    }

    private fun doSearch() : Boolean{
        if (binding.searchView.isVisible) {
            binding.searchView.isVisible(false)
        } else {
            binding.searchView.hint = "Search categories"
            binding.searchView.isVisible(true)
            binding.searchView.requestFocus()
        }

        binding.searchText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searched = categoriesList.filter { it.description.uppercase().contains(s.toString().uppercase()) }
                categoryAdapter.differ.submitList(searched)
            }

            override fun afterTextChanged(s: Editable?) {}

        })
        return true
    }

    private fun cancelActionsOnBackPressed() {
        if (view == null) return

        requireView().isFocusableInTouchMode = true
        requireView().requestFocus()
        requireView().setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                when {
                    binding.searchView.isVisible -> {
                        binding.searchView.isVisible(false)
                    }
                    else -> {
                       navigateBack()
                    }
                }
                true
            } else false
        }

        KeyboardVisibilityEvent.setEventListener(
            requireActivity(),
            viewLifecycleOwner
        ) { isOpen ->
            if (binding.searchView.isVisible && !isOpen) {
                binding.searchView.clearFocus()
                requireView().requestFocus()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        cancelActionsOnBackPressed()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}