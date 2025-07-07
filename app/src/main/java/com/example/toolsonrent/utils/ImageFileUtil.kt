package com.example.toolsonrent.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Utility object for handling image file operations such as creating,
 * saving, and deleting image files within the app's storage.
 */
object ImageFileUtil {

    private const val DEFAULT_TEMP_IMAGE_CACHE_SUBDIR = "images"
    private const val TEMP_FILE_PREFIX = "TEMP_CAPTURE_"
    private const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"
    const val PERMANENT_TOOL_IMAGES_SUBDIR = "tool_images"
    const val PERMANENT_INSTANCE_IMAGES_SUBDIR = "instance_images" // Added

    fun createTempImageFile(context: Context, subDirName: String = DEFAULT_TEMP_IMAGE_CACHE_SUBDIR): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val imageFileName = "${TEMP_FILE_PREFIX}${timeStamp}_${UUID.randomUUID()}"
            val storageDir = File(context.cacheDir, subDirName)
            if (!storageDir.exists()) {
                if (!storageDir.mkdirs()) {
                    Log.e("ImageFileUtil", "Failed to create temp directory: ${storageDir.absolutePath}")
                    return null
                }
            }
            val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
            Log.d("ImageFileUtil", "Temporary image file created: ${imageFile.absolutePath}")
            imageFile
        } catch (ex: IOException) {
            Log.e("ImageFileUtil", "Error creating temporary image file", ex)
            null
        }
    }

    fun getUriForFile(context: Context, file: File): Uri? {
        return try {
            val authority = "${context.packageName}${FILE_PROVIDER_AUTHORITY_SUFFIX}"
            FileProvider.getUriForFile(context, authority, file)
        } catch (e: IllegalArgumentException) {
            Log.e("ImageFileUtil", "Error getting URI for file. Check FileProvider authorities and paths. File: ${file.absolutePath}", e)
            null
        } catch (e: Exception) {
            Log.e("ImageFileUtil", "Unexpected error getting URI for file: ${file.absolutePath}", e)
            null
        }
    }

    /**
     * Retrieves the display name of a file from its content URI.
     * Includes fallbacks and sanitization (replaces invalid characters, limits length).
     * @param context The context.
     * @param uri The content URI of the file.
     * @return The display name, or a generated name if it cannot be determined, or null on critical error.
     */
    fun getDisplayFileNameFromUri(context: Context, uri: Uri): String? {
        var fileName: String? = null
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) { // Catch SecurityException or others that might prevent query
                Log.e("ImageFileUtil", "Error querying content resolver for DISPLAY_NAME: $uri", e)
            }
            // Fallback for content URIs if DISPLAY_NAME is not available or query failed
            if (fileName == null) {
                fileName = uri.lastPathSegment ?: "unknown_image_${System.currentTimeMillis()}"
                Log.w("ImageFileUtil", "DISPLAY_NAME not found or query failed for content URI: $uri. Using fallback: $fileName")
            }
        } else if (uri.scheme == ContentResolver.SCHEME_FILE) {
            fileName = uri.lastPathSegment
        }

        // Sanitize and limit length
        return fileName?.replace(Regex("[^a-zA-Z0-9._-]"), "_")?.take(200)
    }

    fun copyUriContentToInternalAppFile(
        context: Context,
        sourceUri: Uri,
        internalDirName: String,
        fileNamePrefix: String
    ): File? {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(sourceUri)
            if (inputStream == null) {
                Log.e("ImageFileUtil", "Failed to open input stream for URI: $sourceUri")
                return null
            }
            val targetDirectory = File(context.filesDir, internalDirName)
            if (!targetDirectory.exists()) {
                if (!targetDirectory.mkdirs()) {
                    Log.e("ImageFileUtil", "Failed to create target directory: ${targetDirectory.absolutePath}")
                    return null
                }
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val originalFileName = getDisplayFileNameFromUri(context, sourceUri) // Now uses the robust version
            val extension = originalFileName?.substringAfterLast('.', "")?.lowercase(Locale.ROOT) ?: "jpg"

            val uniqueFileName = "${fileNamePrefix}${timeStamp}_${UUID.randomUUID()}.$extension"
            val outputFile = File(targetDirectory, uniqueFileName)
            outputStream = FileOutputStream(outputFile)
            inputStream.copyTo(outputStream)
            Log.d("ImageFileUtil", "File copied successfully to: ${outputFile.absolutePath}")
            return outputFile
        } catch (e: Exception) {
            Log.e("ImageFileUtil", "Error copying file from URI $sourceUri to internal storage", e)
            return null
        } finally {
            try {
                inputStream?.close()
                outputStream?.close()
            } catch (e: IOException) {
                Log.e("ImageFileUtil", "Error closing streams", e)
            }
        }
    }

    fun deleteAppInternalFile(context: Context, fileUriString: String?): Boolean {
        // ... (existing implementation from previous step) ...
        if (fileUriString.isNullOrBlank()) {
            Log.w("ImageFileUtil", "Attempted to delete a null or blank file path string.")
            return false
        }
        val fileUri: Uri = try { Uri.parse(fileUriString) } catch (e: Exception) {
            Log.e("ImageFileUtil", "Invalid URI string for deletion: $fileUriString", e); return false
        }
        if (fileUri.scheme != "file") {
            Log.w("ImageFileUtil", "Cannot delete non-file URI: $fileUriString. Scheme was ${fileUri.scheme}"); return false
        }
        val filePath = fileUri.path
        if (filePath.isNullOrBlank()) {
            Log.w("ImageFileUtil", "Could not extract valid path from URI: $fileUriString"); return false
        }
        val fileToDelete = File(filePath)
        val appFilesDir = context.filesDir.absolutePath
        val appCacheDir = context.cacheDir.absolutePath
        if (!fileToDelete.absolutePath.startsWith(appFilesDir) && !fileToDelete.absolutePath.startsWith(appCacheDir)) {
            Log.e("ImageFileUtil", "Attempt to delete file outside app's specific directories: ${fileToDelete.absolutePath}. Deletion aborted."); return false
        }
        val parentDirName = fileToDelete.parentFile?.name
        // Added PERMANENT_INSTANCE_IMAGES_SUBDIR to knownImageDirs
        val knownImageDirs = listOf(PERMANENT_TOOL_IMAGES_SUBDIR, PERMANENT_INSTANCE_IMAGES_SUBDIR, DEFAULT_TEMP_IMAGE_CACHE_SUBDIR)
        if (parentDirName == null || parentDirName !in knownImageDirs) {
            Log.e("ImageFileUtil", "Attempt to delete file outside designated image subdirectories. Path: ${fileToDelete.absolutePath}. Deletion aborted."); return false
        }
        return try {
            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    Log.i("ImageFileUtil", "Successfully deleted internal file: ${fileToDelete.absolutePath}"); true
                } else {
                    Log.w("ImageFileUtil", "Failed to delete internal file: ${fileToDelete.absolutePath}"); false
                }
            } else {
                Log.w("ImageFileUtil", "File to delete does not exist: ${fileToDelete.absolutePath}"); true
            }
        } catch (e: SecurityException) {
            Log.e("ImageFileUtil", "Security error deleting file ${fileToDelete.absolutePath}", e); false
        } catch (e: Exception) {
            Log.e("ImageFileUtil", "Error deleting file ${fileToDelete.absolutePath}", e); false
        }
    }
}
