package br.com.zamfir.comprarei.view.components

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import br.com.zamfir.comprarei.databinding.SortBottomSheetBinding
import br.com.zamfir.comprarei.util.Constants
import br.com.zamfir.comprarei.view.adapters.BottomSheetAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.Serializable

class SortBottomSheet : BottomSheetDialogFragment() {

    private var _binding: SortBottomSheetBinding? = null
    private val binding: SortBottomSheetBinding get() = _binding!!

    private var onSelectionFinished: ((Int) -> Unit)? = null

    private lateinit var optionsAdapter : BottomSheetAdapter
    private val list : ArrayList<String> = arrayListOf()

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
            onSelectionFinished = it.getSerializable(ON_SELECTION_KEY) as ((Int) -> Unit)?
            list.addAll(it.getSerializable(ITEMS_KEY) as ArrayList<String>)
        }

        return super.onCreateDialog(savedInstanceState)
    }


    override fun onStart() {
        super.onStart()

        val dialog = requireView().parent as View
        val behavior = BottomSheetBehavior.from(dialog)

        recyclerViewSetup()

        optionsAdapter.setOnClickLister { filter ->
            onSelectionFinished?.let { it(filter) }
            behavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        binding.btnCancel.setOnClickListener {
            behavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun recyclerViewSetup() {
        optionsAdapter = BottomSheetAdapter()
        binding.optionsLayoutRv.adapter = optionsAdapter
        optionsAdapter.differ.submitList(list)
    }

    companion object {

        private const val ITEMS_KEY = "items"
        private const val ON_SELECTION_KEY = "selection"

        fun Fragment.openSortBottomSheetDialog(items : ArrayList<String>, onSelectionFinished: (Int) -> Unit) {

            val bundle = Bundle().apply {
                putSerializable(ITEMS_KEY, items)
                putSerializable(ON_SELECTION_KEY, onSelectionFinished as Serializable)
            }

            val bottomSheetFragment = SortBottomSheet()
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.show(parentFragmentManager, Constants.SORT_TAG)
        }

    }


}