package com.example.data.local

import java.io.File

/**
 * Abstraction over file I/O used by [JsonDataManager].
 *
 * Production code uses [EncryptedFileStorage] (AES-256-GCM via EncryptedFile).
 * Tests use [PlainFileStorage] in `src/test/` which writes unencrypted bytes
 * to a temporary directory — no Keystore required.
 */
interface IFileStorage {
    /** Writes [text] to [file], replacing it entirely if it already exists. */
    fun write(file: File, text: String)

    /**
     * Reads the content of [file].
     * @return The file content, or `null` if the file does not exist.
     */
    fun read(file: File): String?
}
