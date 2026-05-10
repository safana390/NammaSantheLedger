package com.example.nammasantheledger

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.nammasantheledger.data.AppDatabase
import com.example.nammasantheledger.data.Transaction
import java.net.URLEncoder
import java.util.concurrent.Executors

class CustomerDetailsActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_details)

        db = AppDatabase.getDatabase(this)

        val customerName = intent.getStringExtra("CUSTOMER_NAME") ?: ""
        val phoneNumber  = intent.getStringExtra("PHONE_NUMBER") ?: ""

        supportActionBar?.title = "$customerName Details"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val avatarTv  = findViewById<TextView>(R.id.detailAvatarText)
        val nameTv    = findViewById<TextView>(R.id.detailCustomerName)
        val phoneTv   = findViewById<TextView>(R.id.detailPhoneNumber)
        val creditTv  = findViewById<TextView>(R.id.detailTotalCredit)
        val balanceTv = findViewById<TextView>(R.id.detailNetBalance)
        val labelTv   = findViewById<TextView>(R.id.detailBalanceLabel)
        val listView  = findViewById<ListView>(R.id.detailHistoryList)
        val whatsappBtn = findViewById<Button>(R.id.whatsappRemindBtn)

        nameTv.text   = customerName
        phoneTv.text  = if (phoneNumber.isEmpty()) "No phone provided" else phoneNumber
        avatarTv.text = customerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        executor.execute {
            val all = db.transactionDao().getAll()
            val customerTransactions = all.filter { it.customerName == customerName }
            
            var totalCredit = 0.0
            var totalPaid   = 0.0
            
            for (t in customerTransactions) {
                if (t.type == "PAYMENT") totalPaid += t.amount else totalCredit += t.amount
            }

            val netBalance = totalCredit - totalPaid

            runOnUiThread {
                creditTv.text  = "₹${String.format("%.2f", totalCredit)}"
                
                if (netBalance > 0) {
                    labelTv.text = "BALANCE"
                    balanceTv.setTextColor(Color.parseColor("#C62828"))
                    balanceTv.text = "₹${String.format("%.2f", netBalance)}"
                    
                    // Show WhatsApp button only if there is pending balance and a phone number
                    if (phoneNumber.isNotEmpty()) {
                        whatsappBtn.visibility = View.VISIBLE
                        whatsappBtn.setOnClickListener {
                            sendWhatsAppReminder(customerName, phoneNumber, netBalance)
                        }
                    }
                } else {
                    labelTv.text = "BALANCE"
                    balanceTv.setTextColor(Color.parseColor("#2E7D32"))
                    balanceTv.text = "₹${String.format("%.2f", Math.abs(netBalance))}"
                    whatsappBtn.visibility = View.GONE
                }

                val adapter = TransactionAdapter(this, customerTransactions)
                listView.adapter = adapter
            }
        }
    }

    private fun sendWhatsAppReminder(name: String, phone: String, balance: Double) {
        val message = "Dear $name, a friendly reminder that your total pending udari is ₹${String.format("%.2f", balance)}. Kindly clear it when possible. Thank you!"
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val url = "https://api.whatsapp.com/send?phone=91$phone&text=" + URLEncoder.encode(message, "UTF-8")
            intent.setPackage("com.whatsapp")
            intent.data = Uri.parse(url)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}