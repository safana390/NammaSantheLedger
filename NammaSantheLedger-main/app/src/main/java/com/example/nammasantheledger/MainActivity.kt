package com.example.nammasantheledger

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.nammasantheledger.data.AppDatabase
import com.example.nammasantheledger.data.Transaction
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class TransactionAdapter(context: Context, private val items: List<Transaction>) :
    ArrayAdapter<Transaction>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_transaction, parent, false)
        val item = items[position]
        
        val avatarTv   = view.findViewById<TextView>(R.id.avatarText)
        val nameTv     = view.findViewById<TextView>(R.id.customerNameText)
        val typeTv     = view.findViewById<TextView>(R.id.typeText)
        val amountTv   = view.findViewById<TextView>(R.id.amountText)
        val timeTv     = view.findViewById<TextView>(R.id.timestampText)

        avatarTv.text = item.customerName.first().uppercaseChar().toString()
        nameTv.text   = item.customerName
        timeTv.text   = item.timestamp

        if (item.type == "PAYMENT") {
            typeTv.text = "GOT PAYMENT"
            typeTv.setBackgroundColor(Color.parseColor("#2E7D32"))
            amountTv.setTextColor(Color.parseColor("#2E7D32"))
            amountTv.text = "- ₹${String.format("%.2f", item.amount)}"
        } else {
            typeTv.text = "UDARI GIVEN"
            typeTv.setBackgroundColor(Color.parseColor("#C62828"))
            amountTv.setTextColor(Color.parseColor("#C62828"))
            amountTv.text = "+ ₹${String.format("%.2f", item.amount)}"
        }

        return view
    }
}

// Helper class for the summarized main list
data class CustomerSummary(
    val name: String,
    val phone: String,
    val balance: Double
)

class CustomerAdapter(context: Context, private val items: List<CustomerSummary>) :
    ArrayAdapter<CustomerSummary>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_transaction, parent, false)
        val item = items[position]
        
        val avatarTv  = view.findViewById<TextView>(R.id.avatarText)
        val nameTv    = view.findViewById<TextView>(R.id.customerNameText)
        val typeTv    = view.findViewById<TextView>(R.id.typeText)
        val amountTv  = view.findViewById<TextView>(R.id.amountText)
        val timeTv    = view.findViewById<TextView>(R.id.timestampText)

        avatarTv.text = item.name.first().uppercaseChar().toString()
        nameTv.text   = item.name
        timeTv.text   = if(item.phone.isEmpty()) "Click to view history" else item.phone

        if (item.balance > 0) {
            typeTv.text = "PENDING"
            typeTv.setBackgroundColor(Color.parseColor("#C62828")) // Red
            amountTv.setTextColor(Color.parseColor("#C62828"))
            amountTv.text = "₹${String.format("%.2f", item.balance)}"
        } else if (item.balance == 0.0) {
            typeTv.text = "CLEARED"
            typeTv.setBackgroundColor(Color.parseColor("#2E7D32")) // Green
            amountTv.setTextColor(Color.parseColor("#2E7D32"))
            amountTv.text = "₹0.00"
        } else {
            typeTv.text = "ADVANCE"
            typeTv.setBackgroundColor(Color.parseColor("#1565C0")) // Blue
            amountTv.setTextColor(Color.parseColor("#1565C0"))
            amountTv.text = "₹${String.format("%.2f", Math.abs(item.balance))}"
        }

        return view
    }
}

class MainActivity : AppCompatActivity() {

    private val allTransactions = ArrayList<Transaction>()
    private val customerSummaries = ArrayList<CustomerSummary>()
    private val filteredSummaries = ArrayList<CustomerSummary>()
    private var adapter: CustomerAdapter? = null
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var db: AppDatabase
    
