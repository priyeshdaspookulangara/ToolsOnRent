package com.example.toolsonrent.ui.toolitem

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
// import androidx.navigation.fragment.navArgs // ViewModel handles arg via SavedStateHandle
import com.bumptech.glide.Glide
import com.example.toolsonrent.R
import com.example.toolsonrent.databinding.FragmentEditToolItemBinding
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

class EditToolItemFragment : Fragment() {

    private var _binding: FragmentEditToolItemBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: EditToolItemViewModel

    private lateinit var pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private var tempCameraImageUri: Uri? = null
    private var tempCameraImageFile: File? = null

    private val itemStatuses = arrayOf("Available", "On Rent", "Maintenance", "Damaged", "Missing", "Retired/Scrapped", "Reserved")
    private val itemConditions = arrayOf("New", "Good", "Fair", "Poor")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupImageLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditToolItemBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[EditToolItemViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupImageLaunchers() {
        pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let {
                val oldImageUri = viewModel.imageUri.value
                val newFile = ImageFileUtil.copyUriContentToInternalAppFile(
                    requireContext(), it, ImageFileUtil.PERMANENT_TOOL_IMAGES_SUBDIR, "ITEM_EDIT_"
                )
                if (newFile != null) {
                    val newFileUriString = Uri.fromFile(newFile).toString()
                    oldImageUri?.let { old -> if (old != newFileUriString) ImageFileUtil.deleteAppInternalFile(requireContext(), old) }
                    viewModel.imageUri.value = newFileUriString
                } else {
                    Toast.makeText(requireContext(), "Failed to save image.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                tempCameraImageUri?.let { capturedUri ->
                    val oldImageUri = viewModel.imageUri.value
                    val newFile = ImageFileUtil.copyUriContentToInternalAppFile(
                        requireContext(), capturedUri, ImageFileUtil.PERMANENT_TOOL_IMAGES_SUBDIR, "ITEM_EDIT_CAM_"
                    )
                    if (newFile != null) {
                        val newFileUriString = Uri.fromFile(newFile).toString()
                        oldImageUri?.let { old -> if (old != newFileUriString) ImageFileUtil.deleteAppInternalFile(requireContext(), old) }
                        viewModel.imageUri.value = newFileUriString
                    } else {
                        Toast.makeText(requireContext(), "Failed to save image.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            tempCameraImageFile?.delete()
            tempCameraImageFile = null
            tempCameraImageUri = null
        }
    }

    private fun setupSpinners() {
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, itemStatuses)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerItemEditStatus.adapter = statusAdapter

        val conditionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, itemConditions)
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerItemEditCondition.adapter = conditionAdapter
    }

    private fun setupClickListeners() {
        binding.buttonChangeItemEditPhoto.setOnClickListener { showImageSourceDialog() }
        binding.buttonRemoveItemEditPhoto.setOnClickListener {
            val oldImageUri = viewModel.imageUri.value
            viewModel.imageUri.value = null
            oldImageUri?.let { ImageFileUtil.deleteAppInternalFile(requireContext(), it) }
        }

        binding.buttonItemEditPurchaseDate.setOnClickListener { showDatePickerDialog(true) }
        binding.buttonItemEditWarrantyExpiryDate.setOnClickListener { showDatePickerDialog(false) }

        binding.buttonSaveItemChanges.setOnClickListener {
            // Push current UI values to ViewModel StateFlows before saving
            viewModel.unitIdUser.value = binding.editTextItemEditUnitIdUser.text.toString()
            viewModel.serialNumber.value = binding.editTextItemEditSerialNumber.text.toString().ifBlank { null }
            viewModel.assetTag.value = binding.editTextItemEditAssetTag.text.toString().ifBlank { null }
            viewModel.currentLocation.value = binding.editTextItemEditLocation.text.toString()
            viewModel.status.value = binding.spinnerItemEditStatus.selectedItem.toString()
            viewModel.condition.value = binding.spinnerItemEditCondition.selectedItem.toString()
            viewModel.purchasePrice.value = binding.editTextItemEditPurchasePrice.text.toString().toDoubleOrNull()
            // Dates are updated directly in ViewModel by DatePicker
            viewModel.notes.value = binding.editTextItemEditNotes.text.toString().ifBlank { null }

            viewModel.updateToolItem()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Image Source")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        tempCameraImageFile = ImageFileUtil.createTempImageFile(requireContext())
                        tempCameraImageFile?.let { file ->
                            tempCameraImageUri = ImageFileUtil.getUriForFile(requireContext(), file)
                            tempCameraImageUri?.let { uri -> takePictureLauncher.launch(uri) }
                        }
                    }
                    1 -> pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    2 -> dialog.dismiss()
                }
            }.show()
    }

    private fun showDatePickerDialog(isPurchaseDate: Boolean) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val currentDateVal = if (isPurchaseDate) viewModel.purchaseDate.value else viewModel.warrantyExpiryDate.value
        currentDateVal?.let { calendar.time = it }

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isPurchaseDate) "Select Purchase Date" else "Select Warranty Expiry")
            .setSelection(calendar.timeInMillis)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = Date(selection)
            if (isPurchaseDate) {
                viewModel.purchaseDate.value = selectedDate
            } else {
                viewModel.warrantyExpiryDate.value = selectedDate
            }
        }
        datePicker.show(childFragmentManager, "EDIT_ITEM_DATE_PICKER")
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isDataLoaded.collectLatest { loaded ->
                if (loaded) {
                    // Initial population, subsequent updates handled by individual collectors for two-way binding feel
                }
            }
        }

        // Bind EditTexts (one-way from ViewModel, user input changes local EditText directly)
        // For two-way binding, TextWatchers would update viewModel.property.value
        viewLifecycleOwner.lifecycleScope.launch { viewModel.unitIdUser.collectLatest { binding.editTextItemEditUnitIdUser.setText(it) } }
        viewLifecycleOwner.lifecycleScope.launch { viewModel.serialNumber.collectLatest { binding.editTextItemEditSerialNumber.setText(it ?: "") } }
        viewLifecycleOwner.lifecycleScope.launch { viewModel.assetTag.collectLatest { binding.editTextItemEditAssetTag.setText(it ?: "") } }
        viewLifecycleOwner.lifecycleScope.launch { viewModel.currentLocation.collectLatest { binding.editTextItemEditLocation.setText(it) } }
        viewLifecycleOwner.lifecycleScope.launch { viewModel.purchasePrice.collectLatest { binding.editTextItemEditPurchasePrice.setText(it?.toString() ?: "") } }
        viewLifecycleOwner.lifecycleScope.launch { viewModel.notes.collectLatest { binding.editTextItemEditNotes.setText(it ?: "") } }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.imageUri.collectLatest { uriString ->
                Glide.with(requireContext())
                    .load(uriString ?: R.drawable.ic_placeholder_item)
                    .error(R.drawable.ic_placeholder_item_error)
                    .placeholder(R.drawable.ic_placeholder_item)
                    .into(binding.imageViewItemEditPhoto)
                binding.buttonRemoveItemEditPhoto.isVisible = uriString != null
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.status.collectLatest { statusVal ->
                binding.spinnerItemEditStatus.setSelection(itemStatuses.indexOf(statusVal).coerceAtLeast(0))
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.condition.collectLatest { condVal ->
                binding.spinnerItemEditCondition.setSelection(itemConditions.indexOf(condVal).coerceAtLeast(0))
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.purchaseDate.collectLatest { date ->
                binding.textViewItemEditPurchaseDate.text = date?.let { dateFormat.format(it) } ?: "Not Set"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.warrantyExpiryDate.collectLatest { date ->
                binding.textViewItemEditWarrantyExpiryDate.text = date?.let { dateFormat.format(it) } ?: "Not Set"
            }
        }
         viewLifecycleOwner.lifecycleScope.launch {
            viewModel.toolItemId.collectLatest { id ->
                 viewModel.originalToolItem?.let { item -> // Check if original item is loaded
                    binding.textViewEditItemHeader.text = "Edit Item: ${item.unitIdUser} (ID: ${item.id})"
                } ?: id?.let {
                     binding.textViewEditItemHeader.text = "Edit Item ID: $it"
                 }
            }
        }


        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Item updated successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                },
                onFailure = { exception ->
                    Toast.makeText(requireContext(), "Error updating item: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
