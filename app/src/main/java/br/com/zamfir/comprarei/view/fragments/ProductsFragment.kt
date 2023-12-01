package br.com.zamfir.comprarei.view.fragments

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.FragmentProductsBinding
import br.com.zamfir.comprarei.model.entity.Product
import br.com.zamfir.comprarei.util.Constants
import br.com.zamfir.comprarei.util.FormatFrom
import br.com.zamfir.comprarei.util.Share
import br.com.zamfir.comprarei.util.setVisibility
import br.com.zamfir.comprarei.view.adapters.ProductsAdapter
import br.com.zamfir.comprarei.view.components.SortBottomSheet.Companion.openSortBottomSheetDialog
import br.com.zamfir.comprarei.view.fragments.NewProductFragment.Companion.openEditProductBottomSheet
import br.com.zamfir.comprarei.view.fragments.NewProductFragment.Companion.openNewProductBottomSheet
import br.com.zamfir.comprarei.view.interfaces.BaseFragment
import br.com.zamfir.comprarei.viewmodel.CartViewModel
import br.com.zamfir.comprarei.viewmodel.ProductViewModel
import com.kevincodes.recyclerview.ItemDecorator
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Collections

class ProductsFragment : Fragment(), BaseFragment {

    private var _binding: FragmentProductsBinding? = null
    private val binding: FragmentProductsBinding get() = _binding!!

    private lateinit var deleteManyMenu: MenuItem
    private lateinit var sortMenu: MenuItem
    private lateinit var searchMenu: MenuItem
    private lateinit var shareMenu: MenuItem
    private lateinit var selectAllMenu : MenuItem
    private lateinit var cancelActionMenu : MenuItem

    private var originalList: MutableList<Product> = mutableListOf()
    private lateinit var productsAdapter: ProductsAdapter
    private var selectionMode = false
    private var lastAction = -1
    private var cartId = -1
    private var cartName = ""

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

        cartName = arguments?.getString(Constants.CART_NAME_KEY) ?: ""
        cartId = arguments?.getLong(Constants.CART_ID_KEY)?.toInt() ?: -1
        binding.toolbar.title = cartName

        setupMenuItems()

        setupAdapter()

        configItemTouchHelper()

        configDeleteListeners()

        navigationHandler()

        viewModelProduct.getProducts(cartId)

        viewModelProduct.products.observe(viewLifecycleOwner) { products ->
            showProductsList(products)
        }

        viewModelProduct.saveState.observe(viewLifecycleOwner){saveState ->
            originalList.find { it == (saveState.savedItem as? Product)}?.apply {
                id = saveState.savedId.toInt()
            }
        }

        productsAdapter.setOnItemClickListener { product, _ ->
            if(selectionMode){
                setItemSelected(product)
            }else{
                openEditProductBottomSheet(product) { productEdited ->
                    updateProduct(productEdited)
                    updateSummary()
                }
            }
        }

