package br.com.zamfir.comprarei.view.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.BottomSheetNewProductBinding
import br.com.zamfir.comprarei.data.model.entity.Product
import br.com.zamfir.comprarei.util.Constants
import br.com.zamfir.comprarei.util.FormatFrom
import br.com.zamfir.comprarei.util.MoneyTextWatcher
import br.com.zamfir.comprarei.util.convertMonetaryToDouble
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.Serializable

class NewProductFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetNewProductBinding? = null
    private val binding: BottomSheetNewProductBinding get() = _binding!!

    private var onFormFinish: ((product: Product) -> Unit)? = null
    private var product: Product? = null
    private var cartId: Int = -1
    private var lastKnowPosition : Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetNewProductBinding.inflate(inflater, container, false)

        binding.addQuantityButton.setOnClickListener {
            val value = Integer.valueOf(binding.productQuantity.text.toString().takeIf { it.isNotBlank()} ?: Constants.DEFAULT_OR_MIN_VALUE_PRODUCT)
            binding.productQuantity.setText(value.plus(1).toString())
        }

        binding.minutsQuantityButton.setOnClickListener {
            val value = Integer.valueOf(binding.productQuantity.text.toString().takeIf { it.isNotBlank()} ?: Constants.DEFAULT_OR_MIN_VALUE_PRODUCT)
            binding.productQuantity.setText(if(value > 0) value.minus(1).toString() else value.toString())
        }

        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.let {
            onFormFinish = it.getSerializable(ON_FORM_FINISH_KEY) as? ((product: Product) -> Unit)?
            if (it.containsKey(CARTID_KEY)) {
                cartId = it.getInt(CARTID_KEY)
            }
            if (it.containsKey(PRODUCT_KEY)) {
                product = it.getSerializable(PRODUCT_KEY) as Product
            }
            if(it.containsKey(LAST_KNOW_POSITION)){
                lastKnowPosition = it.getInt(LAST_KNOW_POSITION)
            }
        }

        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogThemeNoFloating)

        return super.onCreateDialog(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        dialog!!.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                isCancelable = true
                dismiss()
                true
            } else false
        }
    }

    override fun onStart() {
        super.onStart()

        val behavior = BottomSheetBehavior.from(requireView().parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.isDraggable = false
        behavior.isHideable = false
        isCancelable = false

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.productPrice.addTextChangedListener(MoneyTextWatcher(binding.productPrice))

        product?.let {
            setValuesOfProduct(it)
        }

        binding.btnSave.setOnClickListener {
            saveProduct()
        }
    }

    private fun setValuesOfProduct(it: Product) {
        binding.dialogTitle.text = it.name
        binding.productName.setText(it.name)
        binding.productPrice.setText(FormatFrom.doubleToMonetary(getString(R.string.moeda_real), it.price))
        binding.productBrand.setText(it.brand)
        binding.productQuantity.setText(it.quantity.toString())
    }

    private fun saveProduct() {
        if (binding.productName.text.isNullOrEmpty()) {
            binding.productNameLayout.error = Constants.EMPTY_STRING
            binding.productName.error = getString(R.string.can_t_be_blank)
            binding.productName.requestFocus()
        } else {
            onFormFinish?.let {
                if(product != null){
                    if (validateValueQuantity()) {
                        binding.productQuantity.setText(Constants.DEFAULT_OR_MIN_QNT_PRODUCT)
                        product!!.name = binding.productName.text.toString()
                        product!!.brand = binding.productBrand.text.toString()
                        product!!.price = binding.productPrice.text.convertMonetaryToDouble()
                        product!!.quantity = FormatFrom.stringToInt(binding.productQuantity.text.toString().ifBlank { Constants.DEFAULT_OR_MIN_QNT_PRODUCT })
                        it(product!!)

                        isCancelable = true
                        dismiss()
                    } else {
                        product!!.name = binding.productName.text.toString()
                        product!!.brand = binding.productBrand.text.toString()
                        product!!.price = binding.productPrice.text.convertMonetaryToDouble()
                        product!!.quantity = FormatFrom.stringToInt(binding.productQuantity.text.toString().ifBlank { Constants.DEFAULT_OR_MIN_QNT_PRODUCT })
                        it(product!!)

                        isCancelable = true
                        dismiss()
                    }
                }else{
                    if (validateValueQuantity()) {
                        binding.productQuantity.setText(Constants.DEFAULT_OR_MIN_QNT_PRODUCT)
                        it(
                            Product(
                                binding.productName.text.toString(),
                                binding.productBrand.text.toString(),
                                binding.productPrice.text.convertMonetaryToDouble(),
                                FormatFrom.stringToInt(binding.productQuantity.text.toString().ifBlank { Constants.DEFAULT_OR_MIN_QNT_PRODUCT }),
                                cartId,
                                position = lastKnowPosition
                            )
                        )
                        isCancelable = true
                        dismiss()
                    } else {
                        it(
                            Product(
                                binding.productName.text.toString(),
                                binding.productBrand.text.toString(),
                                binding.productPrice.text.convertMonetaryToDouble(),
                                FormatFrom.stringToInt(binding.productQuantity.text.toString().ifBlank { Constants.DEFAULT_OR_MIN_QNT_PRODUCT }),
                                cartId,
                                position = lastKnowPosition
                            )
                        )
                        isCancelable = true
                        dismiss()
                    }
                }
            }
        }
    }

    private fun validateValueQuantity() =
        binding.productPrice.text.convertMonetaryToDouble() != 0.0 && binding.productQuantity.text.toString().ifBlank { Constants.DEFAULT_OR_MIN_VALUE_PRODUCT }.toInt() == 0

    companion object {
        private const val ON_FORM_FINISH_KEY = "OnFormFinish"
        private const val PRODUCT_KEY = "Product"
        private const val CARTID_KEY = "CartId"
        private const val LAST_KNOW_POSITION = "LAST_KNOW_POSITION"

        fun Fragment.openNewProductBottomSheet(
            cartId: Int,
            lastKnowPosition : Int,
            onFormFinish: (product: Product) -> Unit
        ) {
            val bundle = Bundle().apply {
                putSerializable(ON_FORM_FINISH_KEY, onFormFinish as Serializable)
                putInt(CARTID_KEY, cartId)
                putInt(LAST_KNOW_POSITION, lastKnowPosition)
            }

            val bottomSheetFragment = NewProductFragment()
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.show(parentFragmentManager, Constants.EMPTY_STRING)
        }

        fun Fragment.openEditProductBottomSheet(
            product: Product,
            onFormFinish: (product: Product) -> Unit
        ) {
            val bundle = Bundle().apply {
                putSerializable(ON_FORM_FINISH_KEY, onFormFinish as Serializable)
                putSerializable(PRODUCT_KEY, product)
            }

            val bottomSheetFragment = NewProductFragment()
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.show(parentFragmentManager, Constants.EMPTY_STRING)
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}