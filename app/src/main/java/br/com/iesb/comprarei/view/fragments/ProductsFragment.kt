package br.com.iesb.comprarei.view.fragments

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import br.com.iesb.comprarei.R
import br.com.iesb.comprarei.databinding.FragmentProductsBinding
import br.com.iesb.comprarei.model.Cart
import br.com.iesb.comprarei.model.Product
import br.com.iesb.comprarei.util.*
import br.com.iesb.comprarei.view.adapters.ProductsAdapter
import br.com.iesb.comprarei.view.components.SortBottomSheet.Companion.openSortBottomSheetDialog
import br.com.iesb.comprarei.view.fragments.NewProductFragment.Companion.openEditProductBottomSheet
import br.com.iesb.comprarei.view.fragments.NewProductFragment.Companion.openNewProductBottomSheet
import br.com.iesb.comprarei.view.interfaces.BaseFragment
import br.com.iesb.comprarei.viewmodel.CartViewModel
import br.com.iesb.comprarei.viewmodel.ProductViewModel
import com.kevincodes.recyclerview.ItemDecorator
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class ProductsFragment : Fragment(), BaseFragment {

    private var _binding: FragmentProductsBinding? = null
    private val binding: FragmentProductsBinding get() = _binding!!

    private lateinit var deleteMenu: MenuItem
    private lateinit var sortMenu: MenuItem
    private lateinit var searchMenu: MenuItem
    private lateinit var shareMenu: MenuItem
    private var originalList: List<Product> = listOf()
    private lateinit var productsAdapter: ProductsAdapter
    private var selectionMode = false
    private var lastAction = -1
    private var cartId = -1

    private val viewModelProduct: ProductViewModel by viewModel()

    private val viewModelCart: CartViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProductsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cartName = arguments?.getString("cartName")
        cartId = arguments?.getLong("cartId")?.toInt() ?: -1
        binding.toolbar.title = cartName

        viewModelProduct.cartId = cartId

        setupMenuItems()

        setupAdapter()

        doSearch()

        swipeToRemove()

        binding.closeSelection.setOnClickListener {
            changeSelectState()
        }

        viewModelProduct.products.observe(viewLifecycleOwner) { products ->
            showProductsList(products)
        }

        navigationHandler()

        productsAdapter.setOnItemClickListener { product, _ ->
            if(selectionMode){
                setItemSelected(product)
            }else{
                openEditProductBottomSheet(product) { productEdited ->
                    viewModelProduct.updateProduct(productEdited)
                    productsAdapter.notifyItemChanged(productsAdapter.differ.currentList.indexOf(productEdited))
                }
            }
        }

        shareMenu.setOnMenuItemClickListener {
            sharedProduct(cartName)
            return@setOnMenuItemClickListener true
        }

        sortMenu.setOnMenuItemClickListener {
            sortList()
            return@setOnMenuItemClickListener true
        }

        binding.newProduct.setOnClickListener {
            openNewProductBottomSheet(cartId) { newProduct ->
                viewModelProduct.saveProduct(newProduct)
                productsAdapter.notifyDataSetChanged()
            }
        }

        binding.closeSelection.setOnClickListener {
            changeSelectState()
        }
    }

    private fun navigationHandler() {
        binding.toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        binding.toolbar.setNavigationOnClickListener {
            if (KeyboardVisibilityEvent.isKeyboardVisible(requireActivity())) {
                (requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(requireView().windowToken, 0)
            }

            this.parentFragmentManager.popBackStack(
                null, FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            findNavController().navigateUp()
        }
    }

    private fun swipeToRemove() {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ) = target.adapterPosition != viewHolder.adapterPosition

            override fun onMoved(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, fromPos: Int, target: RecyclerView.ViewHolder, toPos: Int, x: Int, y: Int) {
                super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
                if (originalList.isNotEmpty()) {
                    Collections.swap(originalList, fromPos, toPos)
                    productsAdapter.differ.submitList(originalList)
                    productsAdapter.notifyItemMoved(fromPos, toPos)
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val product = productsAdapter.differ.currentList[position]
                when (direction) {
                    ItemTouchHelper.RIGHT -> {
                        product.done = !product.done
                        viewModelProduct.updateDone(product.done, product.id)
                    }
                    ItemTouchHelper.LEFT -> deleteOneItem(product)
                }
                productsAdapter.notifyItemChanged(position)
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE && lastAction == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewModelProduct.deleteProducts(cartId)
                    viewModelProduct.reorderList(createNewReorderedList())
                }
                lastAction = actionState
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean,
            ) {
                // This is where to start decorating
                ItemDecorator.Builder(c, recyclerView, viewHolder, dX, actionState).set(
                    backgroundColorFromStartToEnd = ContextCompat.getColor(requireContext(), R.color.primary_green),
                    backgroundColorFromEndToStart = ContextCompat.getColor(requireContext(), R.color.delete_red),
                    textFromStartToEnd = "",
                    textFromEndToStart = "",
                    textColorFromStartToEnd = ContextCompat.getColor(requireContext(), R.color.white),
                    textColorFromEndToStart = ContextCompat.getColor(requireContext(), R.color.white),
                    iconTintColorFromStartToEnd = ContextCompat.getColor(requireContext(), R.color.white),
                    iconTintColorFromEndToStart = ContextCompat.getColor(requireContext(), R.color.white),
                    iconResIdFromStartToEnd = if (productsAdapter.differ.currentList[viewHolder.adapterPosition].done) R.drawable.ic_baseline_close_24 else R.drawable.ic_baseline_done_24,
                    iconResIdFromEndToStart = R.drawable.ic_delete_24,typeFaceFromStartToEnd = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                )
                super.onChildDraw(c, recyclerView, viewHolder, dX / 6, dY, actionState, isCurrentlyActive)
            }
        }).apply {
            attachToRecyclerView(binding.productsRv)
        }
    }

    private fun createNewReorderedList() : List<Product>{
        return mutableListOf<Product>().apply {
            productsAdapter.differ.currentList.forEach {
                add(Product(it.name, it.brand, it.price, it.quantity, it.cartId, it.done))
            }
        }
    }

    private fun sharedProduct(cartName: String?) {
        val text = StringBuilder()
        var total = 0.0

        text.append("$cartName \n\n")
        originalList.forEach { product ->
            total += product.price * product.quantity
            if(product.price != 0.0){
                text.append(
                    "* ${product.name} ${if (product.brand != "") "- ${product.brand} - " else "- "}" + FormatFrom.doubleToMonetary("R$", product.price) + " x " + "${if (product.quantity < 10) "0${product.quantity}" else product.quantity.toString()}\n"
                )
                text.append("\nTotal : ${FormatFrom.doubleToMonetary("R$", total)}")
            }else{
                text.append(
                    "* ${if (product.quantity < 10) "0${product.quantity}" else product.quantity.toString()} x ${product.name} ${if(product.brand != "") "- ${product.brand} - " else ""}\n")
            }
        }
        Share().send(text.toString(), requireContext())
    }

    private fun fillSummary(products: List<Product>, id: Int) {
        if(products.isEmpty()){
            binding.totalQuantity.text = "0"
            binding.totalCart.text = FormatFrom.doubleToMonetary("R$", 0.0)
        }
        binding.totalQuantity.text = products.size.toString()
        var totalCart = 0.0
        products.forEach { product ->
            totalCart += (product.price * product.quantity)
        }
        binding.totalCart.text = FormatFrom.doubleToMonetary("R$", totalCart)
        viewModelCart.updateTotal(FormatFrom.doubleToMonetary("R$", totalCart), id)
    }

    override fun doSearch(): Boolean {
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

        return true
    }

    private fun setItemSelected(product: Product) {
        if(productsAdapter.selectedItems.contains(product)) productsAdapter.selectedItems.remove(product) else  productsAdapter.selectedItems.add(product)
        productsAdapter.notifyItemChanged(productsAdapter.differ.currentList.indexOf(product))
    }

    private fun showProductsList(productList: List<Product>) {
        fillSummary(productList, cartId)
        if (productList.isEmpty()) {
            showEmptyMessage(true)
        } else {
            showEmptyMessage(false)
            shareMenu.isVisible = true
            productsAdapter.differ.submitList(productList)
            originalList = productList
        }
    }

    private fun changeSelectState() {
        deleteMenu.let { delete ->
            selectionMode = if (!selectionMode) {
                binding.newProduct.setVisibility(false)
                binding.closeSelection.setVisibility(true)
                binding.toolbar.title = "Selecione os itens..."
                deleteMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                true
            } else {
                binding.newProduct.setVisibility(true)
                binding.closeSelection.setVisibility(false)
                productsAdapter.selectedItems.clear()
                deleteMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                productsAdapter.notifyDataSetChanged()
                binding.toolbar.title = requireContext().resources.getString(R.string.app_name)
                false
            }
        }
    }

    private fun setupAdapter() {
        productsAdapter = ProductsAdapter()
        binding.productsRv.adapter = productsAdapter
    }

    private fun showEmptyMessage(visibility: Boolean) {
        binding.productsRv.setVisibility(!visibility)
        binding.emptyList.root.setVisibility(visibility)
    }

    override fun sortList(): Boolean {
        val options = arrayListOf(
            getString(R.string.product_sort_name), getString(R.string.product_sort_price), getString(
                R.string.product_sort_total
            ), "Original"
        )
        openSortBottomSheetDialog(options) { option ->
            productsAdapter.differ.submitList(viewModelProduct.sortList(option, originalList))
        }

        return true
    }

    private fun setupMenuItems() {
        deleteMenu = binding.toolbar.menu.findItem(R.id.delete_menu)
        sortMenu = binding.toolbar.menu.findItem(R.id.sort_menu)
        searchMenu = binding.toolbar.menu.findItem(R.id.search_menu)
        shareMenu = binding.toolbar.menu.findItem(R.id.share_menu)
        binding.toolbar.menu.findItem(R.id.night_mode).isVisible = false

        deleteMenu.setOnMenuItemClickListener {
            deleteItems()
        }

    }

    private fun deleteSelectedItems() {
        val deleteQuantity = productsAdapter.selectedItems.size
        if (deleteQuantity != 0) {
            confirmDeletion(deleteQuantity)
        }
    }

    private fun deleteItems() : Boolean{
        if(selectionMode && productsAdapter.selectedItems.isNotEmpty()){
            deleteSelectedItems()
        }else{
            changeSelectState()
        }
        return true
    }

    private fun confirmDeletion(deleteQuantity: Int) {
        AlertDialog.Builder(context).setTitle(getString(R.string.title_confirmation)).setMessage(
                getString(R.string.message_confirmation) + deleteQuantity + " " + if (deleteQuantity == 1) getString(
                    R.string.single_item_confirmation
                ) else getString(R.string.multiple_items_confirmation) + "?"
            ).setIcon(R.drawable.ic_info_24).setPositiveButton(getString(R.string.positive_confirmation)) { _, _ ->
                productsAdapter.selectedItems.forEach { product ->
                    viewModelProduct.deleteProduct(product)
                }
                changeSelectState()
            }.setNegativeButton(getString(R.string.negative_confirmation)) { _, _ ->
                changeSelectState()
            }.show()
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
        requireView().setOnKeyListener { _, keyCode, event ->
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
                            null, FragmentManager.POP_BACK_STACK_INCLUSIVE
                        )
                        findNavController().navigateUp()
                    }
                }
                true
            } else false
        }

        KeyboardVisibilityEvent.setEventListener(
            requireActivity(), viewLifecycleOwner
        ) { isOpen ->
            if (binding.searchView.isVisible && !isOpen) {
                binding.searchView.clearFocus()
                requireView().requestFocus()
            }
        }
    }

    fun deleteOneItem(product: Product) {
        AlertDialog.Builder(context).setTitle(getString(R.string.title_confirmation)).setMessage(getString(R.string.message_confirmation) + product.name + "?").setIcon(R.drawable.ic_info_24).setPositiveButton(getString(R.string.positive_confirmation)) { _, _ ->
                viewModelProduct.deleteProduct(product)
            }.setNegativeButton(getString(R.string.negative_confirmation)) { _, _ -> }.show()
    }
}