        binding.newProduct.setOnClickListener {
            openNewProductBottomSheet(cartId, productsAdapter.itemCount) { newProduct ->
                saveNewProduct(newProduct)
                updateSummary()
            }
        }

    }

    private fun showProductsList(productList: List<Product>) {
        if (productList.isEmpty()) {
            showEmptyMessage(true)
        } else {
            showEmptyMessage(false)
            shareMenu.isVisible = true
            originalList.addAll(productList)
            productsAdapter.differ.submitList(originalList)
        }
        updateSummary()
        productsAdapter.notifyDataSetChanged()
    }

    //region CRUD
    private fun updateProduct(productEdited: Product) {
        viewModelProduct.updateProduct(productEdited)
        originalList[originalList.indexOf(productEdited)] = productEdited
        productsAdapter.differ.submitList(originalList)
        productsAdapter.notifyItemChanged(productsAdapter.differ.currentList.indexOf(productEdited))
    }

    private fun saveNewProduct(newProduct: Product) {
        viewModelProduct.saveProduct(newProduct)
        originalList.add(newProduct)
        showEmptyMessage(originalList.isEmpty())
        productsAdapter.differ.submitList(originalList)
        productsAdapter.notifyItemInserted(productsAdapter.differ.currentList.size)

    }

    private fun deleteProducts(list : List<Product>){
        viewModelProduct.deleteProduct(list.map { it.id })
        originalList.removeAll(list)
        productsAdapter.differ.submitList(originalList)
        updateSummary()
        productsAdapter.notifyDataSetChanged()
    }
    //endregion

    //region Delete

    private fun deleteItems(){
        if(selectionMode && productsAdapter.selectedItems.isNotEmpty()) confirmDeletion(productsAdapter.selectedItems)
        else changeSelectState()
    }

    private fun configDeleteListeners(){
        binding.deleteSelection.setOnClickListener {
            deleteItems()
        }

        deleteManyMenu.setOnMenuItemClickListener {
            changeSelectState()
            false
        }
    }

    private fun deleteOneItem(product: Product) {
        confirmDeletion(listOf(product))
    }

    private fun confirmDeletion(productsToDelete : List<Product>) {
        if(productsToDelete.size == 1){
            AlertDialog.Builder(context).setTitle(getString(R.string.title_confirmation))
                .setMessage(getString(R.string.message_delete_confirmation) + productsToDelete.first().name + " ?")
                .setIcon(R.drawable.ic_info_24).setPositiveButton(getString(R.string.positive_confirmation)) { dialog, _ ->
                    deleteProducts(productsToDelete)
                    if(selectionMode) changeSelectState()
                    dialog.dismiss()
                }.setNegativeButton(getString(R.string.negative_confirmation)) { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }else{
            AlertDialog.Builder(context).setTitle(getString(R.string.title_confirmation)).setMessage(
                getString(R.string.message_delete_confirmation) + productsToDelete.size + getString(R.string.multiple_items_confirmation) + " ?")
                .setIcon(R.drawable.ic_info_24)
                .setPositiveButton(getString(R.string.positive_confirmation)) { dialog, _ ->
                    deleteProducts(productsToDelete)
                    if(selectionMode) changeSelectState()
                    dialog.dismiss()
                }.setNegativeButton(getString(R.string.negative_confirmation)) { dialog, _ ->
                    changeSelectState()
                    dialog.dismiss()
                }.show()
        }
    }

    private fun setItemSelected(product: Product) {
        if(productsAdapter.selectedItems.contains(product)) productsAdapter.selectedItems.remove(product) else  productsAdapter.selectedItems.add(product)
        productsAdapter.notifyItemChanged(productsAdapter.differ.currentList.indexOf(product))
    }

    private fun changeSelectState() {
        selectionMode = !selectionMode
        binding.deleteSelection.setVisibility(selectionMode)

        selectAllMenu.isVisible = selectionMode
        cancelActionMenu.isVisible = selectionMode

        deleteManyMenu.isVisible = !selectionMode
        sortMenu.isVisible = !selectionMode

        binding.toolbar.title = if(selectionMode) getString(R.string.select_items) else getString(R.string.app_name)

        if(!selectionMode) {
            productsAdapter.selectedItems.clear()
            productsAdapter.notifyDataSetChanged()
        }
    }

    //endregion

    private fun navigationHandler() {
        binding.toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        binding.toolbar.setNavigationOnClickListener {
            if (KeyboardVisibilityEvent.isKeyboardVisible(requireActivity())) {
                (requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(requireView().windowToken, 0)
            }

            findNavController().navigateUp()
        }
    }

    private fun configItemTouchHelper() {
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
                        changeItemDone(product, position)
                    }
                    ItemTouchHelper.LEFT -> deleteOneItem(product)
                }
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE && lastAction == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewModelProduct.updateOrder(originalList)
                    updateSummary()
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

    private fun changeItemDone(product: Product, position: Int) {
        product.done = !product.done
        viewModelProduct.updateDone(product.done, product.id)
        productsAdapter.notifyItemChanged(position)
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
            }else{
                text.append(
                    "* ${if (product.quantity < 10) "0${product.quantity}" else product.quantity.toString()} x ${product.name} ${if(product.brand != "") "- ${product.brand} - " else ""}\n")
            }
        }
        text.append("\nTotal : ${FormatFrom.doubleToMonetary("R$", total)}")
        Share().send(text.toString(), requireContext())
    }

    private fun updateSummary() {
        if(originalList.isEmpty()){
            binding.totalQuantity.text = "0"
            binding.totalCart.text = FormatFrom.doubleToMonetary("R$", 0.0)
            viewModelCart.updateTotal(FormatFrom.doubleToMonetary("R$", 0.0), cartId)
        }else{
            var totalCart = 0.0
            binding.totalQuantity.text = originalList.size.toString()
            originalList.forEach { product ->
                totalCart += (product.price * product.quantity)
            }
            binding.totalCart.text = FormatFrom.doubleToMonetary("R$", totalCart)
            viewModelCart.updateTotal(FormatFrom.doubleToMonetary("R$", totalCart), cartId)
        }
    }

    override fun doSearch(): Boolean {
        if (binding.searchView.isVisible) {
            binding.searchView.setVisibility(false)
        } else {
            binding.searchView.hint = "Search products"
            binding.searchView.setVisibility(true)
            binding.searchView.requestFocus()
        }

        binding.searchText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searched = originalList.filter {
                    it.name.uppercase().contains(s.toString().uppercase())
                }
                productsAdapter.differ.submitList(searched)
            }

            override fun afterTextChanged(s: Editable?) {}

        })
        return true
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
        cancelActionMenu = binding.toolbar.menu.findItem(R.id.cancel_action)
        searchMenu = binding.toolbar.menu.findItem(R.id.search_menu)
        sortMenu = binding.toolbar.menu.findItem(R.id.sort_menu)
        selectAllMenu = binding.toolbar.menu.findItem(R.id.select_all)
        deleteManyMenu = binding.toolbar.menu.findItem(R.id.delete_menu)

        shareMenu = binding.toolbar.menu.findItem(R.id.share_menu)
        binding.toolbar.menu.findItem(R.id.categories_menu).apply {
            isVisible = false
        }

        searchMenu.setOnMenuItemClickListener {
            doSearch()
        }

        cancelActionMenu.setOnMenuItemClickListener {
            changeSelectState()
            true
        }

        selectAllMenu.setOnMenuItemClickListener {
            if(productsAdapter.selectedItems.size == originalList.size){
                productsAdapter.selectedItems.removeAll(originalList)
                productsAdapter.notifyItemRangeChanged(0, originalList.size)
                selectAllMenu.icon = AppCompatResources.getDrawable(requireContext(), R.drawable.baseline_check_box_24)
            }else{
                productsAdapter.selectedItems.addAll(originalList)
                productsAdapter.notifyItemRangeChanged(0, originalList.size)
                selectAllMenu.icon = AppCompatResources.getDrawable(requireContext(), R.drawable.outline_check_box_24)
            }

            true
        }

        shareMenu.setOnMenuItemClickListener {
            sharedProduct(cartName)
            return@setOnMenuItemClickListener true
        }

        sortMenu.setOnMenuItemClickListener {
            sortList()
            return@setOnMenuItemClickListener true
        }
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
        if (view == null) return

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
}