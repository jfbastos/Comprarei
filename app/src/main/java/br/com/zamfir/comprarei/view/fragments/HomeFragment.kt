package br.com.zamfir.comprarei.view.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.FragmentHomeBinding
import br.com.zamfir.comprarei.model.entity.Cart
import br.com.zamfir.comprarei.model.entity.Category
import br.com.zamfir.comprarei.model.entity.Filter
import br.com.zamfir.comprarei.model.entity.UserInfo
import br.com.zamfir.comprarei.util.Constants
import br.com.zamfir.comprarei.util.isVisible
import br.com.zamfir.comprarei.view.activity.LoginActivity
import br.com.zamfir.comprarei.view.adapters.CartsAdapter
import br.com.zamfir.comprarei.view.components.FilterDialog
import br.com.zamfir.comprarei.view.dialog.NewCategoryDialog
import br.com.zamfir.comprarei.view.interfaces.BaseFragment
import br.com.zamfir.comprarei.view.listeners.InfoUpdateListener
import br.com.zamfir.comprarei.viewmodel.CartViewModel
import br.com.zamfir.comprarei.viewmodel.CategoryViewModel
import br.com.zamfir.comprarei.viewmodel.LoginViewModel
import br.com.zamfir.comprarei.viewmodel.states.CartsState
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.imageview.ShapeableImageView
import com.kevincodes.recyclerview.ItemDecorator
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Collections
import kotlin.math.log

class HomeFragment : Fragment(), BaseFragment {
    private var _binding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding get() = _binding!!
    private lateinit var cartsAdapter: CartsAdapter

    private lateinit var cancelActionMenu: MenuItem
    private lateinit var sortMenu: MenuItem
    private lateinit var searchMenu: MenuItem
    private lateinit var selectAllMenu : MenuItem
    private lateinit var deleteManyMenu : MenuItem
    private var originalList: MutableList<Cart> = mutableListOf()
    private var categoriesList : MutableList<Category> = mutableListOf()
    private var selectionMode = false
    private var fabExapanded = false
    private var lastAction = -1

