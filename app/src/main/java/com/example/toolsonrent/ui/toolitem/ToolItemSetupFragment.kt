package com.example.toolsonrent.ui.toolitem

import android.app.Activity
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.toolsonrent.R
import com.example.toolsonrent.databinding.FragmentToolItemSetupBinding
import com.example.toolsonrent.databinding.ItemToolItemDraftEditBinding
import com.example.toolsonrent.utils.ImageFileUtil
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ToolItemSetupFragment : Fragment() {

    private var _binding: FragmentToolItemSetupBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ToolItemSetupViewModel
    private lateinit var draftAdapter: ToolItemDraftAdapter

    // For handling image selection for a specific draft item
    private var currentDraftItemImagePickerIndex: Int = -1
    private lateinit var pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private var tempCameraImageUri: Uri? = null
    private var tempCameraImageFile: File? = null

    private val itemStatuses = arrayOf("Available", "Maintenance", "Reserved", "Damaged", "Missing", "Retired/Scrapped") // From requirements
    private val itemConditions = arrayOf("New", "Good", "Fair", "Poor") // From requirements


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupImageLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolItemSetupBinding.inflate(inflater, container, false)
        // ViewModelProvider will use SavedStateHandle for toolTypeId from nav args
        viewModel = ViewModelProvider(this)[ToolItemSetupViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupImageLaunchers() {
        pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null && currentDraftItemImagePickerIndex != -1) {
                val newFile = ImageFileUtil.copyUriContentToInternalAppFile(
                    requireContext(), uri, ImageFileUtil.PERMANENT_TOOL_IMAGES_SUBDIR, "ITEM_DRAFT_"
                )
                if (newFile != null) {
                    val newFileUriString = Uri.fromFile(newFile).toString()
                    updateDraftItemImageUri(currentDraftItemImagePickerIndex, newFileUriString)
                } else {
                    Toast.makeText(requireContext(), "Failed to save selected image.", Toast.LENGTH_SHORT).show()
                }
            }
            currentDraftItemImagePickerIndex = -1 // Reset
        }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success && tempCameraImageUri != null && currentDraftItemImagePickerIndex != -1) {
                val newPermanentFile = ImageFileUtil.copyUriContentToInternalAppFile(
                    requireContext(), tempCameraImageUri!!, ImageFileUtil.PERMANENT_TOOL_IMAGES_SUBDIR, "ITEM_DRAFT_CAM_"
                )
                if (newPermanentFile != null) {
                     updateDraftItemImageUri(currentDraftItemImagePickerIndex, Uri.fromFile(newPermanentFile).toString())
                } else {
                    Toast.makeText(requireContext(), "Failed to save captured image.", Toast.LENGTH_SHORT).show()
                }
            }
            tempCameraImageFile?.delete()
            tempCameraImageFile = null
            tempCameraImageUri = null
            currentDraftItemImagePickerIndex = -1 // Reset
        }
    }

    private fun updateDraftItemImageUri(index: Int, newImageUri: String?) {
        val currentDrafts = viewModel.generatedDraftItems.value.toMutableList()
        if (index >= 0 && index < currentDrafts.size) {
            val oldImageUri = currentDrafts[index].imageUri
            // Delete old image if it's different and not null
            if (oldImageUri != null && oldImageUri != newImageUri) {
                ImageFileUtil.deleteAppInternalFile(requireContext(), oldImageUri)
            }
            currentDrafts[index] = currentDrafts[index].copy(imageUri = newImageUri)
            viewModel.updateDraftItem(index, currentDrafts[index]) // Let VM update the list internally
        }
    }


    private fun setupRecyclerView() {
        draftAdapter = ToolItemDraftAdapter(
            onItemChange = { index, updatedDraft ->
                viewModel.updateDraftItem(index, updatedDraft)
            },
            onRemoveClick = { draftItem ->
                viewModel.removeDraftItem(draftItem)
            },
            onSetImageClick = { index ->
                currentDraftItemImagePickerIndex = index
                showImageSourceDialogForItem()
            },
            onSetDateClick = { index, isPurchaseDate ->
                showDatePickerDialog(index, isPurchaseDate)
            },
            itemStatuses = itemStatuses,
            itemConditions = itemConditions
        )
        binding.recyclerViewDraftItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = draftAdapter
        }
    }

    private fun showImageSourceDialogForItem() {
         val options = arrayOf("Take Photo", "Choose from Gallery", "Clear Image", "Cancel")
         MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Image Source")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // Take Photo
                        tempCameraImageFile = ImageFileUtil.createTempImageFile(requireContext())
                        tempCameraImageFile?.let { file ->
                            tempCameraImageUri = ImageFileUtil.getUriForFile(requireContext(), file)
                            tempCameraImageUri?.let { uri -> takePictureLauncher.launch(uri) }
                                ?: Toast.makeText(requireContext(), "Error creating URI for camera.", Toast.LENGTH_SHORT).show()
                        } ?: Toast.makeText(requireContext(), "Error creating temp file for camera.", Toast.LENGTH_SHORT).show()
                    }
                    1 -> pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    2 -> { // Clear Image
                        if (currentDraftItemImagePickerIndex != -1) {
                            updateDraftItemImageUri(currentDraftItemImagePickerIndex, null)
                        }
                        currentDraftItemImagePickerIndex = -1
                    }
                    3 -> {
                        dialog.dismiss()
                        currentDraftItemImagePickerIndex = -1
                    }
                }
            }.setOnCancelListener { currentDraftItemImagePickerIndex = -1 }
            .show()
    }

    private fun showDatePickerDialog(itemIndex: Int, isPurchaseDate: Boolean) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val currentDrafts = viewModel.generatedDraftItems.value
        if (itemIndex < 0 || itemIndex >= currentDrafts.size) return

        val existingDate = if (isPurchaseDate) currentDrafts[itemIndex].purchaseDate else currentDrafts[itemIndex].warrantyExpiryDate
        existingDate?.let { calendar.time = it }

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isPurchaseDate) "Select Purchase Date" else "Select Warranty Expiry")
            .setSelection(calendar.timeInMillis)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = Date(selection)
            val updatedDraft = if (isPurchaseDate) {
                currentDrafts[itemIndex].copy(purchaseDate = selectedDate)
            } else {
                currentDrafts[itemIndex].copy(warrantyExpiryDate = selectedDate)
            }
            viewModel.updateDraftItem(itemIndex, updatedDraft)
        }
        datePicker.show(childFragmentManager, "DATE_PICKER_TAG")
    }


    private fun setupClickListeners() {
        binding.buttonGenerateDraftItems.setOnClickListener {
            val countStr = binding.editTextNumberOfItemsToGenerate.text.toString()
            val count = countStr.toIntOrNull()
            if (count != null && count > 0) {
                viewModel.generateDraftItems(count)
            } else {
                Toast.makeText(requireContext(), "Please enter a valid number of items.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.buttonAddSingleDraftItem.setOnClickListener {
            viewModel.addEmptyDraftItem()
        }
        binding.fabSaveAllItems.setOnClickListener {
            // Ensure latest EditText values are pushed to ViewModel before saving
            // This is tricky with RecyclerView; adapter should handle updates via TextWatchers ideally.
            // For now, assume adapter updates ViewModel on focus loss or item change.
            viewModel.saveGeneratedItems()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.toolTypeName.collectLatest { name ->
                val title = "Setup Items for: ${name ?: "Unknown Tool"}"
                binding.toolbarToolItemSetup.title = title
                binding.textViewToolTypeNameHeader.text = title
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.generatedDraftItems.collectLatest { drafts ->
                draftAdapter.submitList(drafts)
            }
        }
        viewModel.generationResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { count -> Toast.makeText(requireContext(), "$count draft items generated.", Toast.LENGTH_SHORT).show() },
                onFailure = { Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LONG_SHORT).show()}
            )
        }
        viewModel.saveItemsResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "All items saved successfully!", Toast.LENGTH_SHORT).show()
                    // Optionally navigate back or clear the list
                    // findNavController().popBackStack()
                },
                onFailure = { exception ->
                    Log.e("ToolItemSetupFragment", "Error saving items", exception)
                    Toast.makeText(requireContext(), "Error saving items: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewDraftItems.adapter = null // Clear adapter to prevent leaks
        _binding = null
    }
}


