package br.com.iesb.comprarei.view.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import br.com.iesb.comprarei.R
import br.com.iesb.comprarei.databinding.FragmentProductsBinding
import br.com.iesb.comprarei.model.Product
import br.com.iesb.comprarei.util.FormatFrom
import br.com.iesb.comprarei.util.setVisibility
import br.com.iesb.comprarei.util.show
import br.com.iesb.comprarei.util.toggleVisibility
import br.com.iesb.comprarei.view.adapters.ProductsAdapter
import br.com.iesb.comprarei.view.components.SortBottomSheet.Companion.openSortBottomSheetDialog
import br.com.iesb.comprarei.view.fragments.NewProductFragment.Companion.openEditProductBottomSheet
import br.com.iesb.comprarei.view.fragments.NewProductFragment.Companion.openNewProductBottomSheet
import br.com.iesb.comprarei.viewmodel.CartViewModel
import br.com.iesb.comprarei.viewmodel.ProductViewModel
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.koin.androidx.viewmodel.ext.android.viewModel


class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding: FragmentProductsBinding get() = _binding!!


    private lateinit var deleteMenu: MenuItem
    private lateinit var sortMenu: MenuItem
    private lateinit var searchMenu: MenuItem
    private lateinit var shareMenu: MenuItem
    private var originalList: List<Product> = listOf()
    private lateinit var productsAdapter: ProductsAdapter


    private val viewModelProduct: ProductViewModel by viewModel()

    private val viewModelCart: CartViewModel by viewModel()

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

        deleteItems(cartId)

        doSearch()

        viewModelProduct.getProducts(cartId)

        productsAdapter.setOnItemClickListener { product, position ->
            openEditProductBottomSheet(product) { productEdited ->
                viewModelProduct.updateProduct(productEdited)
                productsAdapter.notifyItemChanged(position)
            }
        }

        viewModelProduct.productsLiveData.observe(viewLifecycleOwner) { products ->
            productsAdapter.differ.submitList(products)
            fillSummary(products, cartId)
            showProductsList(products)
        }

        shareMenu.setOnMenuItemClickListener {

            share(cartName)

            return@setOnMenuItemClickListener true
        }

        sortMenu.setOnMenuItemClickListener {
            sortList()
            return@setOnMenuItemClickListener true
        }

        binding.newProduct.setOnClickListener {
            openNewProductBottomSheet(cartId) { newProduct ->
                viewModelProduct.saveProduct(newProduct)
                viewModelProduct.getProducts(cartId)
            }
        }

        binding.closeSelection.setOnClickListener {
            changeSelectState()
        }
    }

    private fun share(cartName: String?) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, createShareText(cartName).toString())
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun createShareText(cartName: String?): StringBuilder {
        val text = StringBuilder()
        var total = 0.0

        text.append("$cartName \n\n")
        originalList.forEach { product ->
            total += product.price * product.quantity
            text.append(
                "* ${product.name} - ${
                    FormatFrom.doubleToMonetary(
                        "R$",
                        product.price
                    )
                } x ${if (product.quantity < 10) "0${product.quantity}" else product.quantity.toString()}\n"
            )
        }
        text.append("\nTotal : ${FormatFrom.doubleToMonetary("R$", total)}")
        return text
    }

    private fun fillSummary(products: List<Product>, id : String) {
        var totalCart = 0.0
        binding.totalQuantity.text = products.size.toString()
        products.forEach { product ->
            totalCart += (product.price * product.quantity)
        }
        binding.totalCart.text = FormatFrom.doubleToMonetary("R$", totalCart)
        viewModelCart.updateTotal(FormatFrom.doubleToMonetary("R$", totalCart), id)
    }

    private fun doSearch() {
        searchMenu.setOnMenuItemClickListener {
            binding.searchView.show(requireContext())
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
        }
    }

    private fun deleteItems(id : String) {
        deleteMenu.setOnMenuItemClickListener {
            deleteSelectedItems(id)
            return@setOnMenuItemClickListener true
        }
    }

    private fun setItemsSelectable() {
        productsAdapter.setOnLongItemClickListener {
            changeSelectState()
            return@setOnLongItemClickListener true
        }
    }

    private fun changeSelectState() {
        deleteMenu.let { delete ->
            delete.toggleVisibility()
            if (delete.isVisible) {
                binding.newProduct.setVisibility(false)
                binding.closeSelection.setVisibility(true)
            } else {
                productsAdapter.clearSelectedItems()
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


    private fun showEmptyMessage(visibility: Boolean) {
        binding.productsRv.setVisibility(!visibility)
    }

    private fun sortList() {
        val options = arrayListOf(getString(R.string.product_sort_name), getString(R.string.product_sort_price), getString(
                    R.string.product_sort_total), "Original")
        openSortBottomSheetDialog(options) { option ->
            productsAdapter.differ.submitList(viewModelProduct.sortList(option, originalList))
        }
    }

    private fun setupMenuItems() {
        deleteMenu = binding.toolbar.menu.findItem(R.id.delete_menu)
        sortMenu = binding.toolbar.menu.findItem(R.id.sort_menu)
        searchMenu = binding.toolbar.menu.findItem(R.id.search_menu)
        shareMenu = binding.toolbar.menu.findItem(R.id.share_menu)
        deleteMenu.toggleVisibility()
    }

    private fun deleteSelectedItems(id : String) {
        val actualList = productsAdapter.differ.currentList.toMutableList()
        val deleteQuantity = productsAdapter.getSelectedItems().size
        if (deleteQuantity != 0) {
            confirmDeletion(deleteQuantity, actualList, id)
        }
    }

    private fun confirmDeletion(
        deleteQuantity: Int,
        actualList: MutableList<Product>,
        id : String
    ) {
        AlertDialog.Builder(context)
            .setTitle(getString(R.string.title_confirmation))
            .setMessage(getString(R.string.message_confirmation) + deleteQuantity + " " + if (deleteQuantity == 1) getString(
                R.string.single_item_confirmation) else getString(R.string.multiple_items_confirmation) + "?")
            .setIcon(R.drawable.ic_info_24)
            .setPositiveButton(getString(R.string.positive_confirmation)) { _, _ ->
                productsAdapter.getSelectedItems().forEach { product ->
                    viewModelProduct.deleteProduct(product)
                    productsAdapter.notifyItemRemoved(actualList.indexOf(product))
                    actualList.remove(product)
                }
                changeSelectState()
                productsAdapter.differ.submitList(actualList)
                fillSummary(actualList, id)
            }
            .setNegativeButton(getString(R.string.negative_confirmation)) { _, _ ->
                changeSelectState()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

        cancelActionOnBackPressed()
    }

    private fun cancelActionOnBackPressed() {
        if (view == null) {
            return
        }

        requireView().isFocusableInTouchMode = true
        requireView().requestFocus()
        requireView().setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                when {
                    productsAdapter.selectionMode -> {
                        changeSelectState()
                    }
                    binding.searchView.isVisible -> {
                        binding.searchView.setVisibility(false)
                    }
                    else -> {
                        this.parentFragmentManager.popBackStack(
                            null,
                            FragmentManager.POP_BACK_STACK_INCLUSIVE
                        )
                        findNavController().navigateUp()
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
}