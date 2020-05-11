package com.example.kotlineatv2.ui.cart

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
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
import com.example.kotlineatv2.Common.Common
import com.example.kotlineatv2.Common.MySwipeHelper
import com.example.kotlineatv2.Database.CartDataSource
import com.example.kotlineatv2.Database.CartDatabase
import com.example.kotlineatv2.Database.CartItem
import com.example.kotlineatv2.Database.LocalCartDataSource
import com.example.kotlineatv2.EventBus.CounterCartEvent
import com.example.kotlineatv2.EventBus.HidenFABCart
import com.example.kotlineatv2.EventBus.UpdateItemInCart
import com.example.kotlineatv2.Model.OrderModel
import com.example.kotlineatv2.R
import com.example.kotlineatv2.Remote.ICloudFunctions
import com.example.kotlineatv2.Remote.RetrofitCloudClient
import com.google.android.gms.location.*
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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

class CartFragment : Fragment(), ILoadTimeFromFirebaseCallback {

    override fun onLoadTimeSuccess(order: OrderModel, estimatedTimeMs: Long) {

        order.createDate = estimatedTimeMs
        writeOrderToFirebase(order)
    }

    override fun onLoadTimeFailed(message: String) {
        Toast.makeText(context,message,Toast.LENGTH_SHORT).show()
    }


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

    private lateinit var locationRequest:LocationRequest
    private lateinit var locationCallback:LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentLocation:Location

    internal var address:String = ""
    internal var comment:String = ""
    lateinit var cloudFunctions:ICloudFunctions
    lateinit var listener:ILoadTimeFromFirebaseCallback


    override fun onResume() {
        super.onResume()
        calculateTotalPrice()
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

        setHasOptionsMenu(true) // important, if not add it , menu will never be inflate
        cloudFunctions = RetrofitCloudClient.getInstance().create(ICloudFunctions::class.java)
        listener = this

        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context!!).cartDAO())
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

                        }

                    }))

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
            val edt_address_dialog = view.findViewById<View>(R.id.edt_address_dialog) as EditText
            val edt_comment_dialog = view.findViewById<View>(R.id.edt_comment_dialog) as EditText
            val txt_address_detail = view.findViewById<View>(R.id.txt_address_detail) as TextView

            val rdi_home = view.findViewById<RadioButton>(R.id.rdi_home_address)
            val rdi_other_adress = view.findViewById<RadioButton>(R.id.rdi_other_address)
            val rdi_shipAddres = view.findViewById<RadioButton>(R.id.rdi_ship_this_address)
            val rdi_cod = view.findViewById<RadioButton>(R.id.rdi_cod)
            val rdi_braintree = view.findViewById<RadioButton>(R.id.rdi_braintree)
            edt_address_dialog.setText(Common.currentUser!!.address!!)

            rdi_home.setOnCheckedChangeListener{
                compoundButton,b->
                if (b){
                    edt_address_dialog.setText(Common.currentUser!!.address!!)
                    txt_address_detail.visibility = View.GONE
                }
            }
            rdi_other_adress.setOnCheckedChangeListener{
                    compoundButton,b->
                if (b){
                    edt_address_dialog.setText("")
                    edt_address_dialog.setHint("Ingrese su direccion")
                    txt_address_detail.visibility = View.GONE
                }
            }
            rdi_shipAddres.setOnCheckedChangeListener{
                    compoundButton , b ->
                if (b){
                    fusedLocationProviderClient!!.lastLocation
                        .addOnFailureListener{ e ->
                            txt_address_detail.visibility = View.GONE
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
                                        edt_address_dialog.setText(coordinates)
                                        txt_address_detail.visibility = View.VISIBLE
                                        txt_address_detail.setText(t)
                                    }

                                    override fun onError(e: Throwable) {
                                        edt_address_dialog.setText(coordinates)
                                        txt_address_detail.visibility = View.VISIBLE
                                        txt_address_detail.setText(e.message)
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
                    paymedCOD(edt_address_dialog.text.toString(),edt_comment_dialog.text.toString())
                }else if(rdi_braintree.isChecked){
                    address = edt_address_dialog.text.toString()
                    comment = edt_comment_dialog.text.toString()
                    //si no es nulo, si tenemos Token
                    if (!TextUtils.isEmpty(Common.currentToken))
                    {
                        val dropInRequest = DropInRequest().clientToken(Common.currentToken)
                        startActivityForResult(dropInRequest.getIntent(context!!),REQUEST_BRAINTREE_CODE)

                    }

                }


            }
            val crear = dialog.create()
            crear.show()

        }

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
                                Toast.makeText(context,"La orden se realizo exitosamente!",Toast.LENGTH_LONG).show()
                               // EventBus.getDefault().postSticky(CounterCartEvent(true))
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
                            Toast.makeText(context!!,e.message,Toast.LENGTH_LONG).show()

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

}