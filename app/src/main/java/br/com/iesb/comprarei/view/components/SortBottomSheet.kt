package br.com.iesb.comprarei.view.components

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import br.com.iesb.comprarei.databinding.SortBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.Serializable

class SortBottomSheet : BottomSheetDialogFragment() {

    private var _binding: SortBottomSheetBinding? = null
    private val binding: SortBottomSheetBinding get() = _binding!!

    private var onSelectionFinished: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SortBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.let {
            onSelectionFinished = it.getSerializable(ON_SELECTION_KEY) as ((String) -> Unit)?
        }
        return super.onCreateDialog(savedInstanceState)
    }


    override fun onStart() {
        super.onStart()

        val dialog = requireView().parent as View
        val behavior = BottomSheetBehavior.from(dialog)



        binding.btnAlphabetical.setOnClickListener { btn ->
            onSelectionFinished?.let {
                it(binding.btnAlphabetical.text.toString())
                behavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }

        binding.btnInverseAlphabetical.setOnClickListener { btn ->
            onSelectionFinished?.let {
                it(binding.btnInverseAlphabetical.text.toString())
                behavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }

        binding.btnOriginal.setOnClickListener { btn ->
            onSelectionFinished?.let {
                it(binding.btnOriginal.text.toString())
                behavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }

    }

    companion object {

        private const val ON_SELECTION_KEY = "selection"

        fun Fragment.openBottomSheetDialog(onSelectionFinished: (String) -> Unit) {

            val bundle = Bundle().apply {
                putSerializable(ON_SELECTION_KEY, onSelectionFinished as Serializable)
            }

            val bottomSheetFragment = SortBottomSheet()
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.show(parentFragmentManager, "BOTTOMSHEET")
        }

    }


}