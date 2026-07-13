package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.File

/**
 * Production [IFileStorage] implementation that reads and writes files using
 * [androidx.security.crypto.EncryptedFile] (AES256_GCM_HKDF_4KB).
 *
 * All file I/O is backed by the Android Keystore. This class must never be
 * used in unit tests running on the JVM without a real Keystore.
 *
 * @param context Application context used by [EncryptedFile.Builder].
 */
class EncryptedFileStorage(private val context: Context) : IFileStorage {

    override fun write(file: File, text: String) {
        try {
            if (file.exists()) file.delete()
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val encryptedFile = EncryptedFile.Builder(
                file,
                context,
                masterKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            encryptedFile.openFileOutput().use { it.write(text.toByteArray(Charsets.UTF_8)) }
        } catch (e: Exception) {
            Log.e("EncryptedFileStorage", "Failed to write encrypted file: ${file.name}", e)
            throw EncryptedFileWriteException("Failed to persist data to ${file.name}", e)
        }
    }

    override fun read(file: File): String? {
        if (!file.exists()) return null
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val encryptedFile = EncryptedFile.Builder(
                file,
                context,
                masterKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            encryptedFile.openFileInput().use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (e: Exception) {
            Log.e("EncryptedFileStorage", "Failed to read encrypted file: ${file.name}", e)
            throw EncryptedFileReadException(
                "Failed to decrypt ${file.name}. The file may be corrupted or tampered.",
                e
            )
        }
    }
}

class EncryptedFileWriteException(message: String, cause: Throwable) : RuntimeException(message, cause)
class EncryptedFileReadException(message: String, cause: Throwable) : RuntimeException(message, cause)

