package com.example.toolsonrent.ui.settings

import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat // For colors
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider // For ViewModel
import com.example.toolsonrent.R
import com.example.toolsonrent.databinding.FragmentSettingsBinding // ViewBinding
import com.example.toolsonrent.ui.backuprestore.BackupRestoreViewModel // ViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder // Material Dialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var createDocumentLauncher: ActivityResultLauncher<String>
    private lateinit var openDocumentLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var backupRestoreViewModel: BackupRestoreViewModel

    private var tempBackupPassword: String? = null // To temporarily store password for launcher callback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createDocumentLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream")
        ) { uri: Uri? ->
            val currentPassword = tempBackupPassword
            if (uri != null && !currentPassword.isNullOrBlank()) {
                Log.d("SettingsFragment", "Backup file URI selected: $uri, proceeding with password.")
                backupRestoreViewModel.backupDatabase(uri, currentPassword)
            } else if (uri == null) {
                Log.d("SettingsFragment", "Backup cancelled: No location chosen by user.")
                Toast.makeText(requireContext(), "Backup cancelled: No location chosen.", Toast.LENGTH_SHORT).show()
            } else if (currentPassword.isNullOrBlank()){
                 Log.w("SettingsFragment", "Password was not set or was blank before file selection callback.")
                 Toast.makeText(requireContext(), "Password error. Please try backup again.", Toast.LENGTH_SHORT).show()
            }
            tempBackupPassword = null // Clear password after attempt, regardless of outcome
        }

        openDocumentLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                Log.d("SettingsFragment", "Restore file URI selected: $uri")
                Toast.makeText(requireContext(), "Restore file selected: $uri. Restore logic TBC.", Toast.LENGTH_LONG).show()
                // Example: backupRestoreViewModel.startRestore(uri, "password_from_dialog_later")
            } else {
                Log.d("SettingsFragment", "No restore file selected by user.")
                Toast.makeText(requireContext(), "Restore cancelled: No file chosen.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backupRestoreViewModel = ViewModelProvider(this)[BackupRestoreViewModel::class.java]

        binding.buttonBackupDatabase.setOnClickListener {
            showPasswordDialogForBackup()
        }

        binding.buttonRestoreDatabase.isEnabled = false // Restore is placeholder for now

        observeBackupStatus()
    }

    private fun showPasswordDialogForBackup() {
        val passwordInput = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Enter backup password"
            setPadding( (19 * resources.displayMetrics.density).toInt(), // ~19dp left/right
                        (12 * resources.displayMetrics.density).toInt(), // ~12dp top/bottom
                        (19 * resources.displayMetrics.density).toInt(),
                        (12 * resources.displayMetrics.density).toInt()
            )
        }

        // Using a FrameLayout to host EditText to apply padding correctly within dialog
        val container = FrameLayout(requireContext()).apply {
             val paddingHorizontal = (20 * resources.displayMetrics.density).toInt() // ~20dp
             val paddingVertical = (8 * resources.displayMetrics.density).toInt() // ~8dp
            setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
            addView(passwordInput)
        }


        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set Backup Password")
            .setMessage("Enter a strong password to encrypt your backup. This password will be required for restore.")
            .setView(container) // Set the container with EditText
            .setNegativeButton("Cancel") { dialog, _ ->
                tempBackupPassword = null // Ensure password is null if cancelled
                dialog.dismiss()
            }
            .setPositiveButton("Proceed to Save") { _, _ ->
                val password = passwordInput.text.toString()
                if (password.isNotBlank()) {
                    tempBackupPassword = password // Store password temporarily
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val suggestedFilename = "ToolsOnRent_Backup_${timeStamp}.encrypted"
                    createDocumentLauncher.launch(suggestedFilename)
                } else {
                    Toast.makeText(requireContext(), "Password cannot be empty.", Toast.LENGTH_SHORT).show()
                    tempBackupPassword = null // Ensure password is null if invalid
                }
            }
            .show()
    }

    private fun observeBackupStatus() {
        backupRestoreViewModel.backupStatus.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { message ->
                    binding.textViewBackupStatus.text = "Status: $message"
                    binding.textViewBackupStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_available_green))
                    // Avoid long toast for initial "Starting backup..." message
                    if (!message.startsWith("Starting backup...", ignoreCase = true)) {
                         Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                },
                onFailure = { exception ->
                    val errMessage = exception.message ?: "Unknown backup error"
                    binding.textViewBackupStatus.text = "Status: Backup Failed - $errMessage"
                    binding.textViewBackupStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_rented_red))
                    Log.e("SettingsFragment", "Backup failed", exception) // Log full exception
                    Toast.makeText(requireContext(), "Backup Failed: $errMessage", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