// --- ToolItemDraftAdapter ---
class ToolItemDraftAdapter(
    private val onItemChange: (Int, TemporaryToolItem) -> Unit,
    private val onRemoveClick: (TemporaryToolItem) -> Unit,
    private val onSetImageClick: (Int) -> Unit, // Pass index
    private val onSetDateClick: (Int, Boolean) -> Unit, // Int for index, Boolean for isPurchaseDate
    private val itemStatuses: Array<String>,
    private val itemConditions: Array<String>
) : ListAdapter<TemporaryToolItem, ToolItemDraftAdapter.ViewHolder>(TemporaryToolItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemToolItemDraftEditBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    inner class ViewHolder(private val binding: ItemToolItemDraftEditBinding) : RecyclerView.ViewHolder(binding.root) {
        // Remove existing listeners to prevent multiple listeners on recycled views
        private fun clearListeners() {
            binding.editTextItemDraftUnitIdUser.onFocusChangeListener = null
            binding.editTextItemDraftSerialNumber.onFocusChangeListener = null
            binding.editTextItemDraftAssetTag.onFocusChangeListener = null
            binding.editTextItemDraftLocation.onFocusChangeListener = null
            binding.editTextItemDraftPurchasePrice.onFocusChangeListener = null
            binding.editTextItemDraftNotes.onFocusChangeListener = null
            binding.spinnerItemDraftStatus.onItemSelectedListener = null
            binding.spinnerItemDraftCondition.onItemSelectedListener = null
        }

        fun bind(item: TemporaryToolItem, position: Int) {
            clearListeners() // Important for RecyclerView

            binding.editTextItemDraftUnitIdUser.setText(item.unitIdUser)
            binding.editTextItemDraftSerialNumber.setText(item.serialNumber ?: "")
            binding.editTextItemDraftAssetTag.setText(item.assetTag ?: "")
            binding.editTextItemDraftLocation.setText(item.currentLocation)
            binding.editTextItemDraftPurchasePrice.setText(item.purchasePrice?.toString() ?: "")
            binding.editTextItemDraftNotes.setText(item.notes ?: "")

            // Image
            Glide.with(binding.imageViewItemDraftPhoto.context)
                .load(item.imageUri ?: R.drawable.ic_placeholder_item)
                .error(R.drawable.ic_placeholder_item_error)
                .placeholder(R.drawable.ic_placeholder_item)
                .into(binding.imageViewItemDraftPhoto)
            binding.buttonChangeItemDraftPhoto.setOnClickListener { onSetImageClick(adapterPosition) }

            // Status Spinner
            val statusAdapter = ArrayAdapter(binding.root.context, android.R.layout.simple_spinner_item, itemStatuses)
            statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerItemDraftStatus.adapter = statusAdapter
            binding.spinnerItemDraftStatus.setSelection(itemStatuses.indexOf(item.status).coerceAtLeast(0))

            // Condition Spinner
            val conditionAdapter = ArrayAdapter(binding.root.context, android.R.layout.simple_spinner_item, itemConditions)
            conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerItemDraftCondition.adapter = conditionAdapter
            binding.spinnerItemDraftCondition.setSelection(itemConditions.indexOf(item.condition).coerceAtLeast(0))

            // Dates
            binding.textViewItemDraftPurchaseDate.text = item.purchaseDate?.let { dateFormat.format(it) } ?: "Not Set"
            binding.buttonItemDraftPurchaseDate.setOnClickListener { onSetDateClick(adapterPosition, true) }
            binding.textViewItemDraftWarrantyExpiryDate.text = item.warrantyExpiryDate?.let { dateFormat.format(it) } ?: "Not Set"
            binding.buttonItemDraftWarrantyExpiryDate.setOnClickListener { onSetDateClick(adapterPosition, false) }


            // Listeners for changes - using onFocusChangeListener for EditText for simplicity
            // For production, TextWatchers would provide more immediate updates.
            binding.editTextItemDraftUnitIdUser.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) onItemChange(adapterPosition, item.copy(unitIdUser = binding.editTextItemDraftUnitIdUser.text.toString())) }
            binding.editTextItemDraftSerialNumber.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) onItemChange(adapterPosition, item.copy(serialNumber = binding.editTextItemDraftSerialNumber.text.toString().ifBlank { null })) }
            binding.editTextItemDraftAssetTag.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) onItemChange(adapterPosition, item.copy(assetTag = binding.editTextItemDraftAssetTag.text.toString().ifBlank { null })) }
            binding.editTextItemDraftLocation.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) onItemChange(adapterPosition, item.copy(currentLocation = binding.editTextItemDraftLocation.text.toString())) }
            binding.editTextItemDraftPurchasePrice.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) onItemChange(adapterPosition, item.copy(purchasePrice = binding.editTextItemDraftPurchasePrice.text.toString().toDoubleOrNull())) }
            binding.editTextItemDraftNotes.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) onItemChange(adapterPosition, item.copy(notes = binding.editTextItemDraftNotes.text.toString().ifBlank { null })) }

            binding.spinnerItemDraftStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) { onItemChange(adapterPosition, item.copy(status = itemStatuses[pos])) }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            binding.spinnerItemDraftCondition.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) { onItemChange(adapterPosition, item.copy(condition = itemConditions[pos])) }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            binding.buttonRemoveDraftItemRow.setOnClickListener { onRemoveClick(item) }
        }
    }
}

class TemporaryToolItemDiffCallback : DiffUtil.ItemCallback<TemporaryToolItem>() {
    override fun areItemsTheSame(oldItem: TemporaryToolItem, newItem: TemporaryToolItem): Boolean = oldItem.localDraftId == newItem.localDraftId
    override fun areContentsTheSame(oldItem: TemporaryToolItem, newItem: TemporaryToolItem): Boolean = oldItem == newItem
}
