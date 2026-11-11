package br.com.zamfir.comprarei.feature.cart.presentation

import br.com.zamfir.comprarei.feature.cart.presentation.filter.CartFilter
import br.com.zamfir.comprarei.feature.cart.presentation.filter.CartFilterAction
import br.com.zamfir.comprarei.feature.cart.presentation.filter.CartFilterRoot
import br.com.zamfir.comprarei.feature.cart.presentation.filter.CartFilterViewModel
import br.com.zamfir.comprarei.feature.cart.presentation.filter.applyFilter
import androidx.compose.ui.platform.ViewCompositionStrategy
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.feature.cart.presentation.form.CartFormAction
import br.com.zamfir.comprarei.feature.cart.presentation.form.CartFormRoot
import br.com.zamfir.comprarei.feature.cart.presentation.form.CartFormViewModel
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import br.com.zamfir.comprarei.core.util.log.AppLogger
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
import br.com.zamfir.comprarei.core.database.entity.Cart
import br.com.zamfir.comprarei.core.database.entity.Category
import br.com.zamfir.comprarei.core.database.entity.UserInfo
import br.com.zamfir.comprarei.core.util.Constants
import br.com.zamfir.comprarei.core.presentation.isVisible
import br.com.zamfir.comprarei.feature.auth.presentation.LoginActivity
import br.com.zamfir.comprarei.core.presentation.listeners.InfoUpdateListener
import br.com.zamfir.comprarei.feature.auth.presentation.LoginViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.imageview.ShapeableImageView
import com.kevincodes.recyclerview.ItemDecorator
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Collections
import kotlin.math.log

class HomeFragment : Fragment() {
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
    private var lastAction = -1

    private val viewModel: CartViewModel by viewModel()
    private val cartFormViewModel: CartFormViewModel by viewModel()
    private val cartFilterViewModel: CartFilterViewModel by viewModel()
    private val loginViewModel : LoginViewModel by viewModel()

    // Filtro aplicado na lista; reaplicado sempre que a lista muda.
    private var activeFilter = CartFilter()

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
        setupDialogHost()

        viewModel.updateAllData()

        binding.drawerLayout.close()

    }

    private fun setupNavigationDrawer(loggedUser : UserInfo?){
        binding.navigationDrawer.getHeaderView(0).apply {
            val userNameView = findViewById<TextView>(R.id.user_name_navigation)
            val userEmailView = findViewById<TextView>(R.id.user_email_navigation)
            val userProfileView = findViewById<ShapeableImageView>(R.id.profile_picture_navigation)

            userNameView.text = loggedUser?.name ?: context.getString(R.string.ola)
            userEmailView.text = loggedUser?.email ?: context.getString(R.string.welcome)
            if(loggedUser?.profilePicture.isNullOrBlank()) userProfileView.isVisible(false)
            else Glide.with(requireContext())
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

    private fun setupDialogHost() {
        binding.dialogHost.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        binding.dialogHost.setContent {
            ComprareiTheme {
                CartFormRoot(
                    viewModel = cartFormViewModel,
                    onCartCreated = ::saveNewCart,
                    onCartEdited = ::updateCart,
                )
                CartFilterRoot(
                    viewModel = cartFilterViewModel,
                    onApply = ::applyFilter,
                )
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
            cartFormViewModel.onAction(CartFormAction.OnOpenNew(lastKnownPosition))
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
    }

    private fun showCartsItems(cartsList: List<Cart>?, showTotal: Boolean) {
        if (cartsList.isNullOrEmpty()) {
            showEmptyMessage(true)
        } else {
            showEmptyMessage(false)
            cartsAdapter.isShowTotal = showTotal
            originalList.clear()
            originalList.addAll(cartsList)
            if (activeFilter.isActive) {
                refreshVisibleCarts()
                return
            }
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
        if (activeFilter.isActive) {
            refreshVisibleCarts()
            return
        }
        cartsAdapter.differ.submitList(originalList)
        cartsAdapter.notifyItemInserted(cartsAdapter.itemCount + 1)

    }

    private fun updateCart(cart : Cart){
        try {
            viewModel.updateCart(cart)
            // O formulário devolve uma cópia; o firestoreUUID identifica o item mesmo antes de ter id local.
            val index = originalList.indexOfFirst { it.firestoreUUID == cart.firestoreUUID }
            if (index < 0) return
            originalList[index] = cart
            if (activeFilter.isActive) {
                refreshVisibleCarts()
                return
            }
            cartsAdapter.differ.submitList(originalList)
            cartsAdapter.notifyItemChanged(cartsAdapter.differ.currentList.indexOfFirst { it.firestoreUUID == cart.firestoreUUID })
        }catch (e : Exception){
            AppLogger.error("Falha ao atualizar o carrinho na HomeFragment", e)
        }

    }

    private fun deleteCarts(carts : List<Cart>){
        val cartsPosition = cartsAdapter.getCartsPosition(carts)
        viewModel.deleteCarts(carts.map { it.id })
        originalList.removeAll(carts)
        if (activeFilter.isActive) {
            refreshVisibleCarts()
            showEmptyMessage(originalList.isEmpty())
            return
        }
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
                // Com filtro ativo a lista exibida não é a completa: reordenar trocaria os itens errados.
                override fun getDragDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
                    if (activeFilter.isActive) 0 else super.getDragDirs(recyclerView, viewHolder)

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
                            cartFormViewModel.onAction(CartFormAction.OnOpenEdit(cart))
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
                .setMessage(getString(R.string.message_delete_confirmation) + cartsToDelete.first().name + requireContext().getString(R.string.space_interrogation))
                .setIcon(R.drawable.ic_info_24).setPositiveButton(getString(R.string.positive_confirmation)) { dialog, _ ->
                    deleteCarts(cartsToDelete)
                    if(selectionMode) changeSelectState()
                    dialog.dismiss()
                }.setNegativeButton(getString(R.string.negative_confirmation)) { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }else{
            AlertDialog.Builder(context).setTitle(getString(R.string.title_confirmation)).setMessage(
                getString(R.string.message_delete_confirmation) + cartsToDelete.size + getString(R.string.multiple_items_confirmation) + requireContext().getString(R.string.space_interrogation))
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

    private fun doSearch() : Boolean{
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
                val searched = originalList.applyFilter(activeFilter).filter {
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

    private fun sortList() : Boolean{
        cartFilterViewModel.onAction(CartFilterAction.OnOpen(activeFilter, originalList.toList()))
        return true
    }

    private fun applyFilter(filter: CartFilter) {
        activeFilter = filter
        refreshVisibleCarts()
    }

    // Mostra a lista completa já filtrada/ordenada pelo filtro ativo e atualiza o ícone com badge.
    private fun refreshVisibleCarts() {
        cartsAdapter.differ.submitList(originalList.applyFilter(activeFilter))
        sortMenu.icon = AppCompatResources.getDrawable(
            requireContext(),
            if (activeFilter.isActive) R.drawable.icon_filter_bedge else R.drawable.ic_filter_list_24,
        )
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
