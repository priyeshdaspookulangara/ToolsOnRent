package com.example.toolsonrent.ui.tool.edit

import android.content.ContentResolver // Not used directly, but ImageFileUtil might need it if getFileNameEdit was here
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns // Not used directly
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
import com.example.toolsonrent.utils.ImageFileUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
// SimpleDateFormat, Date, UUID are used by ImageFileUtil or for temp file naming if done locally
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
        setupManageInstancesButton() // Added
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
        // Quantities are now managed by instances, so remove these lines
        // binding.editTextTotalQuantityEdit.setText(tool.totalQuantity.toString())
        // binding.textViewCurrentAvailableQuantityEdit.text = tool.currentAvailableQuantity.toString()

        // Hide or update UI elements related to direct quantity editing
        binding.textFieldLayoutTotalQuantityEdit.visibility = View.GONE
        binding.textViewCurrentAvailableQuantityLabel.visibility = View.GONE
        binding.textViewCurrentAvailableQuantityEdit.visibility = View.GONE

        selectedInternalImageFileUriString = tool.imageUri
        updateImagePreview()
    }

    private fun setupSaveButton() {
        binding.buttonSaveChangesTool.setOnClickListener {
            val name = binding.editTextToolNameEdit.text.toString().trim()
            val description = binding.editTextToolDescriptionEdit.text.toString().trim()
            val priceStr = binding.editTextRentalPriceEdit.text.toString().trim()
            // totalQuantityStr is no longer directly edited here.
            // It will be derived from instances. The ViewModel's updateTool method will need to change.

            clearAllErrors()
            // ViewModel's updateTool signature will change - totalQuantityStr is removed
            viewModel.updateTool(
                currentToolId = args.toolId,
                name = name,
                description = description,
                priceStr = priceStr,
                imageUri = selectedInternalImageFileUriString
            )
        }
    }

    private fun setupManageInstancesButton() { // Added
        binding.buttonManageInstances.setOnClickListener {
            val action = EditToolFragmentDirections.actionEditToolFragmentToToolInstanceListFragment(args.toolId)
            findNavController().navigate(action)
        }
    }

    private fun setupImageSelectionButton() { binding.buttonSelectImageEdit.setOnClickListener { showImageSourceDialogEdit() }; binding.imageViewToolPreviewEdit.setOnClickListener { showImageSourceDialogEdit() } }
    private fun showImageSourceDialogEdit() {
         val options = arrayOf(getString(R.string.dialog_option_gallery), getString(R.string.dialog_option_camera))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_title_select_image_source))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickMediaLauncherEdit.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    1 -> {
                        tempCameraImageFileEdit = ImageFileUtil.createTempImageFile(requireContext(), "TOOL_EDIT_TEMP_")
                        tempCameraImageUriEdit = tempCameraImageFileEdit?.let {
                            ImageFileUtil.getUriForFile(requireContext(), it)
                        }
                        if (tempCameraImageUriEdit != null) {
                            takePictureLauncherEdit.launch(tempCameraImageUriEdit)
                        } else {
                            Toast.makeText(requireContext(), getString(R.string.error_creating_temp_file), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .show()
    }
    private fun setupRemoveImageButton() { binding.buttonRemoveImageEdit.setOnClickListener { showConfirmRemoveImageDialog() }; updateRemoveImageButtonVisibility() }
    private fun showConfirmRemoveImageDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove Image?")
            .setMessage("Are you sure you want to remove the image for this tool type?")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                selectedInternalImageFileUriString = null // ViewModel will handle actual file deletion on save
                updateImagePreview()
                Toast.makeText(requireContext(), "Image will be removed on save.", Toast.LENGTH_SHORT).show()
            }
            .show()

    }
    private fun updateImagePreview() {
        if (selectedInternalImageFileUriString != null) {
            Glide.with(this).load(Uri.parse(selectedInternalImageFileUriString))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_broken_image)
                .into(binding.imageViewToolPreviewEdit)
        } else {
            Glide.with(this).load(R.drawable.ic_image_placeholder) // Default placeholder
                .into(binding.imageViewToolPreviewEdit)
        }
        updateRemoveImageButtonVisibility()
    }
    private fun updateRemoveImageButtonVisibility() { binding.buttonRemoveImageEdit.isVisible = selectedInternalImageFileUriString != null }

    private fun setupDeleteButton() {
        binding.buttonDeleteTool.setOnClickListener {
             MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Tool Type?")
                .setMessage("Are you sure you want to delete this tool type and all its instances? This action cannot be undone.")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.deleteTool() }
                .show()
        }
    }
    private fun observeUpdateResult() {
         viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Tool type updated successfully.", Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                },
                onFailure = { exception -> handleUpdateError(exception) }
            )
        }
    }
    private fun observeDeleteResult() {
        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Tool type deleted successfully.", Toast.LENGTH_LONG).show()
                    // Consider where to navigate after deleting a tool type, e.g., back to tool list
                    findNavController().popBackStack() // Or navigate to tool list
                },
                onFailure = { exception ->
                    Toast.makeText(requireContext(), "Delete failed: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun handleUpdateError(exception: Throwable) {
        val message = exception.message ?: "Unknown error."
        Toast.makeText(requireContext(), "Update failed: $message", Toast.LENGTH_LONG).show()

        if (exception is IllegalArgumentException) {
            if (message.contains("Tool name", ignoreCase = true)) {
                binding.textFieldLayoutToolNameEdit.error = message
            } else if (message.contains("rental price", ignoreCase = true)) {
                binding.textFieldLayoutRentalPriceEdit.error = message
            }
            // Removed quantity error handling here as it's no longer directly edited
        } else if (exception is IllegalStateException && message.contains("Invalid Tool ID", ignoreCase = true)) {
             Toast.makeText(requireContext(), "Error: Cannot update tool. Invalid ID.", Toast.LENGTH_LONG).show()
        }
    }

    private fun clearAllErrors(){
        binding.textFieldLayoutToolNameEdit.error = null
        binding.textFieldLayoutToolDescriptionEdit.error = null
        binding.textFieldLayoutRentalPriceEdit.error = null
        // binding.textFieldLayoutTotalQuantityEdit.error = null // No longer directly edited
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

        super.onDestroyView()
        _binding = null
    }
}
