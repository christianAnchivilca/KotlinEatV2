package com.example.kotlineatv2.ui.cart

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.braintreepayments.api.dropin.DropInRequest
import com.braintreepayments.api.dropin.DropInResult
import com.example.kotlineatv2.Adapter.MyCartAdapter
import com.example.kotlineatv2.Callback.ILoadTimeFromFirebaseCallback
import com.example.kotlineatv2.Callback.IMyButtonCallback
import com.example.kotlineatv2.Callback.ISearchCategoryCallbackListener
import com.example.kotlineatv2.Common.Common
import com.example.kotlineatv2.Common.MySwipeHelper
import com.example.kotlineatv2.Database.CartDataSource
import com.example.kotlineatv2.Database.CartDatabase
import com.example.kotlineatv2.Database.CartItem
import com.example.kotlineatv2.Database.LocalCartDataSource
import com.example.kotlineatv2.EventBus.CounterCartEvent
import com.example.kotlineatv2.EventBus.HidenFABCart
import com.example.kotlineatv2.EventBus.MenuItemBack
import com.example.kotlineatv2.EventBus.UpdateItemInCart
import com.example.kotlineatv2.Model.*
import com.example.kotlineatv2.R
import com.example.kotlineatv2.Remote.ICloudFunctions
import com.example.kotlineatv2.Remote.IFCMService
import com.example.kotlineatv2.Remote.RetrofitCloudClient
import com.example.kotlineatv2.Remote.RetrofitFCMClient
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.IOError
import java.io.IOException
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CartFragment : Fragment(), ILoadTimeFromFirebaseCallback, ISearchCategoryCallbackListener,
    TextWatcher {


    override fun onLoadTimeSuccess(order: OrderModel, estimatedTimeMs: Long) {

        order.createDate = estimatedTimeMs
        order.orderStatus = 0
        writeOrderToFirebase(order)
    }

    override fun onLoadTimeFailed(message: String) {
        Toast.makeText(context,message,Toast.LENGTH_SHORT).show()
    }

    // GOOGLE PLACES VARIABLES
    private var placeSelected: Place?=null
    private var places_fragment: AutocompleteSupportFragment?=null
    private lateinit var placeClient: PlacesClient
    private val placeFields = Arrays.asList(
        Place.Field.ID,
        Place.Field.NAME,
        Place.Field.ADDRESS,
        Place.Field.LAT_LNG)

    private lateinit var addOnBottomSheetDialog: BottomSheetDialog
    var chip_group_user_selected_addon: ChipGroup?=null

    //layout addon
    var chip_group_addon: ChipGroup?=null
    private var edt_search_addon:EditText?=null


    private val REQUEST_BRAINTREE_CODE :Int = 9999
    private var cartDataSource:CartDataSource? = null
    private var compositeDisposable:CompositeDisposable = CompositeDisposable()
    private var recyclerViewState:Parcelable?=null
    private lateinit var cartViewModel: CartViewModel

    var empty_cart_icon:ImageView?=null
    var empty_cart_text:TextView?=null
    var txt_total_price:TextView?=null
    var btn_place_order:MaterialButton?=null
    var group_place_holder:CardView?=null
    var recycler_cart:RecyclerView?=null
    var adapter:MyCartAdapter?=null

    lateinit var iSearchCategoryCallback:ISearchCategoryCallbackListener

    private lateinit var locationRequest:LocationRequest
    private lateinit var locationCallback:LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentLocation:Location

    internal var address:String = ""
    internal var comment:String = ""
    lateinit var cloudFunctions:ICloudFunctions
    lateinit var listener:ILoadTimeFromFirebaseCallback
    lateinit var ifcmService:IFCMService



    override fun onResume() {
        super.onResume()
        calculateTotalPrice()
        if (ActivityCompat.checkSelfPermission(
                context!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,
                Looper.getMainLooper())

    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        EventBus.getDefault().postSticky(HidenFABCart(true))
        cartViewModel = ViewModelProviders.of(this).get(CartViewModel::class.java)

        //despues crear el cartViewModel debemos iniciar nuestro data source
        cartViewModel.initCartDataSource(context!!)

        val root = inflater.inflate(R.layout.fragment_cart, container, false)

        initView(root)
        initLocation()

        cartViewModel.getMutableLiveDataCartItem().observe(this, Observer {

            if (it == null || it.isEmpty()){
                recycler_cart!!.visibility = View.GONE
                group_place_holder!!.visibility = View.GONE
                empty_cart_icon!!.visibility = View.VISIBLE
                empty_cart_text!!.visibility = View.VISIBLE
            }else{
                recycler_cart!!.visibility = View.VISIBLE
                group_place_holder!!.visibility = View.VISIBLE
                empty_cart_icon!!.visibility = View.GONE
                empty_cart_text!!.visibility = View.GONE

                adapter = MyCartAdapter(context!!,it)
                recycler_cart!!.adapter = adapter

            }

        })

        return root
    }

    private fun initLocation() {
        buildLocationRequest()
        buildLocationCallback()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context!!)
        fusedLocationProviderClient!!.requestLocationUpdates(locationRequest,locationCallback,
            Looper.getMainLooper())
    }

    private fun buildLocationCallback() {
        locationCallback = object :LocationCallback(){
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                currentLocation = p0!!.lastLocation

            }
        }
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setInterval(5000)
        locationRequest.setFastestInterval(3000)
        locationRequest.setSmallestDisplacement(10f)

    }

    private fun initView(root: View) {
        iSearchCategoryCallback = this
        initPlaceClient()

        setHasOptionsMenu(true) // important, if not add it , menu will never be inflate
        cloudFunctions = RetrofitCloudClient.getInstance().create(ICloudFunctions::class.java)
        //set initialize fcmservice
        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService::class.java)

        listener = this
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context!!).cartDAO())


        addOnBottomSheetDialog = BottomSheetDialog(context!!,R.style.DialogStyle)
        val layout_user_selected_addon = layoutInflater.inflate(R.layout.layout_addon_display,null)
        chip_group_addon = layout_user_selected_addon.findViewById(R.id.chip_addon_display) as ChipGroup
        edt_search_addon = layout_user_selected_addon.findViewById(R.id.edt_search_addon) as EditText

        addOnBottomSheetDialog.setContentView(layout_user_selected_addon)
        addOnBottomSheetDialog.setOnDismissListener{
                dialogInterface ->

            displayUserSelectedAddon(chip_group_user_selected_addon)
            calculateTotalPrice()

        }

        recycler_cart = root.findViewById(R.id.recycler_cart) as RecyclerView
        recycler_cart!!.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        recycler_cart!!.layoutManager = layoutManager
        recycler_cart!!.addItemDecoration(DividerItemDecoration(context,layoutManager.orientation))

        val swipe = object:MySwipeHelper(context!!,recycler_cart!!,200)
        {

            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(MyButton(context!!,
                    "Eliminar",
                    30,
                    0,
                    Color.parseColor("#FF3C30"),
                    object :IMyButtonCallback{
                        override fun onClick(pos: Int) {

                            val deleteItem = adapter!!.getItemAtPosition(pos)

                            cartDataSource!!.deleteCart(deleteItem!!)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(object:SingleObserver<Int>{
                                    override fun onSuccess(t: Int) {

                                        adapter!!.notifyItemRemoved(pos)
                                        sumCart()

                                        EventBus.getDefault().postSticky(CounterCartEvent(true))
                                        Toast.makeText(context,"Eliminado",Toast.LENGTH_LONG).show()
                                    }

                                    override fun onSubscribe(d: Disposable) {
                                    }

                                    override fun onError(e: Throwable) {
                                        Toast.makeText(context,"[DELETE ERROR]"+e.message,Toast.LENGTH_LONG).show()
                                    }

                                })

                            //fin

                        }}))
                buffer.add(MyButton(context!!,
                    "Actualizar",
                    30,
                    0,
                    Color.parseColor("#5d4037"),
                    object :IMyButtonCallback{
                        override fun onClick(pos: Int) {

                            val cartItem = adapter!!.getItemAtPosition(pos)
                            FirebaseDatabase.getInstance()
                                .getReference(Common.CATEGORY_REFERENCE)
                                .child(cartItem.categoryId)
                                .addListenerForSingleValueEvent(object:ValueEventListener{
                                    override fun onCancelled(error: DatabaseError) {
                                        iSearchCategoryCallback!!.onSearchNotFound(error.message)
                                    }

                                    override fun onDataChange(dataSnapShot: DataSnapshot) {
                                        if(dataSnapShot.exists())
                                        {
                                            val categoryModel = dataSnapShot.getValue(CategoryModel::class.java)
                                            iSearchCategoryCallback!!.onSearchFound(categoryModel!!,cartItem)

                                        }else{
                                            iSearchCategoryCallback!!.onSearchNotFound("Categoria no encontrada")
                                        }

                                    }

                                })


                        }}))

            }


        }

        empty_cart_text = root.findViewById(R.id.empty_cart_text) as TextView
        empty_cart_icon = root.findViewById(R.id.empty_cart_icon) as ImageView
        group_place_holder = root.findViewById(R.id.group_place_holder) as CardView
        txt_total_price = root.findViewById(R.id.txt_total_price) as TextView
        btn_place_order = root.findViewById(R.id.btn_place_order) as MaterialButton

        btn_place_order!!.setOnClickListener{
            val dialog = AlertDialog.Builder(context!!)
            dialog.setTitle("Un Paso Mas !")

            val view = LayoutInflater.from(context!!).inflate(R.layout.layout_place_order,null)

            val edt_comment_dialog = view.findViewById<View>(R.id.edt_comment_dialog) as EditText
            val txt_address = view.findViewById<View>(R.id.txt_address_detail) as TextView

            val rdi_home = view.findViewById<RadioButton>(R.id.rdi_home_address)
            val rdi_other_adress = view.findViewById<RadioButton>(R.id.rdi_other_address)
            val rdi_shipAddres = view.findViewById<RadioButton>(R.id.rdi_ship_this_address)
            val rdi_cod = view.findViewById<RadioButton>(R.id.rdi_cod)
            val rdi_braintree = view.findViewById<RadioButton>(R.id.rdi_braintree)

            places_fragment = activity!!.supportFragmentManager
                .findFragmentById(R.id.places_autocomplete_fragment) as AutocompleteSupportFragment
            places_fragment!!.setPlaceFields(placeFields)
            places_fragment!!.setOnPlaceSelectedListener(object: PlaceSelectionListener {
                override fun onPlaceSelected(p0: Place) {
                    placeSelected = p0
                    txt_address.text = placeSelected!!.address
                }

                override fun onError(p0: Status) {

                    Toast.makeText(context!!,""+p0.statusMessage,Toast.LENGTH_LONG).show()

                }

            })


            txt_address.setText(Common.currentUser!!.address!!)

            rdi_home.setOnCheckedChangeListener{
                compoundButton,b->
                if (b){
                    txt_address.setText(Common.currentUser!!.address!!)

                }
            }
            rdi_other_adress.setOnCheckedChangeListener{
                    compoundButton,b->
                if (b){

                    txt_address.setText("")
                }
            }
            rdi_shipAddres.setOnCheckedChangeListener{
                    compoundButton , b ->
                if (b){
                    fusedLocationProviderClient!!.lastLocation
                        .addOnFailureListener{ e ->
                            txt_address.visibility = View.GONE
                            Toast.makeText(context,""+e.message, Toast.LENGTH_LONG).show()
                        }
                        .addOnCompleteListener{ task ->
                            if (task.result == null){
                                Toast.makeText(context,"Verifica si tienes activo tu ubicacion y vuelve a ingresar a la APP", Toast.LENGTH_LONG).show()
                            }else{
                                val coordinates = StringBuilder()
                                    .append(task.result!!.latitude)
                                    .append("/")
                                    .append(task.result!!.longitude)
                                    .toString()

                                val singleAddress = Single.just(getAddressFromLatLng(task.result!!.latitude,
                                    task.result!!.longitude))
                                val disposable = singleAddress.subscribe(object:DisposableSingleObserver<String>(){
                                    override fun onSuccess(t: String) {

                                        txt_address.setText(t)
                                    }

                                    override fun onError(e: Throwable) {

                                        txt_address.setText(e.message)
                                    }

                                })


                            }
                        }
                }


            }

            dialog.setView(view)
            dialog.setNegativeButton("NO"){dialogInterface,_->dialogInterface.dismiss()}
            dialog.setPositiveButton("SI"){dialogInterface,_->
                if (rdi_cod.isChecked){
                    paymedCOD(txt_address.text.toString(),edt_comment_dialog.text.toString())
                }else if(rdi_braintree.isChecked){
                    address = txt_address.text.toString()
                    comment = edt_comment_dialog.text.toString()
                    //si no es nulo, si tenemos Token
                    if (!TextUtils.isEmpty(Common.currentToken))
                    {
                        val dropInRequest = DropInRequest().clientToken(Common.currentToken)
                        startActivityForResult(dropInRequest.getIntent(context!!),REQUEST_BRAINTREE_CODE)
                    }
                }
            }
            val crearDialogo = dialog.create()
            crearDialogo.show()
        }

    }

    private fun displayUserSelectedAddon(chipGroupUserSelectedAddon: ChipGroup?)
    {
        if(Common.foodModelSelected!!.userSelectedAddon != null &&
            Common.foodModelSelected!!.userSelectedAddon!!.size > 0)
        {
            chipGroupUserSelectedAddon!!.removeAllViews()
            for (addonModel in Common.foodModelSelected!!.userSelectedAddon!!)
            {
                val chip = layoutInflater.inflate(R.layout.layout_chip_with_delete,null) as Chip
                chip.text = StringBuilder(addonModel.name).append("+($")
                    .append(addonModel.price).append(")")
                chip.setOnCheckedChangeListener { buttonView, isChecked ->
                    if(isChecked)
                        if(Common.foodModelSelected!!.userSelectedAddon == null)Common.foodModelSelected!!.userSelectedAddon= ArrayList()
                        Common.foodModelSelected!!.userSelectedAddon!!.add(addonModel)
                }
                chipGroupUserSelectedAddon.addView(chip)


            }

        }else
            chipGroupUserSelectedAddon!!.removeAllViews()

    }

    private fun initPlaceClient() {
        Places.initialize(context!!,getString(R.string.google_maps_key))
        placeClient = Places.createClient(context!!)

    }

    private fun paymedCOD(address: String, comment: String) {
        compositeDisposable.add(cartDataSource!!.getAllCart(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                cartListItem->
                //una vez tengamos todos los items del carrito , obtenemos tambien la suma total de precio a pagar
                cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : SingleObserver<Double>{
                        override fun onSuccess(totalPrice: Double) {

                            val finalPrice = totalPrice
                            val order = OrderModel()
                            order.userId = Common.currentUser!!.uid!!
                            order.userName = Common.currentUser!!.name!!
                            order.userPhone = Common.currentUser!!.phone!!
                            order.shippingAddress = address
                            order.comment = comment
                            if (currentLocation != null){
                                order.lat = currentLocation!!.latitude
                                order.lng = currentLocation!!.longitude
                            }
                            order.cartItemList = cartListItem
                            order.isCod = true
                            order.totalPayment = totalPrice
                            order.finalpayment = finalPrice
                            order.discount = 0
                            order.transactionId = "COD"

                            //LO ENVIAMOS Y REGISTRAMOS EN FIREBASE
                            syncLocalTimeWithServerTime(order)


                        }

                        override fun onSubscribe(d: Disposable) {
                        }

                        override fun onError(e: Throwable) {
                            if (!e.message!!.contains("Query returned empty"))
                                Toast.makeText(context,""+e.message, Toast.LENGTH_LONG).show()
                        }

                    })


            },{
               t->
                Toast.makeText(context,""+t.message,Toast.LENGTH_LONG).show()
            }))


    }

    private fun writeOrderToFirebase(order: OrderModel) {
        FirebaseDatabase.getInstance()
            .getReference(Common.ORDER_REFERENCE)
            .child(Common.createOrderNumber())
            .setValue(order)
            .addOnFailureListener{e->Toast.makeText(context,""+e.message,Toast.LENGTH_LONG).show()}
            .addOnCompleteListener{task ->

                if (task.isSuccessful){
                    //clean cart
                    cartDataSource!!.cleanCart(Common.currentUser!!.uid!!)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object:SingleObserver<Int>{
                            override fun onSuccess(t: Int) {
                                val dataSend = HashMap<String,String>()
                                dataSend.put(Common.NOTI_TITLE,"Nueva Orden")
                                dataSend.put(Common.NOTI_CONTENT,"Tienes una nueva orden: "+Common.currentUser!!.phone)

                                val sendData = FCMSendData(Common.getNewOrderTopic(),dataSend)

                                compositeDisposable.add(

                                    ifcmService.sendNotification(sendData)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe({
                                            if (it.success != 0)
                                                Toast.makeText(context,"Orden realizada exitosamente!",Toast.LENGTH_LONG).show()
                                        },{
                                            t: Throwable? ->
                                            Toast.makeText(context,""+t!!.message,Toast.LENGTH_LONG).show()
                                        })

                                )




                            }
                            override fun onSubscribe(d: Disposable) {
                            }
                            override fun onError(e: Throwable) {
                                Toast.makeText(context,""+e.message,Toast.LENGTH_LONG).show()
                            }

                        })

                }

            }

    }

    private fun getAddressFromLatLng(latitude: Double, longitude: Double): String {
        val geoCoder = Geocoder(context, Locale.getDefault())
        var result:String?=null
        try {

            val addressList = geoCoder.getFromLocation(latitude,longitude,1)
            if (addressList != null && addressList.size > 0){
                val address = addressList[0]
                val sb = StringBuilder(address.getAddressLine(0))
                result = sb.toString()
            }else{
                result = "Direccion no encontrada"
            }
            return result!!

        }catch (e:IOException){
            return e.message!!
        }


    }

    private fun sumCart() {
        cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object:SingleObserver<Double>{
                override fun onSuccess(price: Double) {
                  txt_total_price!!.text = StringBuilder("Total: $").append(Common.formatPrice(price))
                }

                override fun onSubscribe(d: Disposable) {
                }

                override fun onError(e: Throwable) {
                 if (!e.message!!.contains("Query returned empty"))
                     Toast.makeText(context,""+e.message,Toast.LENGTH_LONG).show()

                }

            })
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
             EventBus.getDefault().register(this)
    }

    override fun onStop() {

        cartViewModel!!.onStop()
        compositeDisposable.clear()
        EventBus.getDefault().postSticky(HidenFABCart(false))
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this)
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()


    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onUpdateItemInCart(event: UpdateItemInCart){
        if (event.cartItem != null){
            //actualizar el ITEM QUE SE ESTA ACTUALIZANDO SU CANTIDAD
            recyclerViewState = recycler_cart!!.layoutManager!!.onSaveInstanceState()

            cartDataSource!!.updateCart(event.cartItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object:SingleObserver<Int>{
                    override fun onSuccess(t: Int) {
                        calculateTotalPrice()
                        recycler_cart!!.layoutManager!!.onRestoreInstanceState(recyclerViewState)
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context,""+e.message, Toast.LENGTH_LONG).show()
                    }

                })
        }

    }

    private fun calculateTotalPrice(){
        cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object:SingleObserver<Double>{
                override fun onSuccess(price: Double) {
                    txt_total_price!!.text = StringBuilder("Total: $").append(Common.formatPrice(price))
                }

                override fun onSubscribe(d: Disposable) {
                }

                override fun onError(e: Throwable) {
                    if (!e.message!!.contains("Query returned empty"))
                         Toast.makeText(context,""+e.message, Toast.LENGTH_LONG).show()
                }

            })

    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_settings).setVisible(false) // hiden setting menu when in Cart
        super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.cart_menu,menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.action_clear_cart){


            cartDataSource!!.cleanCart(Common.currentUser!!.uid!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<Int>{
                    override fun onSuccess(t: Int) {

                        EventBus.getDefault().postSticky(CounterCartEvent(true))
                    }

                    override fun onSubscribe(d: Disposable) {
                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context,""+e.message, Toast.LENGTH_LONG).show()
                    }

                })


            return true
        }

        return super.onOptionsItemSelected(item)
    }

    //Cuando el usuario termina con la actividad subsiguiente y regresa, el sistema llama al método onActivityResult() de la actividad.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_BRAINTREE_CODE){

            // Asegúrese de que la solicitud se haya realizado correctamente
            if(resultCode ==  Activity.RESULT_OK)
            {
               val result = data!!.getParcelableExtra<DropInResult>(DropInResult.EXTRA_DROP_IN_RESULT)

                val nonce = result!!.paymentMethodNonce
                //calculamos la suma total
                cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object:SingleObserver<Double>{
                        override fun onSuccess(totalPrice: Double) {
                            //GET ALL ITEMS TO CREATE CART
                            compositeDisposable.add(cartDataSource!!.getAllCart(Common.currentUser!!.uid!!)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    cartItems :List<CartItem> ->

                                    val headers = HashMap<String,String>()
                                    headers.put("Authorization",Common.buildToken(Common.authorizeToken))

                                    compositeDisposable.add(cloudFunctions.submitPayment(headers,totalPrice,nonce!!.nonce)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe({
                                            braintreeTransaction ->

                                            //si la transaccion fue exitosa
                                            if (braintreeTransaction.success){
                                                //create order en firebase
                                                val finalPrice = totalPrice
                                                val order = OrderModel()
                                                order.userId = Common.currentUser!!.uid!!
                                                order.userName = Common.currentUser!!.name!!
                                                order.userPhone = Common.currentUser!!.phone!!
                                                order.shippingAddress = address
                                                order.comment = comment
                                                if (currentLocation != null){
                                                    order.lat = currentLocation!!.latitude
                                                    order.lng = currentLocation!!.longitude
                                                }
                                                order.cartItemList = cartItems
                                                order.isCod = false
                                                order.totalPayment = totalPrice
                                                order.finalpayment = finalPrice
                                                order.discount = 0
                                                order.transactionId = braintreeTransaction.transaction!!.id

                                                //LO ENVIAMOS Y REGISTRAMOS EN FIREBASE
                                                syncLocalTimeWithServerTime(order)

                                            }


                                        },{
                                          t:Throwable->
                                            if (!t.message!!.contains("Query returned empty"))
                                                Toast.makeText(context,""+t.message, Toast.LENGTH_LONG).show()



                                        }))



                                },{
                                    t:Throwable->
                                    Toast.makeText(context,""+t.message, Toast.LENGTH_LONG).show()
                                }))


                        }

                        override fun onSubscribe(d: Disposable) {

                        }

                        override fun onError(e: Throwable) {
                            if (!e.message!!.contains("Query returned empty"))
                                Toast.makeText(context,""+e.message, Toast.LENGTH_LONG).show()

                        }

                    })
            }
        }
    }

    private fun syncLocalTimeWithServerTime(order: OrderModel) {
        val offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
        offsetRef.addListenerForSingleValueEvent(object :ValueEventListener{


            override fun onCancelled(p0: DatabaseError) {
                listener.onLoadTimeFailed(p0.message)
            }

            override fun onDataChange(p0: DataSnapshot) {
                val offset = p0.getValue(Long::class.java)
                val estimatedServerTimeInMs = System.currentTimeMillis() + offset!!
                val sdf = SimpleDateFormat("MMM dd yyyy, HH:mm")
                val date = Date(estimatedServerTimeInMs)
                Log.d("TIME-EXECUTE",""+sdf.format(date))
                listener.onLoadTimeSuccess(order,estimatedServerTimeInMs)
            }

        })

    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }

    override fun onSearchFound(category: CategoryModel, cartItem: CartItem) {
        val foodModel:FoodModel = Common.findFoodInListById(category,cartItem.foodId)!!
        if(foodModel != null)
            showUpdateDialog(cartItem,foodModel)
        else
            Toast.makeText(context!!,"Food Id no encontrado",Toast.LENGTH_LONG).show()

    }

    private fun showUpdateDialog(cartItem: CartItem, foodModel: FoodModel) {
        Common.foodModelSelected  = foodModel
        val builder = AlertDialog.Builder(context!!)
        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_dialog_update_cart,null)
        builder.setView(itemView)

        val btn_ok = itemView.findViewById<View>(R.id.btn_ok) as Button
        val btn_cancel = itemView.findViewById<View>(R.id.btn_cancel) as Button
        val rb_large = itemView.findViewById<View>(R.id.rb_large) as RadioButton
        val rb_medium = itemView.findViewById<View>(R.id.rb_medium) as RadioButton
        val radio_group_size = itemView.findViewById<View>(R.id.radio_group_size) as RadioGroup

        chip_group_user_selected_addon = itemView.findViewById<View>(R.id.chip_group_user_selected_addon) as ChipGroup
        val img_add_addon = itemView.findViewById<View>(R.id.img_add_addon) as ImageView
        img_add_addon.setOnClickListener{
            if(foodModel.addon != null){
                  displayAddonList()
                addOnBottomSheetDialog!!.show()
            }
        }

        //size
        rb_medium!!.text = foodModel.size[0].name
        rb_medium!!.tag = foodModel.size[0].price
        rb_large!!.text = foodModel.size[1].name
        rb_large!!.tag = foodModel.size[1].price


        rb_medium!!.setOnCheckedChangeListener{compoundButton,b->
            if (b)
                Common.foodModelSelected!!.userSelectedSize = foodModel.size[0]
            calculateTotalPrice()

        }

        rb_large!!.setOnCheckedChangeListener{compoundButton,b->
            if (b)
                Common.foodModelSelected!!.userSelectedSize = foodModel.size[1]
            calculateTotalPrice()

        }

        //addon
        displayAlreadySelectedAddon(chip_group_user_selected_addon!!,cartItem)
        val dialog = builder.create()
        dialog.show()

        //custom
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setGravity(Gravity.CENTER)

        btn_cancel.setOnClickListener{dialog.dismiss()}
        btn_ok.setOnClickListener{
            //DELETE ITEM
            cartDataSource!!.deleteCart(cartItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object:SingleObserver<Int>{
                    override fun onSuccess(t: Int) {
                        //despuess de eliminar , tenemos que actualizar la data y agregar al carrito otra vez
                        if(Common.foodModelSelected!!.userSelectedAddon != null)
                            cartItem.foodAddon = Gson().toJson(Common.foodModelSelected!!.userSelectedAddon)
                        else
                            cartItem.foodAddon = "Default"
                        if (Common.foodModelSelected!!.userSelectedSize != null)
                            cartItem.foodSize = Gson().toJson(Common.foodModelSelected!!.userSelectedSize)
                        else
                            cartItem.foodSize = "Default"

                        cartItem.foodExtraPrice = Common.calculateExtraPrice(Common.foodModelSelected!!.userSelectedSize!!,
                            Common.foodModelSelected!!.userSelectedAddon)

                        compositeDisposable.add(cartDataSource!!.insertOrReplace(cartItem)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                dialog.dismiss()
                                calculateTotalPrice()
                                EventBus.getDefault().postSticky(CounterCartEvent(true))
                                Toast.makeText(context,"Actualizado correctamente",Toast.LENGTH_LONG).show()

                            },{
                                    t: Throwable? ->
                                Toast.makeText(context,"[INSERT ERROR]"+t!!.message,Toast.LENGTH_LONG).show()
                            }))

                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context,"CF-DC"+e.message,Toast.LENGTH_LONG).show()
                    }

                })
        }



    }

    private fun displayAlreadySelectedAddon(chipGroupUserSelectedAddon: ChipGroup, cartItem: CartItem)
    {
        if(cartItem.foodAddon != null){
            val addonModels:List<AddonModel> = Gson().fromJson(cartItem.foodAddon,object:TypeToken<List<AddonModel>>(){}.type)
            Common.foodModelSelected!!.userSelectedAddon = addonModels.toMutableList()
            chipGroupUserSelectedAddon.removeAllViews()
            for(addonModel in addonModels)
            {
                val chip = layoutInflater.inflate(R.layout.layout_chip_with_delete,null) as Chip
                chip.text = StringBuilder(addonModel.name).append("(+$")
                    .append(addonModel.price).append(")")
                chip.isCheckable = false
                 chip.setOnCloseIconClickListener{view->
                     chipGroupUserSelectedAddon.removeView(view)
                     Common.foodModelSelected!!.userSelectedAddon!!.remove(addonModel)
                     calculateTotalPrice()
                 }
                chipGroupUserSelectedAddon.addView(chip)

            }

        }

    }

    private fun displayAddonList() {
        if(Common.foodModelSelected!!.addon != null && Common.foodModelSelected!!.addon.size > 0)
        {
            chip_group_addon!!.clearCheck()
            chip_group_addon!!.removeAllViews()
            edt_search_addon!!.addTextChangedListener(this)
            for(addonModel in Common.foodModelSelected!!.addon )
            {
                  val chip = layoutInflater.inflate(R.layout.layout_chip,null) as Chip
                  chip.text = StringBuilder(addonModel.name).append("(+$")
                    .append(addonModel.price).append(")")
                  chip.setOnCheckedChangeListener { buttonView, isChecked ->
                      if(isChecked)
                         if(Common.foodModelSelected!!.userSelectedAddon == null)
                             Common.foodModelSelected!!.userSelectedAddon = ArrayList()
                         Common.foodModelSelected!!.userSelectedAddon!!.add(addonModel)

                  }
                chip_group_addon!!.addView(chip)
            }

        }

    }

    override fun onSearchNotFound(message: String) {
        Toast.makeText(context,""+message, Toast.LENGTH_LONG).show()
    }

    override fun afterTextChanged(s: Editable?) {

    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
       chip_group_addon!!.clearCheck()
        chip_group_addon!!.removeAllViews()

        for(addonModel in Common.foodModelSelected!!.addon )
        {
            if(addonModel.name!!.toLowerCase().contains(s.toString().toLowerCase()))
            {
                val chip = layoutInflater.inflate(R.layout.layout_chip,null) as Chip
                chip.text = StringBuilder(addonModel.name).append("(+$")
                    .append(addonModel.price).append(")")
                chip.setOnCheckedChangeListener { buttonView, isChecked ->
                    if(isChecked)
                        if(Common.foodModelSelected!!.userSelectedAddon == null)
                            Common.foodModelSelected!!.userSelectedAddon = ArrayList()
                    Common.foodModelSelected!!.userSelectedAddon!!.add(addonModel)

                }
                chip_group_addon!!.addView(chip)

            }


        }
    }

}