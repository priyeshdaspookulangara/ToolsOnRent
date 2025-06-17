package com.example.toolsonrent.ui.customer.add

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.toolsonrent.databinding.FragmentAddCustomerBinding // Generated

class AddCustomerFragment : Fragment() {

    private var _binding: FragmentAddCustomerBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AddCustomerViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCustomerBinding.inflate(inflater, container, false)
        // Initialize ViewModel here, right after binding is available and before returning the view
        viewModel = ViewModelProvider(this)[AddCustomerViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSaveCustomer.setOnClickListener {
            val name = binding.editTextCustomerName.text.toString().trim()
            val phone = binding.editTextCustomerPhone.text.toString().trim()
            val email = binding.editTextCustomerEmail.text.toString().trim()
            val address = binding.editTextCustomerAddress.text.toString().trim()

            // Clear previous errors before calling ViewModel
            binding.textFieldLayoutCustomerName.error = null
            binding.textFieldLayoutCustomerPhone.error = null
            // Optionally clear email/address errors if they were to have any
            // binding.textFieldLayoutCustomerEmail.error = null
            // binding.textFieldLayoutCustomerAddress.error = null


            viewModel.saveCustomer(name, phone, email, address)
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Customer saved successfully!", Toast.LENGTH_SHORT).show()
                    clearForm()
                    // Optionally navigate back or to customer list
                    // For now, let's assume we stay on the form or it's part of a larger flow
                    // findNavController().popBackStack()
                },
                onFailure = { exception ->
                    Log.e("AddCustomerFragment", "Error saving customer", exception)
                    // More specific error handling based on exception type
                    if (exception is IllegalArgumentException && exception.message?.contains("Name and Phone") == true) {
                        // This message comes from the ViewModel validation
                        Toast.makeText(requireContext(), exception.message, Toast.LENGTH_LONG).show()
                        // Highlight specific fields if validation failed
                        if (binding.editTextCustomerName.text.isNullOrBlank()) {
                            binding.textFieldLayoutCustomerName.error = "Name cannot be empty"
                        }
                        if (binding.editTextCustomerPhone.text.isNullOrBlank()) {
                            binding.textFieldLayoutCustomerPhone.error = "Phone cannot be empty"
                        }
                    } else {
                        // Generic error for other issues (e.g., database problems)
                        Toast.makeText(requireContext(), "Error saving customer: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    private fun clearForm() {
        binding.editTextCustomerName.text?.clear()
        binding.editTextCustomerPhone.text?.clear()
        binding.editTextCustomerEmail.text?.clear()
        binding.editTextCustomerAddress.text?.clear()

        // Also clear any errors on the TextInputLayouts
        binding.textFieldLayoutCustomerName.error = null
        binding.textFieldLayoutCustomerPhone.error = null
        binding.textFieldLayoutCustomerEmail.error = null
        binding.textFieldLayoutCustomerAddress.error = null

        // Optionally, request focus on the first field
        // binding.editTextCustomerName.requestFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
