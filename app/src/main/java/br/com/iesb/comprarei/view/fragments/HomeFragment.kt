package br.com.iesb.comprarei.view.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import br.com.iesb.comprarei.R
import br.com.iesb.comprarei.databinding.FragmentHomeBinding
import br.com.iesb.comprarei.model.Cart
import br.com.iesb.comprarei.model.Repository
import br.com.iesb.comprarei.util.setVisibility
import br.com.iesb.comprarei.view.adapters.CartsAdapter
import br.com.iesb.comprarei.view.components.SortBottomSheet.Companion.openBottomSheetDialog
import br.com.iesb.comprarei.viewmodel.MainViewModel
import br.com.iesb.comprarei.viewmodel.ViewModelFactory


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding get() = _binding!!
    private lateinit var cartsAdapter: CartsAdapter

    private var deleteMenu: MenuItem? = null
    private var sortMenu: MenuItem? = null
    private var originalList: List<Cart> = listOf()

    private val viewModel: MainViewModel by lazy {
        val viewModelProviderFactory = ViewModelFactory(Repository())
        ViewModelProvider(this, viewModelProviderFactory)[MainViewModel::class.java]
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

        sortMenu?.setOnMenuItemClickListener {
            sortList()
            return@setOnMenuItemClickListener true
        }

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
        deleteMenu?.setOnMenuItemClickListener { menu ->
            deleteSelectedItems()
            menu.isVisible = false
            return@setOnMenuItemClickListener true
        }
    }

    private fun setItemsSelectable() {
        cartsAdapter.setOnLongItemClickListener { cart ->
            changeSelectState()
            return@setOnLongItemClickListener true
        }
    }

    private fun changeSelectState() {
        deleteMenu?.let { delete ->
            delete.isVisible = !delete.isVisible
            if (delete.isVisible) {
                binding.addCart.setVisibility(false)
                binding.closeSelection.setVisibility(true)
            } else {
                binding.addCart.setVisibility(true)
                binding.closeSelection.setVisibility(false)
            }
            cartsAdapter.selectionMode = !cartsAdapter.selectionMode
            cartsAdapter.notifyDataSetChanged()
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
        openBottomSheetDialog { option ->
            cartsAdapter.differ.submitList(viewModel.sortList(option, originalList))
        }
    }

    private fun setupMenuItems() {
        deleteMenu = binding.toolbar.menu.findItem(R.id.delete_menu)
        sortMenu = binding.toolbar.menu.findItem(R.id.sort_menu)
        deleteMenu?.isVisible = false
    }

    private fun deleteSelectedItems() {
        val actualList = cartsAdapter.differ.currentList.toMutableList()
        val deleteQuantity = cartsAdapter.getSelectedItems().size
        if (deleteQuantity != 0) {
            confirmDeletion(deleteQuantity, actualList)
        }
        changeSelectState()

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
                    actualList.remove(cart)
                }
                cartsAdapter.differ.submitList(actualList)
            }
            .setNegativeButton("Cancel") { _, _ ->

            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}