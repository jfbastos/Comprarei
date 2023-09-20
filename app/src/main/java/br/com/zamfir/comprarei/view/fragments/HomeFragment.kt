package br.com.zamfir.comprarei.view.fragments

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.FragmentHomeBinding
import br.com.zamfir.comprarei.model.entity.Cart
import br.com.zamfir.comprarei.model.entity.Category
import br.com.zamfir.comprarei.util.Constants
import br.com.zamfir.comprarei.util.setVisibility
import br.com.zamfir.comprarei.util.show
import br.com.zamfir.comprarei.view.adapters.CartsAdapter
import br.com.zamfir.comprarei.view.components.SortBottomSheet.Companion.openSortBottomSheetDialog
import br.com.zamfir.comprarei.view.interfaces.BaseFragment
import br.com.zamfir.comprarei.viewmodel.CartViewModel
import br.com.zamfir.comprarei.view.listeners.InfoUpdateListener
import com.kevincodes.recyclerview.ItemDecorator
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Collections


class HomeFragment : Fragment(), BaseFragment {
    private var _binding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding get() = _binding!!
    private lateinit var cartsAdapter: CartsAdapter

    private lateinit var deleteMenu: MenuItem
    private lateinit var sortMenu: MenuItem
    private lateinit var searchMenu: MenuItem
    private lateinit var selectAllMenu : MenuItem
    private var originalList: MutableList<Cart> = mutableListOf()
    private var categoriesList : MutableList<Category> = mutableListOf()
    private var selectionMode = false
    private var lastAction = -1

