package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import com.example.data.model.Category
import com.example.data.model.TransactionEntity

@Database(entities = [Category::class, TransactionEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Generate or retrieve encryption passphrase
                val passphrase = getOrCreateDatabasePassphrase(context)
                val passphraseBytes = passphrase.toByteArray()
                
                // SQLCipher configuration
                val factory = SupportFactory(passphraseBytes)
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_database"
                )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private fun getOrCreateDatabasePassphrase(context: Context): String {
            val prefs = context.getSharedPreferences("db_encryption", Context.MODE_PRIVATE)
            val existingPassphrase = prefs.getString("db_passphrase", null)
            
            if (existingPassphrase != null) {
                return existingPassphrase
            }
            
            // Generate new secure passphrase
            val newPassphrase = generateSecurePassphrase()
            prefs.edit().putString("db_passphrase", newPassphrase).apply()
            return newPassphrase
        }

        private fun generateSecurePassphrase(): String {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
            val random = java.security.SecureRandom()
            val passphrase = StringBuilder(32)
            for (i in 0 until 32) {
                passphrase.append(chars[random.nextInt(chars.length)])
            }
            return passphrase.toString()
        }
    }
}
