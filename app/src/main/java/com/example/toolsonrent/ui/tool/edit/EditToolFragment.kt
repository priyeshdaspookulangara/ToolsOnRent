package com.example.toolsonrent.ui.tool.edit

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.toolsonrent.R // For R.drawable.ic_launcher_background
import com.example.toolsonrent.database.entity.Tool
import com.example.toolsonrent.databinding.FragmentEditToolBinding // Generated
import com.google.android.material.dialog.MaterialAlertDialogBuilder // New import
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale // For String.format with locale

class EditToolFragment : Fragment() {

    private var _binding: FragmentEditToolBinding? = null
    private val binding get() = _binding!!

    private val args: EditToolFragmentArgs by navArgs()
    private lateinit var viewModel: EditToolViewModel

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
        setupDeleteButton()       // Updated to call showConfirmDeleteToolDialog
        observeUpdateResult()
        observeDeleteResult()     // New observer call
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

        if (tool.imageUri != null) {
            binding.imageViewToolPreviewEdit.setImageResource(R.drawable.ic_launcher_background)
        } else {
            binding.imageViewToolPreviewEdit.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    private fun setupSaveButton() {
        binding.buttonSaveChangesTool.setOnClickListener {
            val name = binding.editTextToolNameEdit.text.toString().trim()
            val description = binding.editTextToolDescriptionEdit.text.toString().trim()
            val priceStr = binding.editTextRentalPriceEdit.text.toString().trim()
            val isAvailable = binding.switchAvailabilityEdit.isChecked

            clearAllErrors()
            viewModel.updateTool(args.toolId, name, description, priceStr, isAvailable)
        }
    }

    private fun setupImageSelectionButton() {
        binding.buttonSelectImageEdit.setOnClickListener {
            Toast.makeText(requireContext(), "Image selection/update coming soon!", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupDeleteButton() {
        binding.buttonDeleteTool.setOnClickListener {
            showConfirmDeleteToolDialog() // Call confirmation dialog
        }
    }

    private fun showConfirmDeleteToolDialog() {
        val toolName = viewModel.tool.value?.name ?: "this tool"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Tool")
            .setMessage("Are you sure you want to delete '$toolName'? This action cannot be undone. Ensure this tool has no pending or past rentals associated with it, as this may prevent deletion or lead to data inconsistencies.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTool() // Call ViewModel to delete
            }
            .show()
    }

    private fun observeUpdateResult() {
        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Tool updated successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                },
                onFailure = { exception ->
                    Log.e("EditToolFragment", "Error updating tool", exception)
                    handleUpdateError(exception)
                }
            )
        }
    }

    private fun observeDeleteResult() {
        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Tool deleted successfully.", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                },
                onFailure = { exception ->
                    Log.e("EditToolFragment", "Error deleting tool", exception)
                    val errorMessage = if (exception.message?.contains("FOREIGN KEY constraint failed", ignoreCase = true) == true) {
                        "Cannot delete tool. It may have existing rental transactions. Please resolve these first or ensure all rentals are returned."
                    } else {
                        exception.message ?: "Unknown error deleting tool."
                    }
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Deletion Failed")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK", null)
                        .show()
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
        } else if (exception is IllegalStateException && message.contains("Invalid Tool ID", ignoreCase = true)) {
             Toast.makeText(requireContext(), "Error: Cannot update tool. Invalid ID.", Toast.LENGTH_LONG).show()
        }
    }

    private fun clearAllErrors(){
        binding.textFieldLayoutToolNameEdit.error = null
        binding.textFieldLayoutToolDescriptionEdit.error = null
        binding.textFieldLayoutRentalPriceEdit.error = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
