package com.example.toolsonrent.ui.rentals

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.toolsonrent.R // For navigation action IDs
import com.example.toolsonrent.databinding.FragmentRentalsBinding // Generated ViewBinding class

class RentalsFragment : Fragment() {

    private var _binding: FragmentRentalsBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRentalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // The title "Rental Management" is set in fragment_rentals.xml's TextView.
        // If dynamic title setting based on, e.g., user role was needed:
        // binding.textViewRentalsTitle.text = "Manage Your Rentals"

        binding.buttonGoToStartRental.setOnClickListener {
            try {
                // This action ID (action_nav_rentals_to_startRentalFragment)
                // will need to be defined in nav_graph.xml in the next step.
                findNavController().navigate(R.id.action_nav_rentals_to_startRentalFragment)
            } catch (e: IllegalArgumentException) {
                Log.e("RentalsFragment", "Navigation to StartRentalFragment failed. Action ID likely missing in nav_graph.", e)
                Toast.makeText(context, "Error: Start New Rental action not yet configured.", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonGoToActiveRentals.setOnClickListener {
            try {
                // This action ID (action_nav_rentals_to_activeRentalsFragment)
                // will need to be defined in nav_graph.xml in the next step.
                findNavController().navigate(R.id.action_nav_rentals_to_activeRentalsFragment)
            } catch (e: IllegalArgumentException) {
                Log.e("RentalsFragment", "Navigation to ActiveRentalsFragment failed. Action ID likely missing in nav_graph.", e)
                Toast.makeText(context, "Error: View Active Rentals action not yet configured.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Important to prevent memory leaks with ViewBinding
    }
}
