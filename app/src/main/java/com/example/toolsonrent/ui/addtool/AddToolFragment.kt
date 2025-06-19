package com.example.toolsonrent.ui.addtool

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.toolsonrent.R // For R.drawable.ic_baseline_broken_image_24
import com.example.toolsonrent.databinding.FragmentAddToolBinding
import com.example.toolsonrent.utils.ImageFileUtil // Import the utility class
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File // Still needed for tempCameraImageFile if we manage its File object for deletion
// SimpleDateFormat, Date, Locale, UUID are used by ImageFileUtil internally

class AddToolFragment : Fragment() {

    private var _binding: FragmentAddToolBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AddToolViewModel

    private var selectedInternalImageFileUriString: String? = null
    private lateinit var pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private var tempCameraImageUri: Uri? = null // Uri provided to camera app
    private var tempCameraImageFile: File? = null // Actual temp file created for camera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                Log.d("PhotoPicker", "Selected URI from gallery: $uri")
                val newFile = ImageFileUtil.copyUriContentToInternalAppFile(
                    requireContext(),
                    uri,
                    ImageFileUtil.PERMANENT_TOOL_IMAGES_SUBDIR,
                    "TOOL_GALLERY_"
                )
                if (newFile != null) {
                    selectedInternalImageFileUriString = Uri.fromFile(newFile).toString()
                    updateImagePreview()
                } else {
                    Toast.makeText(requireContext(), "Failed to save selected image.", Toast.LENGTH_SHORT).show()
                    // Reset preview if needed, e.g., if replacing an existing image attempt failed
                    // selectedInternalImageFileUriString = null
                    // updateImagePreview()
                }
            } else {
                Log.d("PhotoPicker", "No media selected from gallery")
            }
        }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                tempCameraImageUri?.let { capturedContentUri -> // This is the content URI from FileProvider
                    Log.d("TakePhoto", "Image captured successfully at: $capturedContentUri (Temp File: ${tempCameraImageFile?.absolutePath})")
                    val newPermanentFile = ImageFileUtil.copyUriContentToInternalAppFile(
                        requireContext(),
                        capturedContentUri, // Source is the temp content URI FileProvider gave us
                        ImageFileUtil.PERMANENT_TOOL_IMAGES_SUBDIR,
                        "TOOL_CAMERA_"
                    )
                    if (newPermanentFile != null) {
                        selectedInternalImageFileUriString = Uri.fromFile(newPermanentFile).toString()
                        updateImagePreview()
                    } else {
                        Toast.makeText(requireContext(), "Failed to save captured image.", Toast.LENGTH_SHORT).show()
                    }

                    // Clean up the temporary camera file using its File object
                    tempCameraImageFile?.let { fileToClean ->
                        if (fileToClean.exists() && fileToClean.delete()) {
                             Log.i("TakePhoto", "Temp camera file deleted: ${fileToClean.absolutePath}")
                        } else {
                            Log.w("TakePhoto", "Failed to delete temp camera file or it didn't exist: ${fileToClean.absolutePath}")
                        }
                    }
                }
            } else {
                Log.d("TakePhoto", "Image capture failed or was cancelled.")
                // If capture failed, delete the (likely empty) temp file we created
                tempCameraImageFile?.let {
                    if (it.exists() && it.delete()) {
                        Log.d("TakePhoto", "Temp file for failed capture deleted.")
                    }
                }
            }
            // Always clear temp file/URI references after a take picture attempt
            tempCameraImageFile = null
            tempCameraImageUri = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddToolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[AddToolViewModel::class.java]

        setupSaveButton()
        setupImageSelectionClickListeners()
        observeSaveResult()
        updateImagePreview() // Initial preview state
    }

    private fun setupSaveButton() {
        binding.buttonSaveTool.setOnClickListener {
            val toolName = binding.editTextToolName.text.toString().trim()
            val description = binding.editTextToolDescription.text.toString().trim()
            val rentalPriceStr = binding.editTextRentalPrice.text.toString().trim()
            val isAvailable = binding.switchAvailability.isChecked

            // Client-side validation (ViewModel also validates)
            var isValid = true
            binding.textFieldLayoutToolName.error = null
            binding.textFieldLayoutRentalPrice.error = null
            if (toolName.isEmpty()) {
                binding.textFieldLayoutToolName.error = "Tool name cannot be empty"; isValid = false
            }
            val rentalPriceDouble = rentalPriceStr.toDoubleOrNull()
            if (rentalPriceDouble == null || rentalPriceDouble <= 0) {
                binding.textFieldLayoutRentalPrice.error = "Enter a valid positive price"; isValid = false
            }

            if (isValid) {
                viewModel.addTool(
                    name = toolName,
                    description = description.ifEmpty { null },
                    priceStr = rentalPriceStr,
                    isAvailable = isAvailable,
                    imageUri = selectedInternalImageFileUriString
                )
            }
        }
    }

    private fun setupImageSelectionClickListeners() {
        binding.buttonSelectImage.setOnClickListener { showImageSourceDialog() }
        binding.imageViewToolPreview.setOnClickListener { showImageSourceDialog() }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Take Photo
                        tempCameraImageFile = ImageFileUtil.createTempImageFile(requireContext()) // Uses default subdir
                        if (tempCameraImageFile != null) {
                            tempCameraImageUri = ImageFileUtil.getUriForFile(requireContext(), tempCameraImageFile!!)
                            if (tempCameraImageUri != null) {
                                takePictureLauncher.launch(tempCameraImageUri)
                            } else {
                                Toast.makeText(requireContext(), "Could not get URI for camera file.", Toast.LENGTH_SHORT).show()
                                tempCameraImageFile?.delete() // Clean up if URI generation failed
                                tempCameraImageFile = null
                            }
                        } else {
                            Toast.makeText(requireContext(), "Could not create file for camera.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> { // Choose from Gallery
                        pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
            }
            .show()
    }

    private fun updateImagePreview() {
        if (selectedInternalImageFileUriString != null) {
            Glide.with(this)
                .load(selectedInternalImageFileUriString)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(R.drawable.ic_baseline_broken_image_24) // Ensure this drawable exists
                .into(binding.imageViewToolPreview)
        } else {
            binding.imageViewToolPreview.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    private fun observeSaveResult() {
        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Tool saved successfully!", Toast.LENGTH_SHORT).show()
                    clearForm()
                },
                onFailure = { exception ->
                    Toast.makeText(requireContext(), "Error saving tool: ${exception.message}", Toast.LENGTH_LONG).show()
                    Log.e("AddToolFragment", "Error saving tool", exception)
                }
            )
        }
    }

    private fun clearForm() {
        binding.editTextToolName.text?.clear()
        binding.editTextToolDescription.text?.clear()
        binding.editTextRentalPrice.text?.clear()
        binding.switchAvailability.isChecked = true

        selectedInternalImageFileUriString = null // Clear selected image URI
        updateImagePreview() // Reset preview to placeholder

        binding.textFieldLayoutToolName.error = null
        binding.textFieldLayoutToolDescription.error = null
        binding.textFieldLayoutRentalPrice.error = null
        binding.editTextToolName.requestFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
