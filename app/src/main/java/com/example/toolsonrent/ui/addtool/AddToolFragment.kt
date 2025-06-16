package com.example.toolsonrent.ui.addtool

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.toolsonrent.database.entity.Tool
import com.example.toolsonrent.databinding.FragmentAddToolBinding // This will be generated

class AddToolFragment : Fragment() {

    private var _binding: FragmentAddToolBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AddToolViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddToolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize ViewModel using the fragment as the ViewModelStoreOwner
        viewModel = ViewModelProvider(this).get(AddToolViewModel::class.java)

        binding.buttonSaveTool.setOnClickListener {
            val toolName = binding.editTextToolName.text.toString().trim()
            val description = binding.editTextToolDescription.text.toString().trim()
            val rentalPriceStr = binding.editTextRentalPrice.text.toString().trim()
            val isAvailable = binding.switchAvailability.isChecked

            var isValid = true

            if (toolName.isEmpty()) {
                binding.textFieldLayoutToolName.error = "Tool name cannot be empty"
                isValid = false
            } else {
                binding.textFieldLayoutToolName.error = null
            }

            val rentalPriceDouble = rentalPriceStr.toDoubleOrNull()
            if (rentalPriceDouble == null || rentalPriceDouble <= 0) {
                binding.textFieldLayoutRentalPrice.error = "Enter a valid positive price"
                isValid = false
            } else {
                binding.textFieldLayoutRentalPrice.error = null
            }

            if (isValid) {
                val tool = Tool(
                    name = toolName,
                    description = description.ifEmpty { null },
                    rentalPrice = rentalPriceDouble!!, // Already checked not null
                    isAvailable = isAvailable,
                    imageUri = null // Placeholder for now, will be handled by image selection logic
                )
                viewModel.saveTool(tool)
            }
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Tool saved successfully!", Toast.LENGTH_SHORT).show()
                    // Clear the form
                    binding.editTextToolName.text?.clear()
                    binding.editTextToolDescription.text?.clear()
                    binding.editTextRentalPrice.text?.clear()
                    binding.switchAvailability.isChecked = true
                    binding.imageViewToolPreview.setImageResource(android.R.drawable.ic_menu_gallery) // Reset placeholder
                    binding.textFieldLayoutToolName.error = null // Clear any previous errors
                    binding.textFieldLayoutToolDescription.error = null // Clear description error too
                    binding.textFieldLayoutRentalPrice.error = null

                    // Optionally, navigate back or to a different screen
                    // findNavController().popBackStack()
                },
                onFailure = { exception ->
                    Toast.makeText(requireContext(), "Error saving tool: ${exception.message}", Toast.LENGTH_LONG).show()
                    Log.e("AddToolFragment", "Error saving tool", exception)
                }
            )
        }

        binding.buttonSelectImage.setOnClickListener {
            Toast.makeText(requireContext(), "Image selection will be implemented here.", Toast.LENGTH_LONG).show()
            // Later, this will launch an image picker intent.
            // For now, we can also simulate selecting an image for testing purposes if desired,
            // by setting a drawable to imageViewToolPreview, e.g.:
            // binding.imageViewToolPreview.setImageResource(android.R.drawable.ic_menu_camera)
        }

        binding.imageViewToolPreview.setOnClickListener {
            Toast.makeText(requireContext(), "Image preview clicked. Selection will be implemented via button.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