    private val rotateOpenAnimation: Animation by lazy { AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_open_animation)}
    private val rotateCloseAnimation: Animation by lazy {AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_close_animation)}
    private val fromBottomAnimation: Animation by lazy {AnimationUtils.loadAnimation(requireContext(), R.anim.from_bottom_animation)}
    private val toBottomAnimation: Animation by lazy {AnimationUtils.loadAnimation(requireContext(), R.anim.to_bottom_animation)}

    private val viewModel: CartViewModel by viewModel()
    private val categoryViewModel : CategoryViewModel by viewModel()
    private val loginViewModel : LoginViewModel by viewModel()

    private var selectedFilter : Filter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?,): View {
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

        setupMenuItems()
        setupAdapter()
        setupItemMoveHandler()
        setupObservables()
        setupListeners()

        viewModel.updateAllData()

        binding.drawerLayout.close()

    }

    private fun setupNavigationDrawer(loggedUser : UserInfo?){
        binding.navigationDrawer.getHeaderView(0).apply {
            val userNameView = findViewById<TextView>(R.id.user_name_navigation)
            val userEmailView = findViewById<TextView>(R.id.user_email_navigation)
            val userProfileView = findViewById<ShapeableImageView>(R.id.profile_picture_navigation)

            userNameView.text = loggedUser?.name ?: "Olá"
            userEmailView.text = loggedUser?.email ?: "Bem-Vindo"
            Glide.with(requireContext())
                .load(loggedUser?.profilePicture)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(userProfileView)
        }


        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.open()
        }

        binding.navigationDrawer.setNavigationItemSelectedListener { menuItem ->
            when(menuItem){
                binding.navigationDrawer.menu.findItem(R.id.categories_navigation) -> {
                    findNavController().navigate(R.id.action_homeFragment_to_categoryFragment)
                    menuItem.isChecked = true
                    binding.drawerLayout.close()
                    true
                }
                binding.navigationDrawer.menu.findItem(R.id.configuration_navigation) -> {
                    findNavController().navigate(R.id.action_homeFragment_to_configuracaoFragment)
                    menuItem.isChecked = true
                    binding.drawerLayout.close()
                    true
                }
                else -> true
            }
        }
    }

    private fun setupListeners() {
        cartsAdapter.setOnItemClickListener { cart ->
            if (selectionMode) {
                setItemSelected(cart)
            } else {
                goToProduct(cart)
            }
        }

        binding.newCart.setOnClickListener {
            val lastKnownPosition = originalList.takeIf { it.isNotEmpty() }?.maxOf { it.position }?.plus(1) ?: 0
            NewCartFragment(lastKnownPosition, categoriesList) { cart ->
                saveNewCart(cart)
            }.show(parentFragmentManager, Constants.NEW_CART_KEY)
        }

        binding.deleteSelection.setOnClickListener {
            deleteItems()
        }

        deleteManyMenu.setOnMenuItemClickListener {
            changeSelectState()
            false
        }
    }

    private fun setupObservables() {
        viewModel.cartsState.observe(viewLifecycleOwner) { infoState ->
            showLoading(infoState)
        }

        viewModel.deleteState.observe(viewLifecycleOwner) { deleteState ->
            if (deleteState.error != null) throw deleteState.error
        }

        viewModel.saveState.observe(viewLifecycleOwner) { saveState ->
            originalList.find { it == (saveState.savedItem as? Cart) }?.apply {
                id = saveState.savedId.toInt()
            }
        }

        loginViewModel.userLoggedOffState.observe(viewLifecycleOwner){ successLoggoff ->
            if(successLoggoff){
                requireActivity().startActivity(Intent(requireActivity(), LoginActivity::class.java))
                requireActivity().finish()
            }
        }

        categoryViewModel.idCreatedCategory.observe(viewLifecycleOwner){
            //viewModel.updateAllData()
        }
    }

    private fun showCartsItems(cartsList: List<Cart>?, showTotal: Boolean) {
        if (cartsList.isNullOrEmpty()) {
            showEmptyMessage(true)
        } else {
            showEmptyMessage(false)
            cartsAdapter.isShowTotal = showTotal
            originalList.clear()
            originalList.addAll(cartsList)
            cartsAdapter.differ.submitList(cartsList)
            cartsAdapter.getCartsPosition(cartsList).onEach {
                cartsAdapter.notifyItemInserted(it)
            }
        }
    }

    private fun saveNewCart(newCart : Cart){
        viewModel.saveCart(newCart)
        originalList.add(newCart)
        showEmptyMessage(originalList.isEmpty())
        cartsAdapter.differ.submitList(originalList)
        cartsAdapter.notifyItemInserted(cartsAdapter.itemCount + 1)

    }

    private fun updateCart(cart : Cart){
        try {
            viewModel.updateCart(cart)
            val index = originalList.indexOf( originalList.find { it.id == cart.id })
            originalList[index].apply {
                categoryId = cart.categoryId
                category = cart.category
            }
            cartsAdapter.differ.submitList(originalList)
            cartsAdapter.notifyItemChanged(cartsAdapter.getCartPosition(cart))
        }catch (e : Exception){
            Log.e("DEBUG", "Falha ao atualizar na fragment : ${e.stackTraceToString()}")
        }

    }

    private fun deleteCarts(carts : List<Cart>){
        val cartsPosition = cartsAdapter.getCartsPosition(carts)
        viewModel.deleteCarts(carts.map { it.id })
        originalList.removeAll(carts)
        cartsAdapter.differ.submitList(originalList)

        cartsPosition.forEach {
            cartsAdapter.notifyItemRemoved(it)
        }

        showEmptyMessage(originalList.isEmpty())
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
            putString(Constants.CART_NAME_KEY, cart.name)
            putLong(Constants.CART_ID_KEY, cart.id.toLong())
        }

        if(KeyboardVisibilityEvent.isKeyboardVisible(requireActivity())){
            (requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(requireView().windowToken, 0)
        }

        findNavController().navigate(R.id.action_homeFragment_to_productsFragment, arguments)
    }

    private fun  setupMenuItems() {
        cancelActionMenu = binding.toolbar.menu.findItem(R.id.cancel_action)
        searchMenu = binding.toolbar.menu.findItem(R.id.search_menu)
        sortMenu = binding.toolbar.menu.findItem(R.id.sort_menu)
        selectAllMenu = binding.toolbar.menu.findItem(R.id.select_all)
        deleteManyMenu = binding.toolbar.menu.findItem(R.id.delete_menu)


        searchMenu.setOnMenuItemClickListener {
            doSearch()
        }

        sortMenu.setOnMenuItemClickListener {
            sortList()
        }

        cancelActionMenu.setOnMenuItemClickListener {
            changeSelectState()
            true
        }

        selectAllMenu.setOnMenuItemClickListener {
            if (cartsAdapter.selectedItems.size == originalList.size) {
                cartsAdapter.selectedItems.clear()
                cartsAdapter.notifyItemRangeChanged(0, cartsAdapter.itemCount)
                selectAllMenu.icon = AppCompatResources.getDrawable(requireContext(), R.drawable.outline_check_box_24)
            } else {
                cartsAdapter.selectedItems.addAll(originalList)
                cartsAdapter.notifyItemRangeChanged(0, cartsAdapter.itemCount)
                selectAllMenu.icon = AppCompatResources.getDrawable(requireContext(), R.drawable.baseline_check_box_24)
            }
            true
        }
    }

    private fun setupAdapter() {
        cartsAdapter = CartsAdapter()
        binding.cartsRv.adapter = cartsAdapter
    }

    private fun setupItemMoveHandler() {
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
                            NewCartFragment(cart, categoriesList){ updatedCart ->
                                updateCart(updatedCart)
                            }.show(parentFragmentManager, Constants.NEW_CART_KEY)
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
    private fun deleteItems(){
        if(selectionMode && cartsAdapter.selectedItems.isNotEmpty()) confirmDeletion(cartsAdapter.selectedItems)
        else changeSelectState()
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
                    if(selectionMode) changeSelectState()
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
                    if(selectionMode) changeSelectState()
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
        selectionMode = !selectionMode
        binding.newCart.isVisible(!selectionMode)
        binding.deleteSelection.isVisible(selectionMode)

        selectAllMenu.isVisible = selectionMode
        cancelActionMenu.isVisible = selectionMode

        deleteManyMenu.isVisible = !selectionMode
        sortMenu.isVisible = !selectionMode

        binding.toolbar.title = if(selectionMode) getString(R.string.select_items) else getString(R.string.app_name)

        if(!selectionMode) {
            cartsAdapter.selectedItems.clear()
            cartsAdapter.notifyDataSetChanged()
        }
    }

    //endregion

    override fun doSearch() : Boolean{
        if (binding.searchView.isVisible) {
            binding.searchView.isVisible(false)
        } else {
            binding.searchView.hint = getString(R.string.search_carts)
            binding.searchView.isVisible(true)
            binding.searchView.requestFocus()
        }

        binding.searchText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searched = originalList.filter {
                    it.name.uppercase().contains(s.toString().uppercase()) ||
                            it.data.contains(s.toString().uppercase())
                }
                cartsAdapter.differ.submitList(searched)
            }

            override fun afterTextChanged(s: Editable?) {}

        })
        return true
    }

    private fun showEmptyMessage(visibility: Boolean) {
        binding.cartsRv.isVisible(!visibility)
        binding.emptyMessagePlaceholder.root.isVisible(visibility)
    }

    private fun showLoading(infoState: CartsState){
        binding.cartsRv.isVisible(!infoState.loading)
        binding.newCart.isVisible(!infoState.loading)
        binding.loading.isVisible(infoState.loading)

        if(!infoState.loading){
            showCartsItems(infoState.carts, infoState.isShowTotal)
            setupNavigationDrawer(infoState.loggedUser)
            categoriesList.clear()
            categoriesList.addAll(infoState.categories)
        }
    }

    @androidx.annotation.OptIn(com.google.android.material.badge.ExperimentalBadgeUtils::class)
    override fun sortList() : Boolean{
        FilterDialog(selectedFilter ?: Filter(), categoriesList){ filter, isClearFilter ->
            if(isClearFilter){
                cartsAdapter.differ.submitList(originalList)
                cartsAdapter.notifyDataSetChanged()
                sortMenu.icon = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_filter_list_24)
            }else{
                if(filter.sortOption.isNullOrBlank() && filter.byCategory == null && filter.byDate == null && filter.byValue?.valueMin == 0.0 && filter.byValue?.valueMax == 0.0){
                    cartsAdapter.differ.submitList(originalList)
                    cartsAdapter.notifyDataSetChanged()
                    sortMenu.icon = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_filter_list_24)
                }else{
                    var tempList = mutableListOf<Cart>()
                    selectedFilter = filter

                    tempList = viewModel.filter(filter, originalList).toMutableList()

                    cartsAdapter.differ.submitList(tempList)
                    cartsAdapter.notifyDataSetChanged()
                    sortMenu.icon = AppCompatResources.getDrawable(requireContext(), R.drawable.icon_filter_bedge)
                }
            }

        }.show(parentFragmentManager, "")

        return true
    }

    private fun cancelActionsOnBackPressed() {
        if (view == null) return

        requireView().isFocusableInTouchMode = true
        requireView().requestFocus()
        requireView().setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                when {
                    selectionMode -> { changeSelectState() }
                    binding.searchView.isVisible -> {
                        binding.searchView.isVisible(false)
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

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
