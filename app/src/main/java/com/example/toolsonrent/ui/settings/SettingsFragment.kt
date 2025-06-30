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
    private var tempRestorePassword: String? = null // To temporarily store password for restore launcher callback

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
            val currentPassword = tempRestorePassword // Use the password stored from the dialog
            if (uri != null && !currentPassword.isNullOrBlank()) {
                Log.d("SettingsFragment", "Restore file URI selected: $uri")
                backupRestoreViewModel.restoreDatabase(uri, currentPassword)
            } else if (uri == null) {
                Log.d("SettingsFragment", "No restore file selected by user.")
                Toast.makeText(requireContext(), "Restore cancelled: No file selected.", Toast.LENGTH_SHORT).show()
            } else { // Password was blank or null
                Log.w("SettingsFragment", "Password was not set before file selection for restore.")
                Toast.makeText(requireContext(), "Password error for restore. Please try again.", Toast.LENGTH_SHORT).show()
            }
            tempRestorePassword = null // Clear password after attempt, regardless of outcome
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

        binding.buttonRestoreDatabase.isEnabled = true // Explicitly enable, though it should be by default from XML
        binding.buttonRestoreDatabase.setOnClickListener {
            showConfirmRestoreDialog()
        }

        observeBackupStatus()
        observeRestoreStatus() // New observer call
    }

    private fun showConfirmRestoreDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Restore")
            .setMessage("Restoring from a backup will overwrite ALL current data in the app. This action cannot be undone and the app may restart after restore. Are you sure you want to proceed?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Proceed to Restore") { _, _ ->
                showPasswordDialogForRestore()
            }
            .show()
    }

    private fun showPasswordDialogForRestore() {
        val passwordInput = EditText(requireContext())
        passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        passwordInput.hint = "Enter backup password"
        // Create a FrameLayout to add padding around EditText for the dialog
        val frameLayout = FrameLayout(requireContext())
        val padding = (16 * resources.displayMetrics.density).toInt()
        frameLayout.setPadding(padding, padding / 2, padding, padding / 2)
        frameLayout.addView(passwordInput)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Enter Backup Password")
            .setMessage("Enter the password used when this backup was created.")
            .setView(frameLayout)
            .setNegativeButton("Cancel") { _,_ -> tempRestorePassword = null } // Clear on cancel
            .setPositiveButton("Select Backup File") { _, _ ->
                val password = passwordInput.text.toString()
                if (password.isNotBlank()) {
                    tempRestorePassword = password
                    // Launch file picker, filtering for octet-stream or all files
                    openDocumentLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                } else {
                    Toast.makeText(requireContext(), "Password cannot be empty.", Toast.LENGTH_SHORT).show()
                    tempRestorePassword = null
                }
            }
            .show()
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

    private fun observeRestoreStatus() {
        backupRestoreViewModel.restoreStatus.observe(viewLifecycleOwner) { result ->
            // Can reuse textViewBackupStatus or have a dedicated one.
            // For now, let's use the same one for simplicity.
            val statusTextView = binding.textViewBackupStatus

            result.fold(
                onSuccess = { message ->
                    statusTextView.text = "Status: $message"
                    statusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_available_green))
                    // Show a non-cancelable dialog for success as it's a critical event
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Restore Successful")
                        .setMessage(message) // ViewModel should provide message like "App will restart" or "Data restored."
                        .setPositiveButton("OK") { _, _ ->
                            // Optionally, could try to programmatically restart or just inform user.
                            // For now, just an OK button.
                        }
                        .setCancelable(false)
                        .show()
                },
                onFailure = { exception ->
                    val errMessage = exception.message ?: "Unknown error during restore."
                    statusTextView.text = "Status: Restore Failed"
                    statusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_rented_red))
                    Log.e("SettingsFragment", "Restore failed: $errMessage", exception)
                    // Show a non-cancelable dialog for critical failure
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Restore Failed")
                        .setMessage(errMessage)
                        .setPositiveButton("OK", null)
                        .setCancelable(false)
                        .show()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
