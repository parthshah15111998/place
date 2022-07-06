package com.example.placesnearby.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.placesnearby.`interface`.ApiInterface
import com.example.placesnearby.adapter.RestaurantsAdapter
import com.example.placesnearby.databinding.ActivityMainBinding
import com.example.placesnearby.model.RestaurantsModel
import com.example.placesnearby.utils.Constants
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var myAdapter: RestaurantsAdapter
    companion object{
        const val BASE_URL= "https://maps.googleapis.com/maps/api/place/"
        const val EXTRA_PLACE_DETAIL="extra_place_detail"
    }

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    var type ="restaurant"
    var lat =""
    var logi =""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val typeFace: Typeface = Typeface.createFromAsset(assets,"OpenSansExtraBoldItalic.ttf")
        binding.tvPlaceNearBy.typeface=typeFace
        binding.tvLetsNurtureAhmadabad.typeface=typeFace
        binding.rvRestaurantList.setHasFixedSize(true)
        binding.rvRestaurantList.layoutManager= LinearLayoutManager(this)


        binding.btnHospital.setOnClickListener {
            type="hospital"
            getData("$lat,$logi",1000,type,"AIzaSyAymWBDEP5EbIKxQXAgeIcwXBXALl89Ah8")
        }
        binding.btnRestaurant.setOnClickListener {
            type="restaurant"
            getData("$lat,$logi",1000,type,"AIzaSyAymWBDEP5EbIKxQXAgeIcwXBXALl89Ah8")
        }
        binding.btnGasStations.setOnClickListener {
            type="gasStation"
            getData("$lat,$logi",1000,type,"AIzaSyAymWBDEP5EbIKxQXAgeIcwXBXALl89Ah8")
        }


        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()
                        }

                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission. Please allow it is mandatory.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()
        }

    }

    private fun isLocationEnabled(): Boolean {

        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        @SuppressLint("SetTextI18n")
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location? = locationResult.lastLocation
            lat = mLastLocation?.latitude.toString()
            logi = mLastLocation?.longitude.toString()
            getData("$lat,$logi",1000,type,"AIzaSyAymWBDEP5EbIKxQXAgeIcwXBXALl89Ah8")

        }
    }

    private fun getLocationWeatherDetails(){

        if (Constants.isNetworkAvailable(this@MainActivity)) {
            Toast.makeText(
                this@MainActivity,
                "You have connected to the internet. Now you can make an api call.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    private fun getData(location:String,radius:Int,type:String,key:String) {


        val interceptor : HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client : OkHttpClient = OkHttpClient.Builder().apply {
            addInterceptor(interceptor)
        }.build()

        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BASE_URL)
            .client(client)
            .build()
            .create(ApiInterface::class.java)

        val retrofitData = retrofitBuilder.getData(location,radius,type,key)

//        Log.e("request",retrofitData.toString())


        retrofitData.enqueue(object : Callback<RestaurantsModel> {
            override fun onResponse(
                call: Call<RestaurantsModel>,
                response: Response<RestaurantsModel>
            ) {

                binding.shimmerFrameLayout.stopShimmer()
                binding.shimmerFrameLayout.visibility = View.GONE
                binding.rvRestaurantList.visibility=View.VISIBLE
                val responseBody = response.body()!!
//                Log.e("response",responseBody.toString())
                myAdapter = RestaurantsAdapter(baseContext,responseBody.results)
                binding.rvRestaurantList.adapter = myAdapter

            }

            override fun onFailure(call: Call<RestaurantsModel>, t: Throwable) {
                binding.shimmerFrameLayout.visibility=View.GONE
                Log.d("MainActivity", "on failure" + t.message)
            }

        })
    }

    override fun onResume() {
        super.onResume()
        binding.shimmerFrameLayout.startShimmer()
    }

    override fun onPause() {
        binding.shimmerFrameLayout.stopShimmer()
        super.onPause()
    }
}