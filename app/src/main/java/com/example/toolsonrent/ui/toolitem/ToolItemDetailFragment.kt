package com.example.toolsonrent.ui.toolitem

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
// import androidx.navigation.fragment.navArgs // ViewModel gets ID from SavedStateHandle
import com.bumptech.glide.Glide
import com.example.toolsonrent.R
import com.example.toolsonrent.databinding.FragmentToolItemDetailBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ToolItemDetailFragment : Fragment() {

    private var _binding: FragmentToolItemDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ToolItemDetailViewModel
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolItemDetailBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[ToolItemDetailViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        observeToolItemDetails()
        setupEditButton()
    }

    private fun setupToolbar() {
        binding.toolbarToolItemDetail.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun observeToolItemDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.toolItemDetails.collectLatest { details ->
                if (details != null) {
                    val item = details.toolItem
                    val toolType = details.toolType

                    binding.toolbarToolItemDetail.title = "Item: ${item.unitIdUser}"
                    binding.textViewItemDetailUnitIdUser.text = item.unitIdUser
                    binding.textViewItemDetailToolTypeName.text = "Type: ${toolType?.name ?: "N/A"}"

                    binding.textViewItemDetailStatus.text = item.status
                    binding.textViewItemDetailCondition.text = item.condition ?: "N/A"
                    binding.textViewItemDetailLocation.text = item.currentLocation ?: "N/A"
                    binding.textViewItemDetailSerialNumber.text = item.serialNumber ?: "N/A"
                    binding.textViewItemDetailAssetTag.text = item.assetTag ?: "N/A"

                    binding.textViewItemDetailPurchaseDate.text = item.purchaseDate?.let { dateFormat.format(it) } ?: "N/A"
                    binding.textViewItemDetailPurchasePrice.text = item.purchasePrice?.let { currencyFormat.format(it) } ?: "N/A"
                    binding.textViewItemDetailWarrantyExpiry.text = item.warrantyExpiryDate?.let { dateFormat.format(it) } ?: "N/A"
                    binding.textViewItemDetailNotes.text = item.notes ?: "None"

                    // Image fallback logic: Item Image -> Tool Type Image -> Placeholder
                    val imageToLoad = item.imageUri ?: toolType?.imageUri ?: R.drawable.ic_placeholder_item
                    Glide.with(requireContext())
                        .load(imageToLoad)
                        .error(R.drawable.ic_placeholder_item_error) // General item error placeholder
                        .placeholder(R.drawable.ic_placeholder_item) // General item placeholder
                        .into(binding.imageViewItemDetailPhoto)

                } else {
                    // Handle item not found or error state
                    binding.toolbarToolItemDetail.title = "Item Not Found"
                    // Show error message in UI, hide fields etc.
                    Toast.makeText(context, "Tool item details could not be loaded.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupEditButton() {
        binding.buttonEditThisItem.setOnClickListener {
            viewModel.toolItemDetails.value?.toolItem?.id?.let { itemId ->
                // TODO: Navigate to EditToolItemFragment, passing itemId
                // val action = ToolItemDetailFragmentDirections.actionToolItemDetailFragmentToEditToolItemFragment(itemId)
                // findNavController().navigate(action)
                Log.d("ToolItemDetail", "Navigate to edit item ID: $itemId")
                 Toast.makeText(context, "Edit for item $itemId (TODO: Nav)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
