package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ssh_servers")
data class SshServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val authType: String = "PASSWORD", // "PASSWORD" or "PRIVATE_KEY"
    val password: String = "",
    val privateKey: String = "",
    val passphrase: String = "",
    val defaultPath: String = "/",
    val lastConnectedTime: Long = 0
)
