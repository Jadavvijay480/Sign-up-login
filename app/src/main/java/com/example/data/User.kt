package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val username: String,
    val passwordHash: String, // Storing password securely
    val birthdate: String = "",
    val securityAnswer: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
