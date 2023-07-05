package br.com.iesb.comprarei.view.fragments

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import br.com.iesb.comprarei.R
import br.com.iesb.comprarei.databinding.FragmentHomeBinding
import br.com.iesb.comprarei.model.Cart
import br.com.iesb.comprarei.util.Constants
import br.com.iesb.comprarei.util.setVisibility
import br.com.iesb.comprarei.util.show
import br.com.iesb.comprarei.view.adapters.CartsAdapter
import br.com.iesb.comprarei.view.components.SortBottomSheet.Companion.openSortBottomSheetDialog
import br.com.iesb.comprarei.view.interfaces.BaseFragment
import br.com.iesb.comprarei.viewmodel.CartViewModel
import com.kevincodes.recyclerview.ItemDecorator
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class HomeFragment : Fragment(), BaseFragment {
    private var _binding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding get() = _binding!!
    private lateinit var cartsAdapter: CartsAdapter

    private lateinit var deleteMenu: MenuItem
    private lateinit var sortMenu: MenuItem
    private lateinit var searchMenu: MenuItem
    private var originalList: List<Cart> = listOf()
    private var selectionMode = false
    private var lastAction = -1

    private val viewModel: CartViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenuItems()

        setupAdapter()

        handleItemMove()

        cartsAdapter.setOnItemClickListener { cart ->
            if(selectionMode){
               setItemSelected(cart)
            }else{
                goToProduct(cart)
            }
        }

        configDeleteListeners()

        binding.addCart.setOnClickListener {
            NewCartFragment().show(parentFragmentManager, Constants.NEW_CART_KEY)
        }

        viewModel.carts.observe(viewLifecycleOwner) { carts ->
            showCartsItems(carts)
        }

        setFragmentResultListener(Constants.NEW_CART_KEY) { requestKey, bundle ->
            val result = bundle.getSerializable(Constants.CART_BUNDLE_KEY) as Cart
            if(cartsAdapter.differ.currentList.contains(result)){
                viewModel.updateCart(result)
                cartsAdapter.notifyItemChanged(cartsAdapter.differ.currentList.indexOf(result))
            }else{
                viewModel.saveCart(result)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        cancelActionsOnBackPressed()
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

        searchMenu.setOnMenuItemClickListener {
            doSearch()
        }

        sortMenu.setOnMenuItemClickListener {
            sortList()
        }
    }

    private fun setupAdapter() {
        cartsAdapter = CartsAdapter()
        binding.cartsRv.adapter = cartsAdapter
    }

    //endregion

    //region Swipe

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
                        cartsAdapter.differ.submitList(originalList)
                        cartsAdapter.notifyItemMoved(fromPos, toPos)
                    }
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val cart = cartsAdapter.differ.currentList[position]
                    when(direction){
                        ItemTouchHelper.RIGHT -> {
                            NewCartFragment(cart).show(parentFragmentManager, Constants.NEW_CART_KEY)
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
                        viewModel.deleteAll()
                        viewModel.reorderList(createNewReorderedList())
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

    fun createNewReorderedList() : List<Cart>{
        return mutableListOf<Cart>().apply {
            cartsAdapter.differ.currentList.forEach {
                add(Cart(it.name, it.data, it.total))
            }
        }
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
        if(selectionMode && cartsAdapter.selectedItems.isNotEmpty()){
            deleteSelectedItems()
        }else{
            changeSelectState()
        }
        return true
    }

    private fun deleteSelectedItems() {
        val actualList = cartsAdapter.differ.currentList.toMutableList()
        val deleteQuantity = cartsAdapter.selectedItems.size
        if (deleteQuantity != 0) {
            confirmDeletion(deleteQuantity, actualList)
        }
    }

    private fun confirmDeletion(
        deleteQuantity: Int,
        actualList: MutableList<Cart>,
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
                cartsAdapter.selectedItems.forEach { cart ->
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

    fun deleteOneItem(cart: Cart) {
        AlertDialog.Builder(context)
            .setTitle(getString(R.string.title_confirmation))
            .setMessage(getString(R.string.message_confirmation) + cart.name + "?")
            .setIcon(R.drawable.ic_info_24)
            .setPositiveButton(getString(R.string.positive_confirmation)) { _, _ ->
                viewModel.deleteCart(cart)
            }
            .setNegativeButton(getString(R.string.negative_confirmation)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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
                binding.toolbar.title = "Selecione os itens..."
                deleteMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                true
            } else {
                binding.addCart.setVisibility(true)
                binding.closeSelection.setVisibility(false)
                cartsAdapter.selectedItems.clear()
                deleteMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
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

    override fun sortList() : Boolean{
        val options = arrayListOf(
            getString(R.string.cart_sort_name_option),
            getString(R.string.cart_sort_date_option),
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