    private lateinit var creditTv: TextView
    private lateinit var paymentTv: TextView
    private lateinit var balanceTv: TextView
    private lateinit var balanceLabelTv: TextView
    private lateinit var dailySummaryTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)

        val customerNameInput = findViewById<EditText>(R.id.customerName)
        val phoneInput        = findViewById<EditText>(R.id.phoneNumber)
        val amountInput       = findViewById<EditText>(R.id.amount)
        val addButton         = findViewById<Button>(R.id.addButton)
        val historyList       = findViewById<ListView>(R.id.historyList)
        val searchInput       = findViewById<EditText>(R.id.searchCustomer)
        
        creditTv       = findViewById(R.id.totalCredit)
        paymentTv      = findViewById(R.id.totalPayment)
        balanceTv      = findViewById(R.id.netBalance)
        balanceLabelTv = findViewById(R.id.balanceLabel)
        dailySummaryTv = findViewById(R.id.dailySummaryTv)
        val clearAllTv = findViewById<TextView>(R.id.clearAll)

        loadTransactions()
        adapter = CustomerAdapter(this, filteredSummaries)
        historyList.adapter = adapter

        clearAllTv.setOnClickListener {
            if (allTransactions.isEmpty()) return@setOnClickListener
            AlertDialog.Builder(this)
                .setTitle("Clear All Records?")
                .setMessage("Delete every transaction in the ledger?")
                .setPositiveButton("Clear All") { _, _ ->
                    executor.execute {
                        db.transactionDao().deleteAll()
                        loadTransactions()
                    }
                }.setNegativeButton("Cancel", null).show()
        }

        historyList.setOnItemClickListener { _, _, position, _ ->
            val s = filteredSummaries[position]
            val intent = Intent(this, CustomerDetailsActivity::class.java)
            intent.putExtra("CUSTOMER_NAME", s.name)
            intent.putExtra("PHONE_NUMBER", s.phone)
            startActivity(intent)
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterCustomers(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        addButton.setOnClickListener {
            val name   = customerNameInput.text.toString().trim()
            val phone  = phoneInput.text.toString().trim()
            val amtStr = amountInput.text.toString().trim()
            val type   = if (findViewById<RadioButton>(R.id.radioPayment).isChecked) "PAYMENT" else "CREDIT"

            if (name.isEmpty() || amtStr.isEmpty()) return@setOnClickListener
            val amount = amtStr.toDoubleOrNull() ?: return@setOnClickListener
            val timestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            val entry = Transaction(customerName = name, phoneNumber = phone, amount = amount, timestamp = timestamp, type = type)

            executor.execute {
                db.transactionDao().insert(entry)
                loadTransactions()
                runOnUiThread {
                    customerNameInput.setText(""); phoneInput.setText(""); amountInput.setText("")
                    Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadTransactions() {
        executor.execute {
            val all = db.transactionDao().getAll()
            runOnUiThread {
                allTransactions.clear()
                allTransactions.addAll(all)
                updateSummaries()
            }
        }
    }

    private fun updateSummaries() {
        val groups = allTransactions.groupBy { it.customerName }
        customerSummaries.clear()
        
        for ((name, txs) in groups) {
            var balance = 0.0
            val phone = txs.firstOrNull { it.phoneNumber.isNotEmpty() }?.phoneNumber ?: ""
            for (t in txs) {
                if (t.type == "PAYMENT") balance -= t.amount else balance += t.amount
            }
            customerSummaries.add(CustomerSummary(name, phone, balance))
        }

        filterCustomers(findViewById<EditText>(R.id.searchCustomer).text.toString())
        updateGlobalSummary()
    }

    private fun filterCustomers(query: String) {
        filteredSummaries.clear()
        if (query.isEmpty()) {
            filteredSummaries.addAll(customerSummaries)
        } else {
            filteredSummaries.addAll(customerSummaries.filter { it.name.contains(query, ignoreCase = true) })
        }
        adapter?.notifyDataSetChanged()
    }

    private fun updateGlobalSummary() {
        var totalCollection = 0.0
        var totalSales = 0.0
        var totalOut = 0.0
        var totalIn  = 0.0
        val todayStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

        for (t in allTransactions) {
            if (t.type == "PAYMENT") {
                totalIn += t.amount
                if (t.timestamp.contains(todayStr)) totalCollection += t.amount
            } else {
                totalOut += t.amount
                if (t.timestamp.contains(todayStr)) totalSales += t.amount
            }
        }

        creditTv.text  = "₹${String.format("%.2f", totalOut)}"
        paymentTv.text = "₹${String.format("%.2f", totalIn)}"
        
        val netBalance = totalOut - totalIn
        balanceLabelTv.text = "🔵 BALANCE"
        balanceTv.text = "₹${String.format("%.2f", Math.abs(netBalance))}"
        
        if (netBalance >= 0) {
            // Customers owe you money (Udari > Received)
            balanceTv.setTextColor(Color.parseColor("#C62828")) // Red for pending dues
        } else {
            // You have extra cash / Advance (Received > Udari)
            balanceTv.setTextColor(Color.parseColor("#2E7D32")) // Green for settled/advance
        }

        dailySummaryTv.text = "Today's Sale: ₹${String.format("%.0f", totalSales)} | Got Cash: ₹${String.format("%.0f", totalCollection)}"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
            R.id.menu_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
            R.id.menu_logout -> {
                getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE).edit().putBoolean("isLoggedIn", false).apply()
                startActivity(Intent(this, LoginActivity::class.java)); finish(); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}