package br.com.iesb.comprarei.view.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import br.com.iesb.comprarei.R
import br.com.iesb.comprarei.databinding.BottomSheetNewProductBinding
import br.com.iesb.comprarei.model.Product
import br.com.iesb.comprarei.util.FormatFrom
import br.com.iesb.comprarei.util.errorAnimation
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.Serializable

class NewProductFragment : BottomSheetDialogFragment() {


    private var _binding: BottomSheetNewProductBinding? = null
    private val binding: BottomSheetNewProductBinding get() = _binding!!

    private var onFormFinish : ((product : Product) -> Unit)? = null
    private var product : Product? = null
    private var cartId : String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetNewProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.let {
            onFormFinish = it.getSerializable(ON_FORM_FINISH_KEY) as? ((product : Product) -> Unit)?
            if(it.containsKey(CARTID_KEY)){
                cartId = it.getString(CARTID_KEY) as String
            }
            if(it.containsKey(PRODUCT_KEY)){
                product = it.getSerializable(PRODUCT_KEY) as Product
            }
        }

        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogThemeNoFloating)

        return super.onCreateDialog(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()

        val dialog = requireView().parent as View
        dialog.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

        val behavior = BottomSheetBehavior.from(dialog)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.isDraggable = false
        behavior.isHideable = false
        isCancelable = false

        binding.btnCancel.setOnClickListener {
            isCancelable = true
            behavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        product?.let {
            println("#########${it.id}#########")
            binding.productName.setText(it.name)
            binding.productPrice.setText(it.price.toString())
            binding.productBrand.setText(it.brand)
            binding.productQuantity.setText(it.quantity.toString())
        }


        binding.btnSave.setOnClickListener {
            if (binding.productName.text.isNullOrEmpty()) {
                binding.productName.errorAnimation("Name can't be empty")
            } else if (binding.productPrice.text.isNullOrEmpty() || isZero(binding.productPrice.text.toString())) {
                binding.productPrice.errorAnimation("Invalid price")
            } else if (binding.productQuantity.text.isNullOrEmpty() || isZero(binding.productQuantity.text.toString())) {
                binding.productQuantity.errorAnimation("Invalid quantity")
            } else {
                onFormFinish?.let {

                    product?.let { product ->
                        product.name = binding.productName.text.toString()
                        product.brand = binding.productBrand.text.toString()
                        product.price =
                            FormatFrom.stringToDouble(binding.productPrice.text.toString())
                        product.quantity =
                            FormatFrom.stringToInt(binding.productQuantity.text.toString())

                    } ?: run {
                        product = Product(
                            binding.productName.text.toString(),
                            binding.productBrand.text.toString(),
                            FormatFrom.stringToDouble(binding.productPrice.text.toString()),
                            FormatFrom.stringToInt(binding.productQuantity.text.toString()),
                            cartId
                        )
                    }

                    it(product!!)

                    isCancelable = true
                    behavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
        }
    }

    private fun isZero(text: String): Boolean {
        try {
            val value = text.toDouble()
            if (value <= 0) {
                return true
            }
            return false
        } catch (e: Exception) {
            return true
        }
    }

    companion object {
        private const val ON_FORM_FINISH_KEY = "OnFormFinish"
        private const val PRODUCT_KEY = "Product"
        private const val CARTID_KEY = "CartId"

        fun Fragment.openNewProductBottomSheet(cartId : String,onFormFinish: (product : Product) -> Unit) {

            val bundle = Bundle().apply {
                putSerializable(ON_FORM_FINISH_KEY, onFormFinish as Serializable)
                putString(CARTID_KEY, cartId)
            }

            val bottomSheetFragment = NewProductFragment()
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.show(parentFragmentManager, "BOTTOMNEWPRODUCT")
        }

        fun Fragment.openEditProductBottomSheet(product : Product,onFormFinish: (product : Product) -> Unit) {

            val bundle = Bundle().apply {
                putSerializable(ON_FORM_FINISH_KEY, onFormFinish as Serializable)
                putSerializable(PRODUCT_KEY, product)
            }

            val bottomSheetFragment = NewProductFragment()
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.show(parentFragmentManager, "BOTTOMNEWPRODUCT")
        }
    }

}