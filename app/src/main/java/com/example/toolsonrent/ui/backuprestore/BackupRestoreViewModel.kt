package com.example.toolsonrent.ui.backuprestore

import android.app.Application
import android.content.ContentResolver // For getFileNameFromUri and openInputStream
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.utils.CryptoUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream // For fisSource

class BackupRestoreViewModel(application: Application) : AndroidViewModel(application) {

    private val _backupStatus = MutableLiveData<Result<String>>()
    val backupStatus: LiveData<Result<String>> = _backupStatus

    private val _restoreStatus = MutableLiveData<Result<String>>()
    val restoreStatus: LiveData<Result<String>> = _restoreStatus

    fun backupDatabase(destinationUri: Uri, password: String) {
        if (password.isBlank()) {
            _backupStatus.postValue(Result.failure(IllegalArgumentException("Password cannot be empty.")))
            return
        }
        viewModelScope.launch {
            _backupStatus.postValue(Result.success("Starting backup... This may take a moment."))
            withContext(Dispatchers.IO) {
                val context = getApplication<Application>().applicationContext
                val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
                if (!dbFile.exists()) {
                    Log.e("BackupRestoreVM", "Database file not found at path: ${dbFile.absolutePath}")
                    _backupStatus.postValue(Result.failure(Exception("Database file not found.")))
                    return@withContext
                }
                val tempEncryptedFile = File(context.cacheDir, "temp_tools_on_rent_backup.encrypted")
                var fis: FileInputStream? = null
                var fosTemp: FileOutputStream? = null
                try {
                    val salt = CryptoUtil.generateSalt()
                    val secretKey = CryptoUtil.getKeyFromPassword(password, salt)
                    if (secretKey == null) {
                        _backupStatus.postValue(Result.failure(Exception("Failed to generate encryption key. Password might be too weak or invalid.")))
                        return@withContext
                    }
                    val ivParameterSpec = CryptoUtil.generateIv()
                    val ivBytes = CryptoUtil.getIvBytes(ivParameterSpec)
                    fis = FileInputStream(dbFile)
                    fosTemp = FileOutputStream(tempEncryptedFile)
                    fosTemp.write(salt)
                    fosTemp.write(ivBytes)
                    Log.d("BackupRestoreVM", "Starting encryption process...")
                    val encryptionSuccess = CryptoUtil.encrypt(fis, fosTemp, secretKey, ivParameterSpec)
                    fis.close()
                    fosTemp.close()
                    if (!encryptionSuccess) {
                        _backupStatus.postValue(Result.failure(Exception("Encryption process failed.")))
                        return@withContext
                    }
                    Log.d("BackupRestoreVM", "Encryption successful. Temp file size: ${tempEncryptedFile.length()} bytes.")
                    context.contentResolver.openOutputStream(destinationUri)?.use { destinationOs ->
                        FileInputStream(tempEncryptedFile).use { tempFis ->
                            tempFis.copyTo(destinationOs)
                        }
                        Log.d("BackupRestoreVM", "Successfully copied temp file to destination URI.")
                    } ?: run {
                        Log.e("BackupRestoreVM", "Failed to open output stream to destination URI: $destinationUri")
                        _backupStatus.postValue(Result.failure(Exception("Failed to open output stream to destination.")))
                        return@withContext
                    }
                    val destinationFileName = getFileNameFromUri(context, destinationUri) ?: "backup file"
                    _backupStatus.postValue(Result.success("Backup successful! Saved to '$destinationFileName'"))
                } catch (e: Exception) {
                    Log.e("BackupRestoreVM", "Backup process failed.", e)
                    _backupStatus.postValue(Result.failure(Exception("Backup failed: ${e.message}", e)))
                } finally {
                    try { fis?.close(); fosTemp?.close() }
                    catch (e: IOException) { Log.e("BackupRestoreVM", "Error closing streams in finally block.", e) }
                    if (tempEncryptedFile.exists()) {
                        tempEncryptedFile.delete()
                        Log.d("BackupRestoreVM", "Temporary backup file deleted.")
                    }
                }
            }
        }
    }

