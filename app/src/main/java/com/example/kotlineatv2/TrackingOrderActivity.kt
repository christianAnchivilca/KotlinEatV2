package com.example.kotlineatv2

import android.animation.ValueAnimator
import com.example.kotlineatv2.R
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.kotlineatv2.Common.Common
import com.example.kotlineatv2.Model.ShippingOrderModel
import com.example.kotlineatv2.Remote.IGoogleApi
import com.example.kotlineatv2.Remote.RetrofitGoogleClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*
import com.google.firebase.database.collection.LLRBNode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject

import kotlin.text.StringBuilder


class TrackingOrderActivity : AppCompatActivity(), OnMapReadyCallback, ValueEventListener {

    private lateinit var mMap: GoogleMap


    //GOOGLE  API
    private lateinit var iGoogleApi: IGoogleApi
    private var polylineOptions:PolylineOptions?=null
    private var shipperMarker:Marker?=null
    private var blackPolylineOptions:PolylineOptions?=null
    private var blackPolyline:Polyline?=null
    private var graykPolyline:Polyline?=null
    private var redPolyline:Polyline?=null
    private val compositeDisposable = CompositeDisposable()
    private var polylineList:List<LatLng> = ArrayList<LatLng>()

    private var isInit=false

    private lateinit var shipperRef:DatabaseReference
    private var handler:Handler?=null
    private var index=0
    private var next:Int=0
    private var v=0f
    private var lat=0.0
    private var lng=0.0
    private var startPosition=LatLng(0.0,0.0)
    private var endPosition = LatLng(0.0,0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.kotlineatv2.R.layout.activity_tracking_order)

