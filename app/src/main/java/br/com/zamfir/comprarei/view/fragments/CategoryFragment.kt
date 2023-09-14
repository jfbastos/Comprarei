package br.com.zamfir.comprarei.view.fragments

import android.os.Bundle
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
import br.com.zamfir.comprarei.model.entity.Category
import br.com.zamfir.comprarei.util.setVisibility
import br.com.zamfir.comprarei.util.show
import br.com.zamfir.comprarei.viewmodel.CategoryViewModel
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.koin.androidx.viewmodel.ext.android.viewModel

class CategoryFragment : Fragment() {

    private var _binding : FragmentCategoryBinding? = null
    private val binding : FragmentCategoryBinding get() = _binding!!
    private val categoriesList = mutableListOf<Category>()

    private lateinit var searchMenu: MenuItem

    private val viewModel : CategoryViewModel by viewModel()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentCategoryBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenuItems()

        viewModel.getCategories()

        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            showCategories(categories)
        }

        binding.toolbar.setNavigationIcon(R.drawable.md_nav_back)
        binding.toolbar.setNavigationOnClickListener {
            voltarTela()
        }
    }

    private fun voltarTela() {
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
        }
    }

    private fun saveCategory(category: Category){
        viewModel.saveCategory(category)
        categoriesList.add(category)
        //TODO -> Adapter
    }

    private fun updateCategory(category: Category){
        viewModel.updateCategory(category)
        categoriesList[categoriesList.indexOf(category)] = category
        //TODO -> Adapter
    }

    private fun deleteCategory(category: Category){
        viewModel.deleteCategory(category)
        categoriesList.remove(category)
        //TODO -> Adapter
    }



    private fun showEmptyMessage(visibility: Boolean) {
        binding.categoriesRcv.setVisibility(!visibility)
        binding.emptyListPlaceholder.setVisibility(visibility)
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
        binding.searchView.show(requireContext())
        if (binding.searchView.isVisible) {
            binding.searchView.setVisibility(false)
        } else {
            binding.searchView.setVisibility(true)
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                val searched = categoriesList.filter {it.description.uppercase().contains(query.uppercase()) }
                //TODO -> Adapter
                return false
            }

            override fun onQueryTextChange(nextText: String): Boolean {
                val searched = categoriesList.filter {it.description.uppercase().contains(nextText.uppercase()) }
                //TODO -> Adapter
                return false
            }
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
                        binding.searchView.setVisibility(false)
                    }
                    else -> {
                       voltarTela()
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
        super.onDestroy()
        _binding = null
    }
}