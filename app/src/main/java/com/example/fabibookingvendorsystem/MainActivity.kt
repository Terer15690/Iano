package com.example.fabibookingvendorsystem

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var mDatabase: DatabaseReference
    private lateinit var eventManager: EventManager
    private lateinit var inputHandler: InputHandler
    private var listingsListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        mDatabase = FirebaseDatabase.getInstance().reference
        eventManager = EventManager(this)
        inputHandler = InputHandler(this, eventManager)

        webView = WebView(this)
        
        // Handling touch gestures on the WebView
        webView.setOnTouchListener(inputHandler)
        
        // Ensure WebView can receive keyboard focus
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        
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
                    EventLogger.log("Explore page loaded, initializing listings listener")
                    loadRealListings()
                }
            }
        }
        webView.webChromeClient = WebChromeClient()

        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun openRegister() {
                EventLogger.log("Opening Register Activity")
                val intent = Intent(this@MainActivity, RegisterActivity::class.java)
                startActivity(intent)
            }

            @JavascriptInterface
            fun openLogin() {
                EventLogger.log("Opening Login Activity")
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(intent)
            }

            @JavascriptInterface
            fun openProfile() {
                EventLogger.log("Opening Profile Activity")
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
                EventLogger.log("Requesting delete for listing: $listingId")
                mDatabase.child("listings").child(listingId).removeValue()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            EventLogger.log("Successfully deleted listing $listingId")
                            Toast.makeText(this@MainActivity, "Listing deleted successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            EventLogger.log("Failed to delete listing: ${task.exception?.message}")
                            Toast.makeText(this@MainActivity, "Delete failed: " + task.exception?.message, Toast.LENGTH_LONG).show()
                        }
                    }
            }

            @JavascriptInterface
            fun logInteraction(message: String) {
                eventManager.logAndNotify("User Interaction: $message")
            }

            @JavascriptInterface
            fun openMap(lat: Double, lon: Double) {
                try {
                    // Use a more direct geo intent
                    val uri = Uri.parse("geo:0,0?q=$lat,$lon(Service Location)")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.setPackage("com.google.android.apps.maps")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to a standard web URL if Maps app is not available
                    val mapsUrl = "https://maps.google.com/maps?q=$lat,$lon"
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(browserIntent)
                }
            }

            @JavascriptInterface
            fun sendBookingEmail(photographer: String, amount: String, location: String, date: String, phone: String) {
                val user = FirebaseAuth.getInstance().currentUser
                val email = user?.email ?: ""
                val userId = user?.uid ?: "Anonymous"
                val devEmail = "ianterer06@gmail.com" 
                
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                
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
                    EventLogger.log("Failed to process transaction: ${e.message}")
                }

                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "message/rfc822"
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email, devEmail))
                intent.putExtra(Intent.EXTRA_SUBJECT, "Booking Confirmation: $photographer")
                
                val messageText = """
                    Hello,
                    
                    Your booking has been confirmed!
                    
                    Details:
                    Photographer: $photographer
                    Phone: $phone
                    Amount: $amount
                    Location: $location
                    Date/Time: $date
                    
                    Thank you for choosing FABI.
                """.trimIndent()
                
                intent.putExtra(Intent.EXTRA_TEXT, messageText)
                
                webView.post {
                    try {
                        val chooser = Intent.createChooser(intent, "Send Booking Confirmation")
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(chooser)
                        Toast.makeText(this@MainActivity, "Please tap SEND in your email app to notify the vendor", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Email client not found", Toast.LENGTH_SHORT).show()
                    }
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

        if (isLoggedIn) {
            mDatabase.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Extremely robust fetching
                    val name = snapshot.child("name").value?.toString() 
                        ?: snapshot.child("fullName").value?.toString() 
                        ?: "User"
                    
                    val role = snapshot.child("role").value?.toString() ?: "Client"
                    val profilePic = snapshot.child("profilePicture").value?.toString() ?: ""
                    
                    // Identify if this is the Master Developer
                    val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
                    val isDeveloper = currentUserEmail == "ianterer06@gmail.com"
                    
                    EventLogger.log("Header Update -> Name: $name, Role: $role, Dev: $isDeveloper")

                    webView.post {
                        webView.loadUrl("javascript:updateAuthUI(true, '$userId', '$name', '$role', '$profilePic', $isDeveloper)")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    EventLogger.log("Auth UI Fetch Cancelled: ${error.message}")
                }
            })
        } else {
            webView.post {
                webView.loadUrl("javascript:updateAuthUI(false, '', '', '')")
            }
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
        val available = isNetworkAvailable()
        webView.post {
            webView.loadUrl("javascript:showNetworkStatus($available)")
        }
    }

    private fun loadRealListings() {
        // Remove existing listener to prevent duplicates
        listingsListener?.let { mDatabase.child("listings").removeEventListener(it) }

        listingsListener = object : ValueEventListener {
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
                val json = Gson().toJson(listings)
                webView.post {
                    webView.evaluateJavascript("displayRealListings($json)", null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FABI_ERROR", "DB Error: ${error.message}")
            }
        }
        
        mDatabase.child("listings").addValueEventListener(listingsListener!!)
    }

    private fun testNetworkCall() {
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
        apiService.getWeather(-1.286, 36.817, true).enqueue(object : retrofit2.Callback<WeatherResponse> {
            override fun onResponse(call: retrofit2.Call<WeatherResponse>, response: retrofit2.Response<WeatherResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val temp = response.body()!!.current_weather.temperature
                    webView.post {
                        webView.loadUrl("javascript:updateWeatherUI('$temp°C')")
                    }
                }
            }

            override fun onFailure(call: retrofit2.Call<WeatherResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Weather API Error: " + t.message, Toast.LENGTH_LONG).show()
            }
        })
    }
}
