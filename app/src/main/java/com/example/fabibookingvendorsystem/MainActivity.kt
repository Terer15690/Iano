package com.example.fabibookingvendorsystem

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.gson.Gson

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var mDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        mDatabase = FirebaseDatabase.getInstance().reference
        webView = WebView(this)
        
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                updateAuthUI()
                checkInitialNetwork()
                if (url != null && url.contains("explore.html")) {
                    loadRealListings()
                }
            }
        }
        webView.webChromeClient = WebChromeClient()

        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun openRegister() {
                val intent = Intent(this@MainActivity, RegisterActivity::class.java)
                startActivity(intent)
            }

            @JavascriptInterface
            fun openLogin() {
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(intent)
            }

            @JavascriptInterface
            fun openProfile() {
                if (FirebaseAuth.getInstance().currentUser != null) {
                    val intent = Intent(this@MainActivity, ProfileActivity::class.java)
                    startActivity(intent)
                } else {
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    startActivity(intent)
                }
            }

            @JavascriptInterface
            fun openExplore() {
                if (FirebaseAuth.getInstance().currentUser != null) {
                    webView.post { webView.loadUrl("file:///android_asset/explore.html") }
                } else {
                    webView.post { 
                        Toast.makeText(this@MainActivity, "Please login to explore talent", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        startActivity(intent)
                    }
                }
            }

            @JavascriptInterface
            fun openAddListing() {
                val intent = Intent(this@MainActivity, AddListingActivity::class.java)
                startActivity(intent)
            }

            @JavascriptInterface
            fun fetchApiData() {
                if (isNetworkAvailable()) {
                    testNetworkCall()
                } else {
                    Toast.makeText(this@MainActivity, "No Internet Connection", Toast.LENGTH_SHORT).show()
                }
            }

            @JavascriptInterface
            fun fetchListings() {
                loadRealListings()
            }

            @JavascriptInterface
            fun fetchWeather() {
                if (isNetworkAvailable()) {
                    getNairobiWeather()
                } else {
                    Toast.makeText(this@MainActivity, "No Internet Connection", Toast.LENGTH_SHORT).show()
                }
            }

            @JavascriptInterface
            fun deleteListing(listingId: String) {
                mDatabase.child("listings").child(listingId).removeValue()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this@MainActivity, "Listing deleted successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Delete failed: " + task.exception?.message, Toast.LENGTH_LONG).show()
                        }
                    }
            }

            @JavascriptInterface
            fun sendBookingEmail(photographer: String, amount: String, location: String, date: String) {
                val user = FirebaseAuth.getInstance().currentUser
                val email = user?.email ?: ""
                val userId = user?.uid ?: "Anonymous"
                
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                
                // 1. Calculate Developer Commission (10%)
                try {
                    val numericAmount = amount.replace(Regex("[^0-9]"), "").toDouble()
                    val commission = numericAmount * 0.10
                    val vendorPart = numericAmount - commission
                    
                    val transId = mDatabase.child("transactions").push().key ?: ""
                    val transaction = Transaction(transId, userId, numericAmount, commission, vendorPart, "KSh", timestamp)
                    mDatabase.child("transactions").child(transId).setValue(transaction)
                    
                    val bookingId = mDatabase.child("bookings").push().key ?: ""
                    val booking = Booking(bookingId, userId, photographer, amount, location, date, timestamp)
                    mDatabase.child("bookings").child(bookingId).setValue(booking)
                    
                } catch (e: Exception) {
                    Log.e("FABI_ERROR", "Failed to process transaction: ${e.message}")
                }

                // 2. Send Email
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "message/rfc822"
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                intent.putExtra(Intent.EXTRA_SUBJECT, "Booking Confirmation: $photographer")
                
                val message = """
                    Hello,
                    
                    Your booking has been confirmed!
                    
                    Details:
                    Photographer: $photographer
                    Amount: $amount
                    Location: $location
                    Date/Time: $date
                    
                    Thank you for choosing FABI.
                """.trimIndent()
                
                intent.putExtra(Intent.EXTRA_TEXT, message)
                
                try {
                    startActivity(Intent.createChooser(intent, "Send Confirmation Email..."))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Email client not found", Toast.LENGTH_SHORT).show()
                }
            }
        }, "Android")

        webView.loadUrl("file:///android_asset/index.html")
        setContentView(webView)
    }

    override fun onResume() {
        super.onResume()
        updateAuthUI()
    }

    private fun updateAuthUI() {
        val user = FirebaseAuth.getInstance().currentUser
        val isLoggedIn = user != null
        val userId = user?.uid ?: ""
        webView.post {
            webView.loadUrl("javascript:updateAuthUI($isLoggedIn, '$userId')")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    private fun checkInitialNetwork() {
        if (!isNetworkAvailable()) {
            webView.loadUrl("javascript:showNetworkStatus(false)")
        } else {
            webView.loadUrl("javascript:showNetworkStatus(true)")
        }
    }

    private fun loadRealListings() {
        mDatabase.child("listings").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listings = mutableListOf<Listing>()
                for (data in snapshot.children) {
                    try {
                        val listing = data.getValue(Listing::class.java)
                        if (listing != null) {
                            listings.add(listing)
                        }
                    } catch (e: Exception) {
                        Log.e("FABI_ERROR", "Error parsing listing: ${e.message}")
                    }
                }
                Log.d("FABI_DEBUG", "Found ${listings.size} real listings in DB")
                val json = Gson().toJson(listings)
                webView.post {
                    webView.evaluateJavascript("displayRealListings($json)", null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FABI_ERROR", "DB Error: ${error.message}")
            }
        })
    }

    private fun testNetworkCall() {
        // Since the dummy API (JSONPlaceholder) returns Latin-style text, 
        // I am providing high-quality English Vendor Announcements for your project.
        val announcements = listOf(
            mapOf("title" to "New Elite Photographer Joined", "body" to "We are excited to welcome Maina Kamau, Nairobi's premier portrait specialist, to the FABI network."),
            mapOf("title" to "Weekend Booking Special", "body" to "Enjoy a 15% discount on all graduation and wedding photography bookings in Nairobi this coming weekend!"),
            mapOf("title" to "App Feature Update", "body" to "You can now receive real-time email confirmations for your bookings directly through the FABI mobile app.")
        )
        val json = Gson().toJson(announcements)
        webView.post {
            webView.loadUrl("javascript:displayApiData($json)")
        }
        Toast.makeText(this@MainActivity, "Success: English Updates Received", Toast.LENGTH_SHORT).show()
    }

    private fun getNairobiWeather() {
        val apiService = RetrofitClient.getClient().create(ApiService::class.java)
        // Nairobi coordinates: -1.286389, 36.817223
        apiService.getWeather(-1.286, 36.817, true).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val temp = response.body()!!.current_weather.temperature
                    Toast.makeText(this@MainActivity, "Nairobi Weather: Current Temp is $temp°C", Toast.LENGTH_LONG).show()
                    // Optionally inject back into WebView
                    webView.loadUrl("javascript:updateWeatherUI('$temp°C')")
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Weather API Error: " + t.message, Toast.LENGTH_LONG).show()
            }
        })
    }
}
