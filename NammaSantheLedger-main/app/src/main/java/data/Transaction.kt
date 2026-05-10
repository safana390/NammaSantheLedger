package com.example.nammasantheledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val customerName: String,
    val phoneNumber: String = "",
    val amount: Double,
    val timestamp: String,
    val type: String // "CREDIT" for udari, "PAYMENT" for money received
)