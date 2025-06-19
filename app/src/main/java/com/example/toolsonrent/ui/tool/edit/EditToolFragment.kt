package com.example.toolsonrent.ui.tool.edit

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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.toolsonrent.R
import com.example.toolsonrent.database.entity.Tool
import com.example.toolsonrent.databinding.FragmentEditToolBinding
import com.example.toolsonrent.utils.ImageFileUtil // Import the utility class
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File // For tempCameraImageFileEdit
import java.util.Locale

class EditToolFragment : Fragment() {

    private var _binding: FragmentEditToolBinding? = null
    private val binding get() = _binding!!

    private val args: EditToolFragmentArgs by navArgs()
    private lateinit var viewModel: EditToolViewModel

    private var selectedInternalImageFileUriString: String? = null
    private lateinit var pickMediaLauncherEdit: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var takePictureLauncherEdit: ActivityResultLauncher<Uri>
    private var tempCameraImageUriEdit: Uri? = null
    private var tempCameraImageFileEdit: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pickMediaLauncherEdit = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                Log.d("PhotoPickerEdit", "Selected URI from gallery: $uri")
                val oldImageUriString = selectedInternalImageFileUriString
                val newFile = ImageFileUtil.copyUriContentToInternalAppFile(
                    requireContext(),
                    uri,
                    ImageFileUtil.PERMANENT_TOOL_IMAGES_SUBDIR,
                    "TOOL_EDIT_GALLERY_"
                )
                if (newFile != null) {
                    val newFileUriString = Uri.fromFile(newFile).toString()
                    if (oldImageUriString != null && oldImageUriString != newFileUriString) {
                        ImageFileUtil.deleteAppInternalFile(requireContext(), oldImageUriString)
                    }
                    selectedInternalImageFileUriString = newFileUriString
                    updateImagePreview()
                } else {
                    Toast.makeText(requireContext(), "Failed to save selected image.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d("PhotoPickerEdit", "No media selected from gallery")
            }
        }