    fun restoreDatabase(sourceUri: Uri, password: String) {
        if (password.isBlank()) {
            _restoreStatus.postValue(Result.failure(IllegalArgumentException("Password cannot be empty for restore.")))
            return
        }

        viewModelScope.launch {
            _restoreStatus.postValue(Result.success("Starting restore... Please wait. The app may restart if successful."))

            withContext(Dispatchers.IO) {
                val context = getApplication<Application>().applicationContext
                val dbName = AppDatabase.DATABASE_NAME
                val dbPath = context.getDatabasePath(dbName)

                val tempEncryptedBackupFile = File(context.cacheDir, "temp_encrypted_restore_import.db")
                val tempDecryptedDbFile = File(context.cacheDir, "temp_decrypted_restore_final.db")

                var fisSource: InputStream? = null
                var fosTempEncrypted: FileOutputStream? = null
                var fisTempEncrypted: FileInputStream? = null
                var fosTempDecrypted: FileOutputStream? = null

                try {
                    // 1. Copy selected backup file (from sourceUri) to a temporary local file (tempEncryptedBackupFile)
                    fisSource = context.contentResolver.openInputStream(sourceUri)
                    if (fisSource == null) {
                        _restoreStatus.postValue(Result.failure(Exception("Failed to open selected backup file.")))
                        return@withContext
                    }
                    fosTempEncrypted = FileOutputStream(tempEncryptedBackupFile)
                    fisSource.copyTo(fosTempEncrypted)
                    Log.d("BackupRestoreVM", "Copied source backup to temp encrypted file: ${tempEncryptedBackupFile.absolutePath}")

                    // 2. Read Salt and IV from the temporary encrypted file
                    fisTempEncrypted = FileInputStream(tempEncryptedBackupFile)
                    val salt = ByteArray(CryptoUtil.SALT_SIZE_BYTES)
                    val ivBytes = ByteArray(CryptoUtil.IV_SIZE_BYTES)

                    val saltRead = fisTempEncrypted.read(salt)
                    val ivRead = fisTempEncrypted.read(ivBytes)

                    if (saltRead != CryptoUtil.SALT_SIZE_BYTES || ivRead != CryptoUtil.IV_SIZE_BYTES) {
                        throw Exception("Invalid backup file format: Could not read complete salt/IV. Salt read: $saltRead, IV read: $ivRead")
                    }
                    Log.d("BackupRestoreVM", "Salt and IV read from temp encrypted file.")

                    val secretKey = CryptoUtil.getKeyFromPassword(password, salt)
                    if (secretKey == null) {
                        throw Exception("Failed to generate key for decryption. Invalid password or corrupted salt.")
                    }
                    val ivParameterSpec = CryptoUtil.getIvParameterSpec(ivBytes)

                    // 3. Decrypt the rest of the temp file into a new temp decrypted DB file
                    fosTempDecrypted = FileOutputStream(tempDecryptedDbFile)
                    Log.d("BackupRestoreVM", "Starting decryption process...")
                    val decryptionSuccess = CryptoUtil.decrypt(fisTempEncrypted, fosTempDecrypted, secretKey, ivParameterSpec)

                    if (!decryptionSuccess) {
                        throw Exception("Decryption failed. Invalid password or corrupted backup file.")
                    }
                    Log.d("BackupRestoreVM", "Decryption successful. Temp decrypted file size: ${tempDecryptedDbFile.length()} bytes.")

                    // 4. Critical Step: Close current DB, replace files, and re-open (implicitly on next access)
                    Log.d("BackupRestoreVM", "Closing current database instance.")
                    AppDatabase.closeInstance()

                    Log.d("BackupRestoreVM", "Replacing database files...")
                    val dbShm = File(dbPath.path + "-shm")
                    val dbWal = File(dbPath.path + "-wal")

                    if (dbPath.exists()) { if (!dbPath.delete()) throw IOException("Could not delete old main database file.") }
                    if (dbShm.exists()) { if (!dbShm.delete()) throw IOException("Could not delete old SHM file.") }
                    if (dbWal.exists()) { if (!dbWal.delete()) throw IOException("Could not delete old WAL file.") }

                    tempDecryptedDbFile.copyTo(dbPath, true)
                    Log.i("BackupRestoreVM", "Database file replaced with decrypted backup.")

                    _restoreStatus.postValue(Result.success("Database restored successfully! Please restart the app for changes to fully apply."))

                } catch (e: Exception) {
                    Log.e("BackupRestoreVM", "Restore process failed.", e)
                    _restoreStatus.postValue(Result.failure(Exception("Restore failed: ${e.message}", e)))
                } finally {
                    try { fisSource?.close() } catch (e: IOException) { Log.e("BackupRestoreVM", "Error closing fisSource: ${e.message}")}
                    try { fosTempEncrypted?.close()} catch (e: IOException) { Log.e("BackupRestoreVM", "Error closing fosTempEncrypted: ${e.message}")}
                    try { fisTempEncrypted?.close()} catch (e: IOException) { Log.e("BackupRestoreVM", "Error closing fisTempEncrypted: ${e.message}")}
                    try { fosTempDecrypted?.close()} catch (e: IOException) { Log.e("BackupRestoreVM", "Error closing fosTempDecrypted: ${e.message}")}

                    if(tempEncryptedBackupFile.exists()) tempEncryptedBackupFile.delete()
                    if(tempDecryptedDbFile.exists()) tempDecryptedDbFile.delete()
                    Log.d("BackupRestoreVM", "Temporary restore files deleted.")
                }
            }
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? { /* ... (existing implementation) ... */ return null }
}
