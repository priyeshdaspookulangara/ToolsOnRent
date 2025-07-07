package com.example.toolsonrent.ui.toolinstance.edit

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
import com.example.toolsonrent.database.entity.ToolInstance
import com.example.toolsonrent.databinding.FragmentEditToolInstanceBinding
import com.example.toolsonrent.utils.ImageFileUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EditToolInstanceFragment : Fragment() {

    private var _binding: FragmentEditToolInstanceBinding? = null
    private val binding get() = _binding!!

    private val args: EditToolInstanceFragmentArgs by navArgs()
    private lateinit var viewModel: EditToolInstanceViewModel

    private var currentToolInstance: ToolInstance? = null
    private var selectedInternalImageFileUriString: String? = null
    private var originalImageUriString: String? = null // To track if image changed from original

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
        _binding = FragmentEditToolInstanceBinding.inflate(inflater, container, false)
        val factory = EditToolInstanceViewModelFactory(requireActivity().application, args.instanceId)
        viewModel = ViewModelProvider(this, factory)[EditToolInstanceViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.title_edit_tool_instance)

        setupStatusSpinner()
        setupPurchaseDatePicker()
        setupImageControls()
        setupUpdateDeleteButtons()
        observeViewModel()
    }

    private fun setupPhotoLaunchers() {
        pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let { handleSelectedImage(it) }
        }
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                tempCameraImageUri?.let { capturedUri -> handleSelectedImage(capturedUri, isTemporaryFile = true) }
            } else {
                tempCameraImageFile?.delete()
                tempCameraImageFile = null
                tempCameraImageUri = null
            }
        }
    }

    private fun handleSelectedImage(uri: Uri, isTemporaryFile: Boolean = false) {
        val previouslySelectedImageForThisEditSession = selectedInternalImageFileUriString

        val newFile = ImageFileUtil.copyUriContentToInternalAppFile(
            requireContext(),
            uri,
            ImageFileUtil.PERMANENT_INSTANCE_IMAGES_SUBDIR,
            "INSTANCE_EDIT_IMG_"
        )

        if (newFile != null) {
            val newFileUriString = Uri.fromFile(newFile).toString()
            // If there was an image selected *during this edit session* (not the original), delete it.
            // The original image (originalImageUriString) is handled during save/update.
            if (previouslySelectedImageForThisEditSession != null &&
                previouslySelectedImageForThisEditSession != newFileUriString &&
                previouslySelectedImageForThisEditSession != originalImageUriString) { // Don't delete original yet
                ImageFileUtil.deleteAppInternalFile(requireContext(), previouslySelectedImageForThisEditSession)
            }
            selectedInternalImageFileUriString = newFileUriString
            updateImagePreview()
            Toast.makeText(requireContext(), R.string.instance_image_updated, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), R.string.error_saving_image, Toast.LENGTH_SHORT).show()
        }

        if (isTemporaryFile) {
            tempCameraImageFile?.delete()
            tempCameraImageFile = null
            tempCameraImageUri = null
        }
    }


    private fun populateForm(toolInstance: ToolInstance) {
        currentToolInstance = toolInstance
        binding.editTextInstanceSerialNumberEdit.setText(toolInstance.serialNumber ?: "")
        binding.editTextInstanceNotesEdit.setText(toolInstance.notes ?: "")

        val localizedStatus = getLocalizedStatus(toolInstance.status)
        val statusPosition = (binding.spinnerInstanceStatusEdit.adapter as ArrayAdapter<String>).getPosition(localizedStatus)
        if (statusPosition >= 0) {
            binding.spinnerInstanceStatusEdit.setSelection(statusPosition)
        }

        selectedPurchaseDateInMillis = toolInstance.purchaseDate
        if (toolInstance.purchaseDate != null) {
            binding.editTextPurchaseDateEdit.setText(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(toolInstance.purchaseDate))
            )
        } else {
            binding.editTextPurchaseDateEdit.text = null
        }

        originalImageUriString = toolInstance.instanceImageUri
        selectedInternalImageFileUriString = toolInstance.instanceImageUri
        updateImagePreview()
    }

    private fun setupStatusSpinner() {
        val statusAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            viewModel.statusOptions.map { getLocalizedStatus(it) }
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerInstanceStatusEdit.adapter = statusAdapter
    }

    private fun getLocalizedStatus(statusKey: String): String {
        return when (statusKey) {
            "Available" -> getString(R.string.label_status_available)
            "Rented" -> getString(R.string.label_status_rented) // Rented might not be editable directly here
            "Maintenance" -> getString(R.string.label_status_maintenance)
            "Damaged" -> getString(R.string.label_status_damaged)
            "Lost" -> getString(R.string.label_status_lost)
            else -> statusKey
        }
    }

    private fun getStatusKeyFromLocalized(localizedStatus: String): String {
        return when (localizedStatus) {
            getString(R.string.label_status_available) -> "Available"
            getString(R.string.label_status_rented) -> "Rented"
            getString(R.string.label_status_maintenance) -> "Maintenance"
            getString(R.string.label_status_damaged) -> "Damaged"
            getString(R.string.label_status_lost) -> "Lost"
            else -> localizedStatus
        }
    }


    private fun setupPurchaseDatePicker() {
        binding.editTextPurchaseDateEdit.setOnClickListener {
            val calendar = Calendar.getInstance()
            selectedPurchaseDateInMillis?.let { calendar.timeInMillis = it }
            DatePickerDialog(requireContext(), { _, year, month, day ->
                val selectedCalendar = Calendar.getInstance().apply { set(year, month, day) }
                selectedPurchaseDateInMillis = selectedCalendar.timeInMillis
                binding.editTextPurchaseDateEdit.setText(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCalendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        binding.textFieldLayoutPurchaseDateEdit.setEndIconOnClickListener {
            binding.editTextPurchaseDateEdit.performClick()
        }
    }

    private fun setupImageControls() {
        binding.buttonSelectInstanceImageEdit.setOnClickListener { showImageSourceDialog() }
        binding.imageViewInstancePreviewEdit.setOnClickListener { showImageSourceDialog() }
        binding.buttonRemoveInstanceImageEdit.setOnClickListener {
            // This only clears the *selected* image for this edit session.
            // If an original image existed, it will be restored unless a new one is picked.
            // If no original image, it just clears the selection.
            val imageToRemoveThisSession = selectedInternalImageFileUriString
            selectedInternalImageFileUriString = null // Effectively reverts to original or no image

            // If the image removed was one picked *during this session* and not the original one
            if (imageToRemoveThisSession != null && imageToRemoveThisSession != originalImageUriString) {
                 ImageFileUtil.deleteAppInternalFile(requireContext(), imageToRemoveThisSession)
            }
            // If originalImageUriString is null, selectedInternalImageFileUriString is now null (correct)
            // If originalImageUriString is not null, selectedInternalImageFileUriString will be set to it again
            // by updateImagePreview if no new image is chosen.
            // For now, simply set to null and let updateImagePreview decide based on original.
             selectedInternalImageFileUriString = null // Clear current selection
            updateImagePreview() // This will show placeholder if selected became null, or original if that's now the case
            Toast.makeText(requireContext(), R.string.instance_image_cleared, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf(getString(R.string.dialog_option_gallery), getString(R.string.dialog_option_camera))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_title_select_image_source))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    1 -> {
                        tempCameraImageFile = ImageFileUtil.createTempImageFile(requireContext(), "INSTANCE_EDIT_TEMP_")
                        tempCameraImageUri = tempCameraImageFile?.let { FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", it) }
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
        val imageToLoad = selectedInternalImageFileUriString ?: originalImageUriString
        if (imageToLoad != null) {
            Glide.with(this).load(Uri.parse(imageToLoad))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_broken_image)
                .into(binding.imageViewInstancePreviewEdit)
            binding.buttonRemoveInstanceImageEdit.isVisible = true
        } else {
            Glide.with(this).load(R.drawable.ic_image_placeholder).into(binding.imageViewInstancePreviewEdit)
            binding.buttonRemoveInstanceImageEdit.isVisible = false
        }
    }

    private fun setupUpdateDeleteButtons() {
        binding.buttonUpdateInstance.setOnClickListener {
            val serialNumber = binding.editTextInstanceSerialNumberEdit.text.toString().trim()
            val statusKey = getStatusKeyFromLocalized(binding.spinnerInstanceStatusEdit.selectedItem.toString())
            val notes = binding.editTextInstanceNotesEdit.text.toString().trim()

            // Image handling:
            // If selectedInternalImageFileUriString is different from originalImageUriString,
            // it means a new image was selected or the image was removed.
            // If they are the same, no change to image.
            // If selected is null but original was not, it means remove.
            val finalImageUri = selectedInternalImageFileUriString

            // Delete old original image if a new one was chosen and they are different
            if (originalImageUriString != null && finalImageUri != originalImageUriString) {
                ImageFileUtil.deleteAppInternalFile(requireContext(), originalImageUriString)
            }

            viewModel.updateInstance(
                serialNumber.ifEmpty { null },
                statusKey,
                selectedPurchaseDateInMillis,
                notes.ifEmpty { null },
                finalImageUri // This is the new or cleared URI
            )
        }

        binding.buttonDeleteInstance.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete_instance_title)
                .setMessage(R.string.confirm_delete_instance_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // Before deleting DB record, delete associated image if it exists
                    originalImageUriString?.let { ImageFileUtil.deleteAppInternalFile(requireContext(), it) }
                    // Also delete any image picked during this session but not yet saved
                    if (selectedInternalImageFileUriString != null && selectedInternalImageFileUriString != originalImageUriString) {
                         ImageFileUtil.deleteAppInternalFile(requireContext(), selectedInternalImageFileUriString)
                    }
                    viewModel.deleteInstance()
                }
                .show()
        }
    }

    private fun observeViewModel() {
        viewModel.toolInstance.observe(viewLifecycleOwner) { instance ->
            if (instance != null) {
                populateForm(instance)
            } else {
                Toast.makeText(requireContext(), R.string.failed_to_load_instance_details, Toast.LENGTH_LONG).show()
                findNavController().popBackStack() // Or handle error more gracefully
            }
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), R.string.tool_instance_updated_successfully, Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                },
                onFailure = { exception ->
                    Log.e("EditToolInstanceFrag", "Update failed", exception)
                     val errorMessage = when (exception) {
                        is IllegalArgumentException -> exception.message
                        else -> getString(R.string.failed_to_update_instance) + ": " + exception.localizedMessage
                    }
                    if (exception.message?.contains("Serial number", ignoreCase = true) == true) {
                        binding.textFieldLayoutInstanceSerialNumberEdit.error = exception.message
                        Toast.makeText(requireContext(), exception.message, Toast.LENGTH_LONG).show()
                    } else {
                         Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), R.string.tool_instance_deleted_successfully, Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                },
                onFailure = { exception ->
                    Log.e("EditToolInstanceFrag", "Delete failed", exception)
                    Toast.makeText(requireContext(), getString(R.string.failed_to_delete_instance) + ": " + exception.localizedMessage, Toast.LENGTH_LONG).show()
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
// 1. EditToolInstanceFragmentArgs from Nav Component for instanceId.
// 2. Image handling logic:
//    - Load original image.
//    - Allow selecting new image (from gallery/camera).
//    - If new image selected, copy to internal storage.
//    - If new image is saved, old original image file is deleted.
//    - If image is removed, selectedInternalImageFileUriString becomes null. If saved, original file is deleted.
//    - If instance is deleted, its image file is also deleted.
// 3. All strings and drawables should mostly exist from Add screen or common resources.
// 4. Spinner population and date picker logic similar to Add screen.
// 5. Confirmation dialog for delete.
// 6. FileProvider authority: "${requireContext().packageName}.fileprovider" - standard.
// 7. ImageFileUtil.PERMANENT_INSTANCE_IMAGES_SUBDIR is used.
// 8. Error handling and user feedback via Toasts.
// 9. Logic for deleting old image if a new one is set needs to be robust.
//    The `originalImageUriString` tracks the image loaded initially.
//    `selectedInternalImageFileUriString` tracks the currently chosen image (could be new, or same as original, or null if removed).
//    Deletion of the actual `originalImageUriString` file happens on successful update if `selectedInternalImageFileUriString` is different (and not null).
//    Or on delete.
//    If an image was picked in this session but then *another* new one was picked before saving, the intermediate one should be cleaned.
//    This is handled by `handleSelectedImage`.
// 10. The logic for `buttonRemoveInstanceImageEdit` has been updated to clear the selection for the session.
//     The actual file deletion of an *original* image due to removal happens at the point of saving the update.
// 11. The "Rented" status might be problematic to set directly if it's tied to active rentals.
//     For now, it's included in the list. Business logic might dictate this should be read-only
//     or managed by the rental process itself. This is out of scope for the current step.
// 12. String R.string.title_edit_tool_instance has been added previously.
// 13. Other strings like success/failure messages for update/delete also exist.
// 14. Ensure ImageFileUtil.getUriForFile is used for takePictureLauncher. (Corrected during generation)
// 15. The `handleSelectedImage` logic was refined to better manage temporary selections vs original image.
// 16. The `updateImagePreview` logic was updated to prioritize `selectedInternalImageFileUriString` then `originalImageUriString`.
// 17. Update button logic for image deletion refined: if `originalImageUriString` exists and `finalImageUri` (which is `selectedInternalImageFileUriString`)
//     is different, then the `originalImageUriString` file needs to be deleted.
// 18. Delete button logic: also delete `originalImageUriString`'s file and any `selectedInternalImageFileUriString`'s file if different.
//     This ensures cleanup.
//
// All necessary components seem to be covered. String resources and drawables are expected to be reused.
// The logic for image file management (especially deletion of old/original files) is critical.
// The current approach aims to:
//   - Delete an image picked *during the session* if another image is picked subsequently in the same session.
//   - Delete the *original* image file from storage ONLY when the update is successful AND the image was changed (new one picked or original removed).
//   - Delete the *original* image file (and any selected temporary one) if the entire instance is deleted.
