package br.com.iesb.comprarei.view.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import br.com.iesb.comprarei.R
import br.com.iesb.comprarei.databinding.FragmentHomeBinding
import br.com.iesb.comprarei.model.Cart
import br.com.iesb.comprarei.model.Repository
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
    private var originalList : List<Cart> = listOf()

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

        deleteMenu = binding.toolbar.menu.findItem(R.id.delete_menu)
        deleteMenu?.isVisible = false

        sortMenu = binding.toolbar.menu.findItem(R.id.sort_menu)

        sortMenu?.setOnMenuItemClickListener {
            openBottomSheetDialog { option ->
                cartsAdapter.differ.submitList(viewModel.sortList(option,originalList))
            }
            return@setOnMenuItemClickListener true

        }


        cartsAdapter = CartsAdapter()
        binding.cartsRv.adapter = cartsAdapter

        cartsAdapter.setOnItemClickListener { cart ->
            val args = Bundle()
            args.putString("cartName", cart.name)
            args.putString("cartId", cart.id)
            arguments = args
            Navigation.findNavController(view)
                .navigate(R.id.action_homeFragment_to_productsFragment, args)
        }

        cartsAdapter.setOnLongItemClickListener { cart ->
            deleteMenu?.let { menu ->
                menu.isVisible = !menu.isVisible
            }
            cartsAdapter.notifyDataSetChanged()
            return@setOnLongItemClickListener true
        }

        deleteMenu?.setOnMenuItemClickListener { menu ->
            deleteSelectedItems()
            menu.isVisible = false
            return@setOnMenuItemClickListener true
        }

        binding.addCart.setOnClickListener {
            NewCartFragment().show(parentFragmentManager, "NewCart")
        }

        viewModel.listOfCarts.observe(viewLifecycleOwner) { cartsList ->
            originalList = cartsList
            cartsAdapter.differ.submitList(cartsList)
        }
    }

    private fun deleteSelectedItems() {
        val actualList = cartsAdapter.differ.currentList.toMutableList()
        val deleteQuantity = cartsAdapter.getSelectedItems().size
        if (deleteQuantity != 0) {
            confirmDeletion(deleteQuantity, actualList)
        }
        cartsAdapter.selectionMode = false
        cartsAdapter.notifyDataSetChanged()
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