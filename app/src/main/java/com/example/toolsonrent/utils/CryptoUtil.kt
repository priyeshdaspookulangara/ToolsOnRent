package com.example.toolsonrent.utils

import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utility object for performing encryption and decryption operations.
 * Uses AES/CBC/PKCS5Padding for symmetric encryption.
 * Keys are derived from passwords using PBKDF2WithHmacSHA256.
 */
object CryptoUtil {

    private const val TAG = "CryptoUtil"
    private const val ALGORITHM = "AES" // Symmetric encryption algorithm
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding" // Cipher transformation string
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256" // Key derivation algorithm
    private const val ITERATION_COUNT = 65536 // Recommended iteration count for PBKDF2
    private const val KEY_LENGTH = 256 // AES key length in bits (AES-256)
    private const val SALT_SIZE_BYTES = 16 // Salt size in bytes (128 bits)
    private const val IV_SIZE_BYTES = 16   // IV size in bytes for AES/CBC (128 bits, matches block size)

    /**
     * Generates a cryptographically strong random salt.
     *
     * @return A byte array containing the generated salt.
     */
    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_SIZE_BYTES)
        random.nextBytes(salt)
        Log.d(TAG, "Generated salt of size: ${salt.size} bytes.")
        return salt
    }

    /**
     * Derives a SecretKey from a user-provided password and a salt using PBKDF2.
     *
     * @param password The password to derive the key from.
     * @param salt The salt to use for key derivation.
     * @return A SecretKey suitable for AES encryption, or null if key derivation fails.
     */
    fun getKeyFromPassword(password: String, salt: ByteArray): SecretKey? {
        return try {
            val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
            // PBEKeySpec(char[] password, byte[] salt, int iterationCount, int keyLength)
            val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
            val secret = factory.generateSecret(spec) // This is a SecretKey (PBEKey)
            // Convert the generic SecretKey into a SecretKeySpec for a specific algorithm (AES)
            SecretKeySpec(secret.encoded, ALGORITHM)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating SecretKey from password", e)
            null
        }
    }

    /**
     * Generates a random Initialization Vector (IV) suitable for AES/CBC.
     *
     * @return An IvParameterSpec containing the generated IV.
     */
    fun generateIv(): IvParameterSpec {
        val random = SecureRandom()
        val iv = ByteArray(IV_SIZE_BYTES)
        random.nextBytes(iv)
        Log.d(TAG, "Generated IV of size: ${iv.size} bytes.")
        return IvParameterSpec(iv)
    }

    /**
     * Retrieves the raw byte array from an IvParameterSpec.
     *
     * @param ivParameterSpec The IvParameterSpec.
     * @return The byte array of the IV.
     */
    fun getIvBytes(ivParameterSpec: IvParameterSpec): ByteArray {
        return ivParameterSpec.iv
    }

    /**
     * Creates an IvParameterSpec from a given byte array.
     *
     * @param ivBytes The byte array to use for the IV.
     * @return An IvParameterSpec.
     * @throws IllegalArgumentException if ivBytes length is incorrect.
     */
    fun getIvParameterSpec(ivBytes: ByteArray): IvParameterSpec {
        if (ivBytes.size != IV_SIZE_BYTES) {
            throw IllegalArgumentException("IV length must be $IV_SIZE_BYTES bytes for AES/CBC.")
        }
        return IvParameterSpec(ivBytes)
    }

    /**
     * Encrypts data from an InputStream and writes the encrypted data to an OutputStream.
     * The salt and IV must be handled (e.g., stored/written) by the caller separately,
     * typically by prepending them to the encrypted data stream.
     *
     * @param inputStream The stream to read plaintext data from.
     * @param outputStream The stream to write encrypted data to.
     * @param secretKey The SecretKey to use for encryption.
     * @param iv The IvParameterSpec (Initialization Vector) to use.
     * @return True if encryption was successful, false otherwise.
     */
    fun encrypt(inputStream: InputStream, outputStream: OutputStream, secretKey: SecretKey, iv: IvParameterSpec): Boolean {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)

            val buffer = ByteArray(1024 * 4) // 4KB buffer for stream processing
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                val encryptedBytes = cipher.update(buffer, 0, bytesRead)
                encryptedBytes?.let { outputStream.write(it) }
            }
            val finalBytes = cipher.doFinal() // Process any remaining buffered data
            finalBytes?.let { outputStream.write(it) }
            Log.d(TAG, "Encryption completed successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            false
        }
        // Streams (inputStream, outputStream) are managed and closed by the caller (ViewModel).
    }

    /**
     * Decrypts data from an InputStream and writes the decrypted data to an OutputStream.
     * The caller is responsible for providing the correct salt (to regenerate the key)
     * and IV (typically read from the beginning of the encrypted data stream).
     *
     * @param inputStream The stream to read encrypted data from.
     * @param outputStream The stream to write decrypted plaintext data to.
     * @param secretKey The SecretKey (derived from password and stored salt) to use for decryption.
     * @param iv The IvParameterSpec (read from the encrypted data stream) to use.
     * @return True if decryption was successful, false otherwise (e.g., wrong key, corrupted data).
     */
    fun decrypt(inputStream: InputStream, outputStream: OutputStream, secretKey: SecretKey, iv: IvParameterSpec): Boolean {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)

            val buffer = ByteArray(1024 * 4) // 4KB buffer
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                val decryptedBytes = cipher.update(buffer, 0, bytesRead)
                decryptedBytes?.let { outputStream.write(it) }
            }
            val finalBytes = cipher.doFinal() // Process any remaining buffered data
            finalBytes?.let { outputStream.write(it) }
            Log.d(TAG, "Decryption completed successfully.")
            true
        } catch (e: Exception) {
            // Common causes: incorrect password (leading to wrong key), incorrect IV,
            // or data corruption.
            Log.e(TAG, "Decryption failed. Possible wrong key/IV or corrupted data.", e)
            false
        }
        // Streams (inputStream, outputStream) are managed and closed by the caller (ViewModel).
    }
}
