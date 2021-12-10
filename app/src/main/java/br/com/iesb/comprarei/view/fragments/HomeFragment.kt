package br.com.iesb.comprarei.view.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import br.com.iesb.comprarei.R
import br.com.iesb.comprarei.databinding.FragmentHomeBinding
import br.com.iesb.comprarei.model.Cart
import br.com.iesb.comprarei.model.CartRepository
import br.com.iesb.comprarei.util.setVisibility
import br.com.iesb.comprarei.util.toggleVisibility
import br.com.iesb.comprarei.view.adapters.CartsAdapter
import br.com.iesb.comprarei.view.components.SortBottomSheet.Companion.openSortBottomSheetDialog
import br.com.iesb.comprarei.viewmodel.CartViewModel
import br.com.iesb.comprarei.viewmodel.factories.CartViewModelFactory


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding get() = _binding!!
    private lateinit var cartsAdapter: CartsAdapter

    private lateinit var deleteMenu: MenuItem
    private lateinit var sortMenu: MenuItem
    private lateinit var searchMenu: MenuItem
    private var originalList: List<Cart> = listOf()

    private val viewModel: CartViewModel by lazy {
        val viewModelProviderFactory = CartViewModelFactory(CartRepository())
        ViewModelProvider(this, viewModelProviderFactory)[CartViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenuItems()


        setupAdapter()

        cartsAdapter.setOnItemClickListener { cart ->
            goToProduct(cart, view)
        }

        setItemsSelectable()

        sortMenu.setOnMenuItemClickListener {
            sortList()
            return@setOnMenuItemClickListener true
        }

        doSearch()

        binding.closeSelection.setOnClickListener {
            changeSelectState()
        }

        deleteItems()

        binding.addCart.setOnClickListener {
            NewCartFragment().show(parentFragmentManager, "NewCart")
        }

        viewModel.listOfCarts.observe(viewLifecycleOwner) { cartsList ->
            showCartsItems(cartsList)
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
                    it.name.uppercase().contains(query.uppercase()) ||
                            it.data.contains(query)
                }

                cartsAdapter.differ.submitList(searched)
                return false
            }

            override fun onQueryTextChange(nextText: String): Boolean {
                val searched = originalList.filter {
                    it.name.uppercase().contains(nextText.uppercase()) ||
                            it.data.contains(nextText)
                }
                cartsAdapter.differ.submitList(searched)
                return false
            }
        })
    }

    private fun showCartsItems(cartsList: List<Cart>) {
        if (cartsList.isEmpty()) {
            showEmptyMessage(true)
        } else {
            showEmptyMessage(false)
            originalList = cartsList
            cartsAdapter.differ.submitList(cartsList)
        }
    }

    private fun deleteItems() {
        deleteMenu.setOnMenuItemClickListener {
            deleteSelectedItems()
            return@setOnMenuItemClickListener true
        }
    }

    private fun setItemsSelectable() {
        cartsAdapter.setOnLongItemClickListener {
            changeSelectState()
            return@setOnLongItemClickListener true
        }
    }

    private fun changeSelectState() {
        deleteMenu.let { delete ->
            delete.toggleVisibility()
            if (delete.isVisible) {
                binding.addCart.setVisibility(false)
                binding.closeSelection.setVisibility(true)
            } else {
                binding.addCart.setVisibility(true)
                binding.closeSelection.setVisibility(false)
            }
            cartsAdapter.apply {
                selectionMode = !cartsAdapter.selectionMode
                notifyDataSetChanged()
            }

        }
    }

    private fun setupAdapter() {
        cartsAdapter = CartsAdapter()
        binding.cartsRv.adapter = cartsAdapter
    }

    private fun goToProduct(cart: Cart, view: View) {
        val args = Bundle()
        args.putString("cartName", cart.name)
        args.putString("cartId", cart.id)
        arguments = args
        Navigation.findNavController(view)
            .navigate(R.id.action_homeFragment_to_productsFragment, args)
    }

    private fun showEmptyMessage(visibity: Boolean) {
        binding.cartsRv.setVisibility(!visibity)
        binding.emptyMessage.root.setVisibility(visibity)
    }

    private fun sortList() {
        val options = arrayListOf("Name", "Date", "Original")
        openSortBottomSheetDialog(options) { option ->
            cartsAdapter.differ.submitList(viewModel.sortList(option, originalList))
        }
    }

    private fun setupMenuItems() {
        deleteMenu = binding.toolbar.menu.findItem(R.id.delete_menu)
        sortMenu = binding.toolbar.menu.findItem(R.id.sort_menu)
        searchMenu = binding.toolbar.menu.findItem(R.id.search_menu)
        deleteMenu.toggleVisibility()
    }

    private fun deleteSelectedItems() {
        val actualList = cartsAdapter.differ.currentList.toMutableList()
        val deleteQuantity = cartsAdapter.getSelectedItems().size
        if (deleteQuantity != 0) {
            confirmDeletion(deleteQuantity, actualList)
        }
    }

    private fun confirmDeletion(
        deleteQuantity: Int,
        actualList: MutableList<Cart>
    ) {
        AlertDialog.Builder(context)
            .setTitle("Are you sure?")
            .setMessage("Do you really want to delete $deleteQuantity ${if (deleteQuantity == 1) "item" else "items"}?")
            .setIcon(R.drawable.ic_info_24)
            .setPositiveButton("Yes") { _, _ ->
                cartsAdapter.getSelectedItems().forEach { cart ->
                    viewModel.deleteCart(cart)
                    cartsAdapter.notifyItemRemoved(actualList.indexOf(cart))
                    actualList.remove(cart)
                }
                changeSelectState()
                cartsAdapter.differ.submitList(actualList)
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