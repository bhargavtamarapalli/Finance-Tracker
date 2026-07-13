package com.example.fakes

import com.example.data.local.IFileStorage
import java.io.File

/**
 * Plaintext [IFileStorage] fake for use in unit and Robolectric tests **only**.
 *
 * Reads and writes unencrypted UTF-8 text to real files in a directory you control
 * (typically a [org.junit.rules.TemporaryFolder] or [android.content.Context.getFilesDir]).
 * No Android Keystore is required.
 *
 * This class lives exclusively in `src/test/`. It must NEVER be referenced from
 * production source (`src/main/`).
 */
class PlainFileStorage : IFileStorage {
    override fun write(file: File, text: String) {
        file.writeText(text, Charsets.UTF_8)
    }

    override fun read(file: File): String? {
        if (!file.exists()) return null
        return file.readText(Charsets.UTF_8)
    }
}
