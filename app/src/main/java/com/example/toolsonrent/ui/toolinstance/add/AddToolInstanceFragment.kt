package com.example.toolsonrent.ui.toolinstance.add

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.toolsonrent.R
import com.example.toolsonrent.databinding.FragmentAddToolInstanceBinding
import com.example.toolsonrent.utils.ImageFileUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddToolInstanceFragment : Fragment() {

    private var _binding: FragmentAddToolInstanceBinding? = null
    private val binding get() = _binding!!

    private val args: AddToolInstanceFragmentArgs by navArgs()
    private lateinit var viewModel: AddToolInstanceViewModel

    private var selectedInternalImageFileUriString: String? = null
    private lateinit var pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private var tempCameraImageUri: Uri? = null
    private var tempCameraImageFile: File? = null

    private var selectedPurchaseDateInMillis: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupPhotoLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddToolInstanceBinding.inflate(inflater, container, false)
        val factory = AddToolInstanceViewModelFactory(requireActivity().application, args.toolTypeId)
        viewModel = ViewModelProvider(this, factory)[AddToolInstanceViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.title_add_tool_instance)

        setupStatusSpinner()
        setupPurchaseDatePicker()
        setupImageControls()
        setupSaveButton()
        observeSaveResult()
    }

    private fun setupPhotoLaunchers() {
        pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let {
                Log.d("PhotoPickerAddInstance", "Selected URI: $it")
                handleSelectedImage(it)
            }
        }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                tempCameraImageUri?.let { capturedUri ->
                    Log.d("TakePhotoAddInstance", "Image captured: $capturedUri")
                    handleSelectedImage(capturedUri, isTemporaryFile = true)
                }
            } else { // Cleanup if photo not taken or cancelled
                tempCameraImageFile?.delete()
                tempCameraImageFile = null
                tempCameraImageUri = null
            }
        }
    }

    private fun handleSelectedImage(uri: Uri, isTemporaryFile: Boolean = false) {
        val oldImageUriString = selectedInternalImageFileUriString
        val newFile = ImageFileUtil.copyUriContentToInternalAppFile(
            requireContext(),
            uri,
            ImageFileUtil.PERMANENT_INSTANCE_IMAGES_SUBDIR, // Use instance-specific subdir
            "INSTANCE_ADD_IMG_"
        )

        if (newFile != null) {
            val newFileUriString = Uri.fromFile(newFile).toString()
            // Delete old image if one was previously selected in this session for this new instance
            if (oldImageUriString != null && oldImageUriString != newFileUriString) {
                ImageFileUtil.deleteAppInternalFile(requireContext(), oldImageUriString)
            }
            selectedInternalImageFileUriString = newFileUriString
            updateImagePreview()
            Toast.makeText(requireContext(), R.string.instance_image_updated, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), R.string.error_saving_image, Toast.LENGTH_SHORT).show()
        }

        // If the source was a temporary camera file, delete it after copying
        if (isTemporaryFile) {
            tempCameraImageFile?.delete() // Delete the temp file from cache/external-cache
            tempCameraImageFile = null
            tempCameraImageUri = null // Clear the temp URI
        }
    }


    private fun setupStatusSpinner() {
        val statusAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            viewModel.statusOptions.map { getLocalizedStatus(it) } // Map to localized strings
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerInstanceStatus.adapter = statusAdapter
        // Set default selection to "Available" if possible
        val availableStatusLocalized = getLocalizedStatus("Available")
        val defaultPosition = viewModel.statusOptions.map { getLocalizedStatus(it) }.indexOf(availableStatusLocalized)
        if (defaultPosition >= 0) {
            binding.spinnerInstanceStatus.setSelection(defaultPosition)
        }
    }

    private fun getLocalizedStatus(statusKey: String): String {
        return when (statusKey) {
            "Available" -> getString(R.string.label_status_available)
            "Rented" -> getString(R.string.label_status_rented)
            "Maintenance" -> getString(R.string.label_status_maintenance)
            "Damaged" -> getString(R.string.label_status_damaged)
            "Lost" -> getString(R.string.label_status_lost)
            else -> statusKey // Fallback to key if no match
        }
    }

    private fun getStatusKeyFromLocalized(localizedStatus: String): String {
        return when (localizedStatus) {
            getString(R.string.label_status_available) -> "Available"
            getString(R.string.label_status_rented) -> "Rented"
            getString(R.string.label_status_maintenance) -> "Maintenance"
            getString(R.string.label_status_damaged) -> "Damaged"
            getString(R.string.label_status_lost) -> "Lost"
            else -> localizedStatus // Fallback
        }
    }


    private fun setupPurchaseDatePicker() {
        binding.editTextPurchaseDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            if (selectedPurchaseDateInMillis != null) {
                calendar.timeInMillis = selectedPurchaseDateInMillis!!
            }
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }
                selectedPurchaseDateInMillis = selectedCalendar.timeInMillis
                binding.editTextPurchaseDate.setText(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCalendar.time)
                )
            }, year, month, day).show()
        }
         binding.textFieldLayoutPurchaseDate.setEndIconOnClickListener {
            binding.editTextPurchaseDate.performClick()
        }
    }

    private fun setupImageControls() {
        binding.buttonSelectInstanceImage.setOnClickListener { showImageSourceDialog() }
        binding.imageViewInstancePreview.setOnClickListener { showImageSourceDialog() }
        binding.buttonRemoveInstanceImage.setOnClickListener {
            selectedInternalImageFileUriString?.let {
                ImageFileUtil.deleteAppInternalFile(requireContext(), it)
            }
            selectedInternalImageFileUriString = null
            updateImagePreview()
            Toast.makeText(requireContext(), R.string.instance_image_cleared, Toast.LENGTH_SHORT).show()
        }
        updateImagePreview() // Initial state
    }

    private fun showImageSourceDialog() {
        val options = arrayOf(getString(R.string.dialog_option_gallery), getString(R.string.dialog_option_camera))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_title_select_image_source))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    1 -> {
                        tempCameraImageFile = ImageFileUtil.createTempImageFile(requireContext(), "INSTANCE_ADD_TEMP_")
                        tempCameraImageUri = tempCameraImageFile?.let {
                            ImageFileUtil.getUriForFile(requireContext(), it)
                        }
                        if (tempCameraImageUri != null) {
                            takePictureLauncher.launch(tempCameraImageUri)
                        } else {
                            Toast.makeText(requireContext(), getString(R.string.error_creating_temp_file), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .show()
    }

    private fun updateImagePreview() {
        if (selectedInternalImageFileUriString != null) {
            Glide.with(this)
                .load(Uri.parse(selectedInternalImageFileUriString))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_broken_image)
                .into(binding.imageViewInstancePreview)
            binding.buttonRemoveInstanceImage.isVisible = true
        } else {
            Glide.with(this)
                .load(R.drawable.ic_image_placeholder)
                .into(binding.imageViewInstancePreview)
            binding.buttonRemoveInstanceImage.isVisible = false
        }
    }

    private fun setupSaveButton() {
        binding.buttonSaveInstance.setOnClickListener {
            val serialNumber = binding.editTextInstanceSerialNumber.text.toString().trim()
            val selectedLocalizedStatus = binding.spinnerInstanceStatus.selectedItem.toString()
            val statusKey = getStatusKeyFromLocalized(selectedLocalizedStatus)
            val notes = binding.editTextInstanceNotes.text.toString().trim()

            // Clear previous errors
            binding.textFieldLayoutInstanceSerialNumber.error = null
            // Add error handling for spinner if needed, though selection is guaranteed

            viewModel.saveInstance(
                serialNumber.ifEmpty { null }, // Send null if empty
                statusKey,
                selectedPurchaseDateInMillis,
                notes.ifEmpty { null }, // Send null if empty
                selectedInternalImageFileUriString
            )
        }
    }

    private fun observeSaveResult() {
        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), R.string.tool_instance_added_successfully, Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                },
                onFailure = { exception ->
                    Log.e("AddToolInstanceFrag", "Failed to add instance", exception)
                    val errorMessage = when (exception) {
                        is IllegalArgumentException -> exception.message
                        else -> getString(R.string.failed_to_add_instance) + ": " + exception.localizedMessage
                    }
                    // Show specific error for serial number if applicable
                    if (exception.message?.contains("Serial number", ignoreCase = true) == true) {
                        binding.textFieldLayoutInstanceSerialNumber.error = exception.message
                        Toast.makeText(requireContext(), exception.message, Toast.LENGTH_LONG).show()
                    } else {
                         Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Notes:
// 1. AddToolInstanceFragmentArgs needs to be generated by Nav Component (for toolTypeId).
// 2. ImageFileUtil needs PERMANENT_INSTANCE_IMAGES_SUBDIR constant.
// 3. Add string resources used in showImageSourceDialog:
//    <string name="dialog_option_gallery">Choose from Gallery</string>
//    <string name="dialog_option_camera">Take Photo</string>
//    <string name="dialog_title_select_image_source">Select Image Source</string>
//    <string name="error_creating_temp_file">Error creating temporary image file.</string>
//    <string name="error_saving_image">Error saving image.</string> (already exists or generic one)
// 4. Spinner uses localized status strings, maps back to keys for ViewModel.
// 5. Image handling (selection, capture, copy, delete old if new selected) implemented.
// 6. Date picker implemented.
// 7. Basic validation feedback (Toast for general, field error for serial number).
// 8. Assumes ImageFileUtil.kt is in place and handles file operations correctly.
// 9. Need to define PERMANENT_INSTANCE_IMAGES_SUBDIR in ImageFileUtil
//    const val PERMANENT_INSTANCE_IMAGES_SUBDIR = "instance_images"
// 10. Ensure ic_photo_library, ic_delete, ic_calendar, ic_save drawables are present.
// 11. Ensure com.example.toolsonrent.ui.toolinstance.add package is correct.
// 12. Handle title setting.
// 13. Error message for "Status is required" will be handled by ViewModel, but UI could also check.
//     For now, spinner always has a selection.
// 14. Image handling for temp camera file needs to be robust.
// 15. String for error_saving_image (R.string.error_saving_image) might be generic, used one that makes sense.
//     It should be defined in strings.xml if not already present.
//     <string name="error_saving_image">Failed to save image.</string>
// 16. String for tool_instance_added_successfully already added.
// 17. String for failed_to_add_instance already added.
// 18. String for title_add_tool_instance already added.
// 19. Strings for label_status_* already added.
// 20. String for instance_image_cleared already added.
// 21. String for instance_image_updated already added.
// 22. String for R.string.error_creating_temp_file needs to be added.
//     <string name="error_creating_temp_file">Could not create temp file for camera.</string>
// 23. The ImageFileUtil.PERMANENT_INSTANCE_IMAGES_SUBDIR will be added in a separate step if it doesn't exist.
//     For now, I'll assume it will be handled.
// 24. The dialog strings for image source selection need to be added.
//    <string name="dialog_option_gallery">Choose from Gallery</string>
//    <string name="dialog_option_camera">Take Photo</string>
//    <string name="dialog_title_select_image_source">Select Image Source</string>
//
//    I'll add these strings and the error_saving_image, error_creating_temp_file now.
//    And update ImageFileUtil for the new subdirectory.
//
// It's better to add the string R.string.error_saving_image to strings.xml
// <string name="error_saving_image">Failed to save image.</string>
//
// And R.string.error_creating_temp_file
// <string name="error_creating_temp_file">Error creating temporary file for camera.</string>
//
// And dialog strings:
// <string name="dialog_option_gallery">Choose from Gallery</string>
// <string name="dialog_option_camera">Take Photo</string>
// <string name="dialog_title_select_image_source">Select Image</string> <!-- Shorter title -->