        iGoogleApi = RetrofitGoogleClient.instance!!.create(IGoogleApi::class.java)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        subscribeShipperMove()
    }

    private fun subscribeShipperMove() {

        shipperRef = FirebaseDatabase.getInstance()
            .getReference(Common.SHIPPING_ORDER_REF)
            .child(Common.currentShippingOrder!!.key!!)
        shipperRef!!.addValueEventListener(this)

    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap!!.uiSettings.isZoomControlsEnabled = true
        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this,
                    R.raw.uber_light_with_label))
            if (!success)
                Log.d("EDMTDEV","Failed to load map style")
        }catch (ex: Resources.NotFoundException){
            Log.d("EDMTDEV","not found json string for map style")

        }

        //dibujar rutas
        drawRoutes()
    }

    private fun drawRoutes() {
        val locationOrder = LatLng(Common.currentShippingOrder!!.orderModel!!.lat,
        Common.currentShippingOrder!!.orderModel!!.lng)

        val locationShipper = LatLng(Common.currentShippingOrder!!.currentLat,
        Common.currentShippingOrder!!.currentLng)
        //addbox
        mMap.addMarker(MarkerOptions()
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.box))
            .title(Common.currentShippingOrder!!.orderModel!!.userName)
            .snippet(Common.currentShippingOrder!!.orderModel!!.shippingAddress)
            .position(locationOrder))

       //addShipper
        if (shipperMarker == null){
            val height=80
            val width=80
            val bitmapDrawable = ContextCompat.getDrawable(this,R.drawable.shippernew) as BitmapDrawable
            val resized = Bitmap.createScaledBitmap(bitmapDrawable.bitmap,width,height,false)

            shipperMarker = mMap.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resized))
                .title(Common.currentShippingOrder!!.shipperName)
                .snippet(Common.currentShippingOrder!!.shipperPhone)
                .position(locationShipper))
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper,18f))

        }else{
            shipperMarker!!.position = locationShipper
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper,18f))

        }
        //draw route
        val to = StringBuilder().append(Common.currentShippingOrder!!.orderModel!!.lat)
            .append(",")
            .append(Common.currentShippingOrder!!.orderModel!!.lng)
            .toString()

        val from = StringBuilder().append(Common.currentShippingOrder!!.currentLat)
            .append(",")
            .append(Common.currentShippingOrder!!.currentLng)
            .toString()

        compositeDisposable.add(iGoogleApi!!.getDirections("driving","less_driving",
            from,to,getString(R.string.google_maps_key))!!
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {s->

                    try {
                        val jsonObject = JSONObject(s)
                        val jsonArray = jsonObject.getJSONArray("routes")
                        for(i in 0 until jsonArray.length())
                        {
                            val route = jsonArray.getJSONObject(i)
                            val poly = route.getJSONObject("overview_polyline")
                            val polyline = poly.getString("points")
                            polylineList = Common.decodePoly(polyline)
                        }

                        polylineOptions = PolylineOptions()
                        polylineOptions!!.color(Color.RED)
                        polylineOptions!!.width(12f)
                        polylineOptions!!.startCap(SquareCap())
                        polylineOptions!!.endCap(SquareCap())
                        polylineOptions!!.jointType(JointType.ROUND)
                        polylineOptions!!.addAll(polylineList)
                        redPolyline = mMap.addPolyline(polylineOptions)


                    }catch (e: Exception){
                        Log.d("DEBUG",e.message)

                    }

                },{
                    Toast.makeText(this,""+it.message, Toast.LENGTH_SHORT).show()

                }))




    }

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }

    override fun onDestroy() {
        shipperRef.removeEventListener(this)
        isInit=false
        super.onDestroy()
    }

    override fun onCancelled(p0: DatabaseError) {

    }

    override fun onDataChange(dataSnapshot: DataSnapshot) {
        val from = StringBuilder().append(Common.currentShippingOrder!!.currentLat)
            .append(",")
            .append(Common.currentShippingOrder!!.currentLng)
            .toString()
        Common.currentShippingOrder = dataSnapshot.getValue(ShippingOrderModel::class.java)
        Common.currentShippingOrder!!.key = dataSnapshot.key
        //save new position
        val to = StringBuilder().append(Common.currentShippingOrder!!.currentLat)
            .append(",")
            .append(Common.currentShippingOrder!!.currentLng)
            .toString()

        val para = StringBuilder().append(",")

        if (dataSnapshot.exists())
            if (isInit) moveMarkerAnimation(shipperMarker,from,to) else isInit=true


    }

    private fun moveMarkerAnimation(marker: Marker?, from: String, to: String) {
        compositeDisposable.add(iGoogleApi!!.getDirections("driving",
            "less_driving",
            from.toString(),
            to.toString(),
            getString(R.string.google_maps_key))!!.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {s->
                    Log.d("DEBUG",s)
                    try {
                        val jsonObject = JSONObject(s)
                        val jsonArray = jsonObject.getJSONArray("routes")
                        for(i in 0 until jsonArray.length())
                        {
                            val route = jsonArray.getJSONObject(i)
                            val poly = route.getJSONObject("overview_polyline")
                            val polyline = poly.getString("points")
                            polylineList = Common.decodePoly(polyline)
                        }

                        polylineOptions = PolylineOptions()
                        polylineOptions!!.color(Color.BLACK)
                        polylineOptions!!.width(5.0f)
                        polylineOptions!!.startCap(SquareCap())
                        polylineOptions!!.endCap(SquareCap())
                        polylineOptions!!.jointType(JointType.ROUND)
                        polylineOptions!!.addAll(polylineList)
                        graykPolyline= mMap!!.addPolyline(polylineOptions)

                        blackPolylineOptions = PolylineOptions()
                        blackPolylineOptions!!.color(Color.GRAY)
                        blackPolylineOptions!!.width(5.0f)
                        blackPolylineOptions!!.startCap(SquareCap())
                        blackPolylineOptions!!.endCap(SquareCap())
                        blackPolylineOptions!!.jointType(JointType.ROUND)
                        blackPolylineOptions!!.addAll(polylineList)
                        blackPolyline = mMap.addPolyline(blackPolylineOptions)

                        //Animator
                        val polylineAnimator = ValueAnimator.ofInt(0,100)
                        polylineAnimator.setDuration(2000)
                        polylineAnimator.setInterpolator(LinearInterpolator())
                        polylineAnimator.addUpdateListener {
                                valueAnimator: ValueAnimator ->
                            val points=graykPolyline!!.points
                            val porcentValue =Integer.parseInt(valueAnimator.animatedValue.toString())
                            val size = points.size
                            val newPoints = (size *(porcentValue /100.0f)).toInt()
                            val p = points.subList(0,newPoints)
                            blackPolyline!!.points = p

                        }

                        polylineAnimator.start()
                        //cart moving
                        index = -1
                        next = -1

                        val r = object:Runnable {
                            override fun run() {
                                if (index < polylineList.size - 1)
                                {
                                    index++
                                    next=index+1
                                    startPosition = polylineList[index]
                                }

                                val valueAnimator = ValueAnimator.ofInt(0,1)
                                valueAnimator.setDuration(1500)
                                valueAnimator.setInterpolator(LinearInterpolator())
                                valueAnimator.addUpdateListener{ valueAnimator->
                                    v=valueAnimator.animatedFraction
                                    lat = v * endPosition!!.latitude + (1-v)*startPosition!!.latitude
                                    lng = v * endPosition!!.longitude +(1-v) * startPosition!!.longitude
                                    val newPos = LatLng(lat,lng)
                                    marker!!.position = newPos
                                    marker!!.setAnchor(0.5f,0.5f)
                                    marker!!.rotation = Common.getBearing(startPosition!!,newPos)

                                    mMap!!.moveCamera(CameraUpdateFactory.newLatLng(marker.position))

                                }
                                valueAnimator.start()
                                if (index < polylineList.size - 2)
                                    handler!!.postDelayed(this,1500)
                            }


                        }

                        handler = Handler()
                        handler!!.postDelayed(r,1500)




                    }catch (e:Exception){
                        Log.d("DEBUG",e.message)

                    }

                },{
                    Toast.makeText(this,""+it.message,Toast.LENGTH_SHORT).show()

                }))



    }




}