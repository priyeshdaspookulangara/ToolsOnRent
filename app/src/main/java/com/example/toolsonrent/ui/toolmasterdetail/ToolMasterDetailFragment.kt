package com.example.toolsonrent.ui.toolmasterdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.toolsonrent.R
import com.example.toolsonrent.database.entity.Tool
import com.example.toolsonrent.database.entity.ToolItem
import com.example.toolsonrent.databinding.FragmentToolMasterDetailBinding
import com.example.toolsonrent.databinding.ItemToolItemListBinding
import com.example.toolsonrent.databinding.ItemToolTypeListBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
// TODO: Replace with actual navigation actions to AddItem and ItemDetail screens
// import androidx.navigation.fragment.findNavController

class ToolMasterDetailFragment : Fragment() {

    private var _binding: FragmentToolMasterDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ToolMasterDetailViewModel
    private lateinit var toolTypeAdapter: ToolTypeAdapter
    private lateinit var toolItemAdapter: ToolItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolMasterDetailBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[ToolMasterDetailViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolTypeRecyclerView()
        setupToolItemRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupToolTypeRecyclerView() {
        toolTypeAdapter = ToolTypeAdapter { toolType ->
            viewModel.selectToolType(toolType.id)
        }
        binding.recyclerViewToolTypes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = toolTypeAdapter
        }
    }

    private fun setupToolItemRecyclerView() {
        toolItemAdapter = ToolItemAdapter { toolItem ->
            // TODO: Navigate to ToolItemDetailFragment, passing toolItem.id
            // findNavController().navigate(ToolMasterDetailFragmentDirections.actionToolMasterDetailFragmentToToolItemDetailFragment(toolItem.id))
            Log.d("ToolItemClick", "Clicked item: ${toolItem.unitIdUser}")
        }
        binding.recyclerViewToolItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = toolItemAdapter
        }
    }

    private fun setupClickListeners() {
        binding.buttonAddNewItemToType.setOnClickListener {
            val selectedToolTypeId = viewModel.selectedToolTypeId.value
            if (selectedToolTypeId != null && selectedToolTypeId != 0) {
                // TODO: Navigate to ToolItemSetupFragment, passing selectedToolTypeId
                // findNavController().navigate(ToolMasterDetailFragmentDirections.actionToolMasterDetailFragmentToToolItemSetupFragment(selectedToolTypeId))
                 Log.d("AddNewItem", "Request to add items for tool type ID: $selectedToolTypeId")
            } else {
                // Should not happen if button is only visible when a type is selected
                 Log.w("AddNewItem", "Add new item clicked but no tool type selected.")
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allToolTypes.collectLatest { toolTypes ->
                toolTypeAdapter.submitList(toolTypes)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedToolTypeName.collectLatest { name ->
                 binding.textViewSelectedToolTypeName.text = name ?: "Select a Tool Type"
                 binding.textViewSelectedToolTypeName.isVisible = true // Always visible, shows placeholder
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.itemsForSelectedToolType.collectLatest { items ->
                // When items for a new tool type are loaded, update the adapter with the parent tool's image URI
                val currentToolTypeId = viewModel.selectedToolTypeId.value
                val parentTool = viewModel.allToolTypes.value.find { it.id == currentToolTypeId }
                toolItemAdapter.setParentToolImageUri(parentTool?.imageUri)

                toolItemAdapter.submitList(items)
                binding.buttonAddNewItemToType.isVisible = viewModel.selectedToolTypeId.value != null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// --- ToolTypeAdapter ---
class ToolTypeAdapter(private val onClick: (Tool) -> Unit) :
    ListAdapter<Tool, ToolTypeAdapter.ToolTypeViewHolder>(ToolTypeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolTypeViewHolder {
        val binding = ItemToolTypeListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ToolTypeViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ToolTypeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ToolTypeViewHolder(
        private val binding: ItemToolTypeListBinding,
        private val onClick: (Tool) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tool: Tool) {
            binding.textViewToolTypeNameRow.text = tool.name
            val priceFormatted = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(tool.rentalPrice)
            binding.textViewToolTypePriceRow.text = "$priceFormatted / day"

            Glide.with(binding.imageViewToolTypeRow.context)
                .load(tool.imageUri ?: R.drawable.ic_placeholder_tool) // ic_placeholder_tool needs to be added
                .error(R.drawable.ic_placeholder_tool_error) // ic_placeholder_tool_error needs to be added
                .placeholder(R.drawable.ic_placeholder_tool)
                .into(binding.imageViewToolTypeRow)

            binding.root.setOnClickListener { onClick(tool) }
            // TODO: Observe available item count from ViewModel for this tool.id and display it
        }
    }
}

class ToolTypeDiffCallback : DiffUtil.ItemCallback<Tool>() {
    override fun areItemsTheSame(oldItem: Tool, newItem: Tool): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Tool, newItem: Tool): Boolean = oldItem == newItem
}

// --- ToolItemAdapter ---
class ToolItemAdapter(private val onItemClick: (ToolItem) -> Unit) :
    ListAdapter<ToolItem, ToolItemAdapter.ToolItemViewHolder>(ToolItemDiffCallback()) {

    private var parentToolImageUri: String? = null // To hold parent tool image for fallback

    // Method to set the parent tool's image URI
    fun setParentToolImageUri(uri: String?) {
        parentToolImageUri = uri
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolItemViewHolder {
        val binding = ItemToolItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ToolItemViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ToolItemViewHolder, position: Int) {
        holder.bind(getItem(position), parentToolImageUri)
    }

    class ToolItemViewHolder(
        private val binding: ItemToolItemListBinding,
        private val onItemClick: (ToolItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ToolItem, parentToolImageUri: String?) {
            binding.textViewItemUnitIdUserRow.text = item.unitIdUser
            binding.textViewItemStatusRow.text = "Status: ${item.status}"
            binding.textViewItemConditionRow.text = "Condition: ${item.condition ?: "N/A"}"

            binding.textViewItemAssetTagRow.text = if (!item.assetTag.isNullOrBlank()) "Asset: ${item.assetTag}" else ""
            binding.textViewItemAssetTagRow.isVisible = !item.assetTag.isNullOrBlank()

            val imageToLoad = item.imageUri ?: parentToolImageUri ?: R.drawable.ic_placeholder_item // ic_placeholder_item
            Glide.with(binding.imageViewToolItemRow.context)
                .load(imageToLoad)
                .error(R.drawable.ic_placeholder_item_error) // ic_placeholder_item_error
                .placeholder(R.drawable.ic_placeholder_item)
                .into(binding.imageViewToolItemRow)

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}

class ToolItemDiffCallback : DiffUtil.ItemCallback<ToolItem>() {
    override fun areItemsTheSame(oldItem: ToolItem, newItem: ToolItem): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: ToolItem, newItem: ToolItem): Boolean = oldItem == newItem
}
