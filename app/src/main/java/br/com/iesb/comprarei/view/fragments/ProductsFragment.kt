package br.com.iesb.comprarei.view.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.iesb.comprarei.R
import br.com.iesb.comprarei.databinding.FragmentProductsBinding
import br.com.iesb.comprarei.model.Product
import br.com.iesb.comprarei.model.ProductRepository
import br.com.iesb.comprarei.util.setVisibility
import br.com.iesb.comprarei.util.toggleVisibility
import br.com.iesb.comprarei.view.adapters.ProductsAdapter
import br.com.iesb.comprarei.view.components.SortBottomSheet.Companion.openSortBottomSheetDialog
import br.com.iesb.comprarei.view.fragments.NewProductFragment.Companion.openEditProductBottomSheet
import br.com.iesb.comprarei.view.fragments.NewProductFragment.Companion.openNewProductBottomSheet
import br.com.iesb.comprarei.viewmodel.ProductViewModel
import br.com.iesb.comprarei.viewmodel.factories.ProductViewModelFactory


class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding: FragmentProductsBinding get() = _binding!!


    private lateinit var deleteMenu: MenuItem
    private lateinit var sortMenu: MenuItem
    private lateinit var searchMenu: MenuItem
    private var originalList: List<Product> = listOf()
    private lateinit var productsAdapter: ProductsAdapter


    private val viewModel: ProductViewModel by lazy {
        val viewModelProviderFactory = ProductViewModelFactory(ProductRepository())
        ViewModelProvider(this, viewModelProviderFactory)[ProductViewModel::class.java]
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cartName = arguments?.getString("cartName")
        val cartId = arguments?.getString("cartId") ?: ""
        binding.toolbar.title = cartName

        setupMenuItems()

        setupAdapter()

        setItemsSelectable()

        deleteItems()

        doSearch()

        viewModel.getProducts(cartId)

        productsAdapter.setOnItemClickListener { product, position ->
            openEditProductBottomSheet(product) { productEdited ->
                viewModel.updateProduct(productEdited)
                productsAdapter.notifyItemChanged(position)
            }
        }

        viewModel.productsLiveData.observe(viewLifecycleOwner) { products ->
            showProductsList(products)
        }

        sortMenu.setOnMenuItemClickListener {
            sortList()
            return@setOnMenuItemClickListener true
        }

        binding.newProduct.setOnClickListener {
            openNewProductBottomSheet(cartId) { newProduct ->
                viewModel.saveProduct(newProduct)
                viewModel.getProducts(cartId)
            }
        }

        binding.closeSelection.setOnClickListener {
            changeSelectState()
        }
    }

    private fun doSearch() {
        searchMenu.setOnMenuItemClickListener {
            binding.searchView.setQuery("", false)
            if (binding.searchView.isVisible) {
                binding.searchView.setVisibility(false)
            } else {
                binding.searchView.setVisibility(true)
            }
            return@setOnMenuItemClickListener true
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {

                val searched = originalList.filter {
                    it.name.uppercase().contains(query.uppercase())
                }

                productsAdapter.differ.submitList(searched)
                return false
            }

            override fun onQueryTextChange(nextText: String): Boolean {
                val searched = originalList.filter {
                    it.name.uppercase().contains(nextText.uppercase())
                }
                productsAdapter.differ.submitList(searched)
                return false
            }
        })
    }

    private fun showProductsList(productList: List<Product>) {
        if (productList.isEmpty()) {
            showEmptyMessage(true)
        } else {
            showEmptyMessage(false)
            originalList = productList
            productsAdapter.differ.submitList(productList)
        }
    }

    private fun deleteItems() {
        deleteMenu?.setOnMenuItemClickListener { menu ->
            deleteSelectedItems()
            return@setOnMenuItemClickListener true
        }
    }

    private fun setItemsSelectable() {
        productsAdapter.setOnLongItemClickListener { product ->
            changeSelectState()
            return@setOnLongItemClickListener true
        }
    }

    private fun changeSelectState() {
        deleteMenu?.let { delete ->
            delete.toggleVisibility()
            if (delete.isVisible) {
                binding.newProduct.setVisibility(false)
                binding.closeSelection.setVisibility(true)
            } else {
                binding.newProduct.setVisibility(true)
                binding.closeSelection.setVisibility(false)
            }
            productsAdapter.selectionMode = !productsAdapter.selectionMode
            productsAdapter.notifyDataSetChanged()
        }
    }

    private fun setupAdapter() {
        productsAdapter = ProductsAdapter()
        binding.productsRv.adapter = productsAdapter
    }

//    private fun goToProduct(cart: P, view: View) {
//    }

    private fun showEmptyMessage(visibity: Boolean) {
        binding.productsRv.setVisibility(!visibity)
    }

    private fun sortList() {
        val options = arrayListOf("Name", "Price", "Total", "Original")
        openSortBottomSheetDialog(options) { option ->
            productsAdapter.differ.submitList(viewModel.sortList(option, originalList))
        }
    }

    private fun setupMenuItems() {
        deleteMenu = binding.toolbar.menu.findItem(R.id.delete_menu)
        sortMenu = binding.toolbar.menu.findItem(R.id.sort_menu)
        searchMenu = binding.toolbar.menu.findItem(R.id.search_menu)
        deleteMenu?.toggleVisibility()
    }

    private fun deleteSelectedItems() {
        val actualList = productsAdapter.differ.currentList.toMutableList()
        val deleteQuantity = productsAdapter.getSelectedItems().size
        if (deleteQuantity != 0) {
            confirmDeletion(deleteQuantity, actualList)
        }
    }

    private fun confirmDeletion(
        deleteQuantity: Int,
        actualList: MutableList<Product>
    ) {
        AlertDialog.Builder(context)
            .setTitle("Are you sure?")
            .setMessage("Do you really want to delete $deleteQuantity ${if (deleteQuantity == 1) "item" else "items"}?")
            .setIcon(R.drawable.ic_info_24)
            .setPositiveButton("Yes") { _, _ ->
                productsAdapter.getSelectedItems().forEach { product ->
                    viewModel.deleteProduct(product)
                    actualList.remove(product)
                }
                changeSelectState()
                productsAdapter.differ.submitList(actualList)
            }
            .setNegativeButton("Cancel") { _, _ ->
                changeSelectState()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}