    private val viewModel: CartViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater)
        InfoUpdateListener.setOnListener(object : InfoUpdateListener {
            override fun infoUpdated() {
                viewModel.updateAllData()
            }
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
           binding.drawerLayout.open()
        }

        setupMenuItems()

        setupAdapter()

        handleItemMove()

        viewModel.updateAllData()

        viewModel.cartsState.observe(viewLifecycleOwner){
            showLoading(it.loading)
        }

        viewModel.deleteState.observe(viewLifecycleOwner){
            if(it.error != null) throw it.error
        }

        viewModel.saveState.observe(viewLifecycleOwner){saveState ->
            originalList.find { it == (saveState.savedItem as? Cart)}?.apply {
                id = saveState.savedId.toInt()
            }
        }

        viewModel.categories.observe(viewLifecycleOwner){
            categoriesList.clear()
            categoriesList.addAll(it)
        }

        viewModel.carts.observe(viewLifecycleOwner) { carts ->
            showCartsItems(carts)
        }

        cartsAdapter.setOnItemClickListener { cart ->
            if(selectionMode){
               setItemSelected(cart)
            }else{
                goToProduct(cart)
            }
        }

        binding.navigationDrawer.setNavigationItemSelectedListener {
            when(it.itemId){
                binding.navigationDrawer.menu.findItem(R.id.categories).itemId -> {
                    it.isChecked = true
                    binding.drawerLayout.close()
                    findNavController().navigate(R.id.action_homeFragment_to_categoryFragment)
                }
            }

            true
        }

        configDeleteListeners()

        binding.addCart.setOnClickListener {
            NewCartFragment(originalList.takeIf { it.isNotEmpty() }?.maxOf { it.position }?.plus(1) ?: 0, categoriesList).show(parentFragmentManager, Constants.NEW_CART_KEY)
        }

        setFragmentResultListener(Constants.NEW_CART_KEY) { requestKey, bundle ->
            val result = bundle.getSerializable(Constants.CART_BUNDLE_KEY) as Cart
            if(cartsAdapter.differ.currentList.contains(result)){
                updateCart(result)
            }else{
                saveNewCart(result)
            }
        }
    }

    private fun showCartsItems(cartsList: List<Cart>) {
        if (cartsList.isEmpty()) {
            showEmptyMessage(true)
        } else {
            showEmptyMessage(false)
            originalList.clear()
            originalList.addAll(cartsList)
            cartsAdapter.differ.submitList(cartsList)
            cartsAdapter.notifyDataSetChanged()
        }
    }

    private fun saveNewCart(newCart : Cart){
        viewModel.saveCart(newCart)
        originalList.add(newCart)
        showEmptyMessage(originalList.isEmpty())
        cartsAdapter.differ.submitList(originalList)
        cartsAdapter.notifyItemInserted(cartsAdapter.itemCount)

    }

    private fun updateCart(cart : Cart){
        viewModel.updateCart(cart)
        val index = originalList.indexOf(originalList.find { it.id == cart.id })
        originalList[index].apply {
            categoryId = cart.categoryId
            category = cart.category
        }
        cartsAdapter.differ.submitList(originalList)
        cartsAdapter.notifyItemChanged(cartsAdapter.differ.currentList.indexOf(cart))
    }

    private fun deleteCarts(carts : List<Cart>){
        viewModel.deleteCarts(carts.map { it.id })
        originalList.removeAll(carts)
        cartsAdapter.differ.submitList(originalList)
        cartsAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        cancelActionsOnBackPressed()
    }

    private fun goToProduct(cart: Cart) {
        arguments = Bundle().apply {
            putString("cartName", cart.name)
            putLong("cartId", cart.id.toLong())
        }

        if(KeyboardVisibilityEvent.isKeyboardVisible(requireActivity())){
            (requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(requireView().windowToken, 0)
        }

        findNavController().navigate(R.id.action_homeFragment_to_productsFragment, arguments)
    }

    private fun setupMenuItems() {
        deleteMenu = binding.toolbar.menu.findItem(R.id.delete_menu)
        searchMenu = binding.toolbar.menu.findItem(R.id.search_menu)
        sortMenu = binding.toolbar.menu.findItem(R.id.sort_menu)
        selectAllMenu = binding.toolbar.menu.findItem(R.id.select_all)

        searchMenu.setOnMenuItemClickListener {
            doSearch()
        }

        sortMenu.setOnMenuItemClickListener {
            sortList()
        }

        selectAllMenu.setOnMenuItemClickListener {
            if (cartsAdapter.selectedItems.size == originalList.size) {
                cartsAdapter.selectedItems.clear()
                cartsAdapter.notifyItemRangeChanged(0, cartsAdapter.itemCount)
                selectAllMenu.icon = AppCompatResources.getDrawable(requireContext(), R.drawable.baseline_check_box_24)
            } else {
                cartsAdapter.selectedItems.addAll(originalList)
                cartsAdapter.notifyItemRangeChanged(0, cartsAdapter.itemCount)
                selectAllMenu.icon = AppCompatResources.getDrawable(requireContext(), R.drawable.outline_check_box_24)
            }
            true
        }

    }

    private fun setupAdapter() {
        cartsAdapter = CartsAdapter()
        binding.cartsRv.adapter = cartsAdapter
    }

    private fun handleItemMove() {
        ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder ) : Boolean {
                    return target.adapterPosition != viewHolder.adapterPosition
                }

                override fun onMoved(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, fromPos: Int, target: RecyclerView.ViewHolder, toPos: Int, x: Int, y: Int) {
                    super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
                    if(originalList.isNotEmpty()){
                        Collections.swap(originalList, fromPos, toPos)
                        cartsAdapter.notifyItemMoved(fromPos, toPos)
                    }
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val cart = cartsAdapter.differ.currentList[position]
                    when(direction){
                        ItemTouchHelper.RIGHT -> {
                            NewCartFragment(cart, categoriesList).show(parentFragmentManager, Constants.NEW_CART_KEY)
                        }
                        ItemTouchHelper.LEFT -> {
                            deleteOneItem(cart)
                        }
                    }
                    cartsAdapter.notifyItemChanged(position)
                }

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)

                    if(actionState == ItemTouchHelper.ACTION_STATE_IDLE && lastAction == ItemTouchHelper.ACTION_STATE_DRAG){
                        viewModel.updateOrder(originalList)
                    }
                    lastAction = actionState
                }

                override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                    super.onChildDraw(c, recyclerView, viewHolder,dX / 4, dY, actionState, isCurrentlyActive)
                    ItemDecorator.Builder(c, recyclerView, viewHolder, dX, actionState).set(
                            backgroundColorFromStartToEnd = ContextCompat.getColor(requireContext(),R.color.secondary_green),
                            backgroundColorFromEndToStart = ContextCompat.getColor(requireContext(),R.color.delete_red),
                            textFromStartToEnd = getString(R.string.swipe_to_edit) ,
                            textFromEndToStart = getString(R.string.swipe_delete_msg),
                            textColorFromStartToEnd = ContextCompat.getColor(requireContext(),R.color.white),
                            textColorFromEndToStart = ContextCompat.getColor(requireContext(),R.color.white),
                            iconTintColorFromStartToEnd = ContextCompat.getColor(requireContext(),R.color.white),
                            iconTintColorFromEndToStart = ContextCompat.getColor(requireContext(),R.color.white),
                            iconResIdFromStartToEnd = R.drawable.ic_baseline_edit_24,
                            iconResIdFromEndToStart = R.drawable.ic_delete_24)
                }
        }).apply{attachToRecyclerView(binding.cartsRv)}
    }

    //region Deletion

    private fun configDeleteListeners(){
        binding.closeSelection.setOnClickListener {
            changeSelectState()
        }

        deleteMenu.setOnMenuItemClickListener {
            deleteItems()
        }
    }

    private fun deleteItems() : Boolean{
        if(selectionMode && cartsAdapter.selectedItems.isNotEmpty()) confirmDeletion(cartsAdapter.selectedItems)
        else changeSelectState()
        return true
    }

    fun deleteOneItem(cart: Cart) {
        confirmDeletion(listOf(cart))
    }

    private fun confirmDeletion(cartsToDelete : List<Cart>) {
        if(cartsToDelete.size == 1){
            AlertDialog.Builder(context).setTitle(getString(R.string.title_confirmation))
                .setMessage(getString(R.string.message_delete_confirmation) + cartsToDelete.first().name + " ?")
                .setIcon(R.drawable.ic_info_24).setPositiveButton(getString(R.string.positive_confirmation)) { dialog, _ ->
                    deleteCarts(cartsToDelete)
                    dialog.dismiss()
                }.setNegativeButton(getString(R.string.negative_confirmation)) { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }else{
            AlertDialog.Builder(context).setTitle(getString(R.string.title_confirmation)).setMessage(
                getString(R.string.message_delete_confirmation) + cartsToDelete.size + getString(R.string.multiple_items_confirmation) + " ?")
                .setIcon(R.drawable.ic_info_24)
                .setPositiveButton(getString(R.string.positive_confirmation)) { dialog, _ ->
                    deleteCarts(cartsToDelete)
                    changeSelectState()
                    dialog.dismiss()
                }.setNegativeButton(getString(R.string.negative_confirmation)) { dialog, _ ->
                    changeSelectState()
                    dialog.dismiss()
                }.show()
        }
    }

    private fun setItemSelected(cart: Cart) {
        if(cartsAdapter.selectedItems.contains(cart)) cartsAdapter.selectedItems.remove(cart) else  cartsAdapter.selectedItems.add(cart)
        cartsAdapter.notifyItemChanged(cartsAdapter.differ.currentList.indexOf(cart))
    }

    private fun changeSelectState() {
        deleteMenu.let {
            selectionMode = if (!selectionMode) {
                binding.addCart.setVisibility(false)
                binding.closeSelection.setVisibility(true)
                binding.toolbar.title = getString(R.string.select_items)
                deleteMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                selectAllMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                selectAllMenu.isVisible = true

                sortMenu.isVisible = false
                true
            } else {
                binding.addCart.setVisibility(true)
                binding.closeSelection.setVisibility(false)
                deleteMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                selectAllMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                selectAllMenu.isVisible = false

                sortMenu.isVisible = true
                cartsAdapter.selectedItems.clear()
                cartsAdapter.notifyDataSetChanged()
                binding.toolbar.title = requireContext().resources.getString(R.string.app_name)
                false
            }
        }
    }

    //endregion

    override fun doSearch() : Boolean{
        binding.searchView.show(requireContext())
        if (binding.searchView.isVisible) {
            binding.searchView.setVisibility(false)
        } else {
            binding.searchView.setVisibility(true)
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
        return true
    }

    private fun showEmptyMessage(visibility: Boolean) {
        binding.cartsRv.setVisibility(!visibility)
        binding.emptyMessagePlaceholder.root.setVisibility(visibility)
    }

    private fun showLoading(visibility: Boolean){
        binding.cartsRv.setVisibility(!visibility)
        binding.addCart.setVisibility(!visibility)
        binding.loading.setVisibility(visibility)
    }

    override fun sortList() : Boolean{
        val options = arrayListOf(
            getString(R.string.cart_sort_name_option),
            getString(R.string.cart_sort_date_option),
            getString(R.string.valor_total),
            "Original"
        )
        openSortBottomSheetDialog(options) { option ->
            cartsAdapter.differ.submitList(viewModel.sortList(option, originalList))
            cartsAdapter.notifyDataSetChanged()
        }

        return true
    }

    private fun cancelActionsOnBackPressed() {
        if (view == null) return

        requireView().isFocusableInTouchMode = true
        requireView().requestFocus()
        requireView().setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                when {
                    selectionMode -> { changeSelectState() }
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
