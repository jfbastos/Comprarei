package br.com.iesb.comprarei.view.fragments

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.com.iesb.comprarei.R
import br.com.iesb.comprarei.databinding.FragmentHomeBinding
import br.com.iesb.comprarei.model.Cart
import br.com.iesb.comprarei.util.setVisibility
import br.com.iesb.comprarei.util.show
import br.com.iesb.comprarei.util.toggleVisibility
import br.com.iesb.comprarei.view.adapters.CartsAdapter
import br.com.iesb.comprarei.view.components.SortBottomSheet.Companion.openSortBottomSheetDialog
import br.com.iesb.comprarei.viewmodel.CartViewModel
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.koin.androidx.viewmodel.ext.android.viewModel


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding get() = _binding!!
    private lateinit var cartsAdapter: CartsAdapter

    private lateinit var deleteMenu: MenuItem
    private lateinit var sortMenu: MenuItem
    private lateinit var searchMenu: MenuItem
    private var originalList: List<Cart> = listOf()

    private val viewModel: CartViewModel by viewModel()

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

        deleteItems()

        setItemsSelectable()

        doSearch()

        cartsAdapter.setOnItemClickListener { cart ->
            goToProduct(cart)
        }

        sortMenu.setOnMenuItemClickListener {
            sortList()
            return@setOnMenuItemClickListener true
        }

        binding.closeSelection.setOnClickListener {
            changeSelectState()
        }

        binding.addCart.setOnClickListener {
            NewCartFragment().show(parentFragmentManager, "NewCart")
        }

        viewModel.listOfCarts.observe(viewLifecycleOwner) { carts ->
            showCartsItems(carts)
        }
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
                cartsAdapter.clearSelectedItems()
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

    private fun goToProduct(cart: Cart) {
        val args = Bundle()

        args.putString("cartName", cart.name)
        args.putString("cartId", cart.id)
        arguments = args

        findNavController().navigate(R.id.action_homeFragment_to_productsFragment, args)
    }

    private fun showEmptyMessage(visibility: Boolean) {
        binding.cartsRv.setVisibility(!visibility)
        binding.emptyMessagePlaceholder.root.setVisibility(visibility)
    }

    private fun sortList() {
        val options = arrayListOf(
            getString(R.string.cart_sort_name_option),
            getString(R.string.cart_sort_date_option),
            "Original"
        )
        openSortBottomSheetDialog(options) { option ->
            cartsAdapter.differ.submitList(viewModel.sortList(option, originalList))
        }
    }

    private fun setupMenuItems() {
        deleteMenu = binding.toolbar.menu.findItem(R.id.delete_menu)
        sortMenu = binding.toolbar.menu.findItem(R.id.sort_menu)
        searchMenu = binding.toolbar.menu.findItem(R.id.search_menu)
        binding.toolbar.menu.findItem(R.id.share_menu).isVisible = false
        deleteMenu.toggleVisibility()
    }


    private fun deleteItems() {
        deleteMenu.setOnMenuItemClickListener {
            deleteSelectedItems()
            return@setOnMenuItemClickListener true
        }
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
            .setTitle(getString(R.string.title_confirmation))
            .setMessage(
                getString(R.string.message_confirmation) + deleteQuantity + " " + (if (deleteQuantity == 1) getString(
                    R.string.single_item_confirmation
                ) else getString(R.string.multiple_items_confirmation)) + " ?"
            )
            .setIcon(R.drawable.ic_info_24)
            .setPositiveButton(getString(R.string.positive_confirmation)) { _, _ ->
                cartsAdapter.getSelectedItems().forEach { cart ->
                    viewModel.deleteCart(cart)
                    cartsAdapter.notifyItemRemoved(actualList.indexOf(cart))
                    actualList.remove(cart)
                }
                changeSelectState()
                cartsAdapter.differ.submitList(actualList)
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
        cancelActionsOnBackPressed()
    }


    private fun cancelActionsOnBackPressed() {
        if (view == null) return

        requireView().isFocusableInTouchMode = true
        requireView().requestFocus()
        requireView().setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                when {
                    cartsAdapter.selectionMode -> {
                        changeSelectState()
                    }
                    binding.searchView.isVisible -> {
                        binding.searchView.setVisibility(false)
                    }
                    else -> {
                        activity?.finish()
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