        takePictureLauncherEdit = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                tempCameraImageUriEdit?.let { capturedContentUri ->
                    Log.d("TakePhotoEdit", "Image captured: $capturedContentUri (Temp File: ${tempCameraImageFileEdit?.absolutePath})")
                    val oldImageUriString = selectedInternalImageFileUriString
                    val newPermanentFile = ImageFileUtil.copyUriContentToInternalAppFile(
                        requireContext(),
                        capturedContentUri,
                        ImageFileUtil.PERMANENT_TOOL_IMAGES_SUBDIR,
                        "TOOL_EDIT_CAMERA_"
                    )

                    if (newPermanentFile != null) {
                        val newPermanentFileUriString = Uri.fromFile(newPermanentFile).toString()
                        if (oldImageUriString != null && oldImageUriString != newPermanentFileUriString) {
                            ImageFileUtil.deleteAppInternalFile(requireContext(), oldImageUriString)
                        }
                        selectedInternalImageFileUriString = newPermanentFileUriString
                        updateImagePreview()
                    } else {
                        Toast.makeText(requireContext(), "Failed to save captured image.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Log.d("TakePhotoEdit", "Image capture failed or was cancelled.")
            }
            // Clean up the temporary camera file in either case (success in copying or failure of capture)
            tempCameraImageFileEdit?.let { fileToClean ->
                if (ImageFileUtil.deleteAppInternalFile(requireContext(), Uri.fromFile(fileToClean).toString())) {
                    Log.i("TakePhotoEdit", "Temp camera file deleted: ${fileToClean.absolutePath}")
                } else {
                    Log.w("TakePhotoEdit", "Failed to delete temp camera file: ${fileToClean.absolutePath}")
                }
            }
            tempCameraImageFileEdit = null
            tempCameraImageUriEdit = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditToolBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[EditToolViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeToolDetails()
        setupSaveButton()
        setupImageSelectionButton()
        setupRemoveImageButton()
        setupDeleteButton()
        observeUpdateResult()
        observeDeleteResult()
    }

    private fun observeToolDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tool.collectLatest { tool ->
                    tool?.let { populateForm(it) }
                }
            }
        }
    }

    private fun populateForm(tool: Tool) {
        binding.editTextToolNameEdit.setText(tool.name)
        binding.editTextToolDescriptionEdit.setText(tool.description ?: "")
        binding.editTextRentalPriceEdit.setText(String.format(Locale.US, "%.2f", tool.rentalPrice))
        binding.switchAvailabilityEdit.isChecked = tool.isAvailable
        selectedInternalImageFileUriString = tool.imageUri
        updateImagePreview()
    }

    private fun setupSaveButton() {
        binding.buttonSaveChangesTool.setOnClickListener {
            val name = binding.editTextToolNameEdit.text.toString().trim()
            val description = binding.editTextToolDescriptionEdit.text.toString().trim()
            val priceStr = binding.editTextRentalPriceEdit.text.toString().trim()
            val isAvailable = binding.switchAvailabilityEdit.isChecked
            clearAllErrors()
            viewModel.updateTool(
                currentToolId = args.toolId,
                name = name,
                description = description,
                priceStr = priceStr,
                isAvailable = isAvailable,
                imageUri = selectedInternalImageFileUriString
            )
        }
    }

    private fun setupImageSelectionButton() {
        binding.buttonSelectImageEdit.setOnClickListener { showImageSourceDialogEdit() }
        binding.imageViewToolPreviewEdit.setOnClickListener { showImageSourceDialogEdit() }
    }

    private fun showImageSourceDialogEdit() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select New Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Take Photo
                        tempCameraImageFileEdit = ImageFileUtil.createTempImageFile(requireContext())
                        if (tempCameraImageFileEdit != null) {
                            tempCameraImageUriEdit = ImageFileUtil.getUriForFile(requireContext(), tempCameraImageFileEdit!!)
                            if (tempCameraImageUriEdit != null) {
                                takePictureLauncherEdit.launch(tempCameraImageUriEdit)
                            } else {
                                Toast.makeText(requireContext(), "Could not get URI for camera file.", Toast.LENGTH_SHORT).show()
                                tempCameraImageFileEdit?.delete() // Clean up if URI generation failed
                                tempCameraImageFileEdit = null
                            }
                        } else {
                            Toast.makeText(requireContext(), "Could not create file for camera.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> { // Choose from Gallery
                        pickMediaLauncherEdit.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
            }
            .show()
    }

    private fun setupRemoveImageButton() {
        binding.buttonRemoveImageEdit.setOnClickListener { showConfirmRemoveImageDialog() }
        updateRemoveImageButtonVisibility()
    }

    private fun showConfirmRemoveImageDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove Image")
            .setMessage("Are you sure you want to remove the current image for this tool?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove") { _, _ ->
                ImageFileUtil.deleteAppInternalFile(requireContext(), selectedInternalImageFileUriString)
                selectedInternalImageFileUriString = null
                updateImagePreview()
            }
            .show()
    }

    private fun updateImagePreview() {
        if (selectedInternalImageFileUriString != null) {
            Glide.with(this)
                .load(selectedInternalImageFileUriString)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(R.drawable.ic_baseline_broken_image_24)
                .into(binding.imageViewToolPreviewEdit)
        } else {
            binding.imageViewToolPreviewEdit.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        updateRemoveImageButtonVisibility()
    }

    private fun updateRemoveImageButtonVisibility() {
        binding.buttonRemoveImageEdit.isVisible = selectedInternalImageFileUriString != null
    }

    // Removed local saveImageToInternalStorageEdit and getFileNameEdit as they are now in ImageFileUtil
    // deleteImageFile was also removed as ImageFileUtil.deleteAppInternalFile is used

    private fun observeUpdateResult() { /* ... (existing logic from previous step) ... */ }
    private fun observeDeleteResult() { /* ... (existing logic from previous step) ... */ }
    private fun setupDeleteButton() { /* ... (existing logic from previous step, calls showConfirmDeleteToolDialog) ... */ }
    private fun handleUpdateError(exception: Throwable) { /* ... (existing logic from previous step) ... */ }
    private fun clearAllErrors(){ /* ... (existing logic from previous step) ... */ }
    override fun onDestroyView() { /* ... (existing logic from previous step) ... */ }

    // Ensure these methods are fully defined from previous steps in the final combined code:
    // observeUpdateResult, observeDeleteResult, setupDeleteButton (it calls showConfirmDeleteToolDialog which is fine),
    // handleUpdateError, clearAllErrors, onDestroyView
}
