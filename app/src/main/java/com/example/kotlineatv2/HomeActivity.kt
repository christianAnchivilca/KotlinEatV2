package com.example.kotlineatv2

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import androidx.navigation.NavController
import com.example.kotlineatv2.Common.Common
import com.example.kotlineatv2.Database.CartDataSource
import com.example.kotlineatv2.Database.CartDatabase
import com.example.kotlineatv2.Database.LocalCartDataSource
import com.example.kotlineatv2.EventBus.*
import com.example.kotlineatv2.Model.CategoryModel
import com.example.kotlineatv2.Model.FoodModel
import com.example.kotlineatv2.Model.PopularCategoryModel
import com.example.kotlineatv2.Model.UserModel
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import dmax.dialog.SpotsDialog
import io.paperdb.Paper
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.internal.operators.single.SingleObserveOn
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.app_bar_home.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.collections.HashMap

class HomeActivity : AppCompatActivity() {

    // GOOGLE PLACES VARIABLES
    private var placeSelected:Place?=null
    private var places_fragment:AutocompleteSupportFragment?=null
    private lateinit var placeClient: PlacesClient
    private val placeFields = Arrays.asList(Place.Field.ID,
        Place.Field.NAME,
        Place.Field.ADDRESS,
        Place.Field.LAT_LNG)

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var  navController :NavController
    private lateinit var cartDataSource : CartDataSource
    private  var drawer:DrawerLayout?=null
    private var dialog:AlertDialog?=null

    private var navView: NavigationView?=null

    private var menuItemClick = -1


    override fun onResume() {
        super.onResume()
        countCartItem()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()

        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(this).cartDAO())

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            navController.navigate(R.id.nav_cart)
        }
        drawer = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_restaurant,
                R.id.nav_home, R.id.nav_menu, R.id.nav_food_detail,
                R.id.nav_cart
            ), drawer
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView!!.setupWithNavController(navController)

        var headerView = navView!!.getHeaderView(0)
        var txt_user = headerView.findViewById<TextView>(R.id.txt_user)
        Common.setSpanString ("Hey, ",Common.currentUser!!.name,txt_user)

        navView!!.setNavigationItemSelectedListener(object :NavigationView.OnNavigationItemSelectedListener{
            override fun onNavigationItemSelected(menu: MenuItem): Boolean {
                menu.isChecked = true
                drawer!!.closeDrawers()
                if (menu.itemId == R.id.nav_sign_out){
                    signOut()
                }
                else if(menu.itemId == R.id.nav_restaurant)
                {
                    if(menuItemClick != menu.itemId)
                        navController.navigate(R.id.nav_restaurant)

                }
                else if(menu.itemId == R.id.nav_home)
                {
                    if(menuItemClick != menu.itemId)
                        navController.navigate(R.id.nav_home)

                }
                else if(menu.itemId == R.id.nav_menu)
                {
                    if(menuItemClick != menu.itemId)
                       navController.navigate(R.id.nav_menu)

                }
                else if(menu.itemId == R.id.nav_cart)
                {
                    if(menuItemClick != menu.itemId)
                       navController.navigate(R.id.nav_cart)

                }
                else if(menu.itemId == R.id.nav_update_info)
                {
                    showUpdateInfoDialog()

                }
                else if(menu.itemId == R.id.nav_view_orders)
                {
                    if(menuItemClick != menu.itemId)
                       navController.navigate(R.id.nav_view_order)

                }
                else if(menu.itemId == R.id.nav_news)
                {
                    showNewsDialog()

                }


                menuItemClick =menu!!.itemId
                return true
            }

        })

        initPlacesClient()


        countCartItem()

    }

    private fun showNewsDialog() {

        Paper.init(this)

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("NUevo Sistema")
            .setMessage("¿Quieres suscribirte a nuevos envios?")
        val itemView=LayoutInflater.from(this).inflate(R.layout.layout_subscribe_news,null)
        builder.setView(itemView)
        val chk_subscribe = itemView.findViewById<View>(R.id.chk_subscribe_news) as CheckBox
        val isSubscribeNews = Paper.book().read<Boolean>(Common.IS_SUBSCRIBE_NEWS,false)
        if (isSubscribeNews) chk_subscribe.isChecked=true
        builder.setNegativeButton("CANCEL"){dialog: DialogInterface, which: Int -> dialog.dismiss() }

        builder.setPositiveButton("ACEPTAR"){dialog: DialogInterface, which: Int ->
            if (chk_subscribe.isChecked){
                Paper.book().write(Common.IS_SUBSCRIBE_NEWS,true)
                FirebaseMessaging.getInstance().subscribeToTopic(Common.NEWS_TOPIC)
                    .addOnFailureListener{e->
                        Toast.makeText(this@HomeActivity,""+e.message,Toast.LENGTH_LONG).show()
                    }.addOnSuccessListener {aVoid:Void?->

                        Toast.makeText(this@HomeActivity,"Te has suscrito exitosamente!",Toast.LENGTH_LONG).show()

                    }
            }
            else{

                Paper.book().delete(Common.IS_SUBSCRIBE_NEWS)
                FirebaseMessaging.getInstance().unsubscribeFromTopic(Common.NEWS_TOPIC)
                    .addOnFailureListener{e->
                        Toast.makeText(this@HomeActivity,""+e.message,Toast.LENGTH_LONG).show()
                    }.addOnSuccessListener {aVoid:Void?->

                        Toast.makeText(this@HomeActivity,"Te has desuscrito correctamente!",Toast.LENGTH_LONG).show()

                    }

            }

        }
        val dialog=builder.create()
        dialog.show()


    }

    private fun initPlacesClient() {
        Places.initialize(this,getString(R.string.google_maps_key))
        placeClient = Places.createClient(this)
    }

    private fun showUpdateInfoDialog() {

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Actualizar")
        builder.setMessage("Completa tu informacion")

        val itemView = LayoutInflater.from(this@HomeActivity)
            .inflate(R.layout.layout_register,null)
        val edt_name = itemView.findViewById<EditText>(R.id.edt_name)
        val edt_phone = itemView.findViewById<EditText>(R.id.edt_phone)
        val txt_address = itemView.findViewById<TextView>(R.id.txt_address_detail)

        places_fragment = supportFragmentManager
            .findFragmentById(R.id.places_autocomplete_fragment) as AutocompleteSupportFragment
        places_fragment!!.setPlaceFields(placeFields)
        places_fragment!!.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onPlaceSelected(p0: Place) {
                placeSelected = p0
                txt_address.text = placeSelected!!.address
            }

            override fun onError(p0: Status) {

                Toast.makeText(this@HomeActivity,""+p0.statusMessage,Toast.LENGTH_LONG).show()

            }

        })

        //set
        edt_phone.setText(Common.currentUser!!.phone)
        txt_address.setText(Common.currentUser!!.address)
        edt_name.setText(Common.currentUser!!.name)

        //Aqui seteamos nuestro formulario de registro dentro del AlertDialog
        builder.setView(itemView)
        builder.setNegativeButton("CANCEL"){dialogInterface, i -> dialogInterface.dismiss() }
        builder.setPositiveButton("UPDATE"){dialogInterface, i ->

            if (placeSelected != null){

                if(TextUtils.isDigitsOnly(edt_name.text.toString()) ) {
                    Toast.makeText(this@HomeActivity,"Please enter your name",Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                //Actualizar info of user
                val update_data = HashMap<String,Any>()
                update_data.put("name",edt_name.text.toString())
                update_data.put("address",txt_address.text.toString())
                update_data.put("phone",edt_phone.text.toString())
                update_data.put("lat",placeSelected!!.latLng!!.latitude)
                update_data.put("lng",placeSelected!!.latLng!!.longitude)

                FirebaseDatabase.getInstance()
                    .getReference(Common.USER_REFERENCE)
                    .child(Common.currentUser!!.uid!!)
                    .updateChildren(update_data)
                    .addOnFailureListener{
                        Toast.makeText(this@HomeActivity,""+it.message,Toast.LENGTH_LONG).show()
                    }
                    .addOnSuccessListener {
                        //update common currentUser
                        Common.currentUser!!.name = update_data["name"].toString()
                        Common.currentUser!!.address = update_data["address"].toString()
                        Common.currentUser!!.phone = update_data["phone"].toString()
                        Common.currentUser!!.lat = update_data["lat"].toString().toDouble()
                        Common.currentUser!!.lng = update_data["lng"].toString().toDouble()

                        Toast.makeText(this@HomeActivity,"Update info success",Toast.LENGTH_LONG).show()
                    }
            }else {
                Toast.makeText(this@HomeActivity,"Please select address",Toast.LENGTH_LONG).show()
            }


        }

        val dialogo1 = builder.create()
        dialogo1.setOnDismissListener {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.remove(places_fragment!!)
            fragmentTransaction.commit()
        }
        dialogo1.show()

    }

    private fun signOut() {
        //ABRIMOS DIALOGO PARA PREGUNTAR SI DESEA CERRAR LA SESION
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Cerrar Sesion")
                .setMessage("¿Estas seguro de cerrar la sesion?")
                .setNegativeButton("cancelar",{dialogInterface,_ ->dialogInterface.dismiss()})
                .setPositiveButton("OK"){dialogInterface,_ ->
                    Common.foodModelSelected = null
                    Common.category_selected = null
                    Common.currentUser = null
                    FirebaseAuth.getInstance().signOut()

                    val intent = Intent(this@HomeActivity,MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                }
        val dialog = builder.create()
        dialog.show()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {

        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onCategorySelected(event:CategoryClick){
        if (event.isSuccess){
            //Toast.makeText(this,""+event.category.name,Toast.LENGTH_LONG).show()
            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_food_list)
        }

    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onFoodSelected(event:FoodItemClick){
        if (event.isSuccess){
            //Toast.makeText(this,""+event.category.name,Toast.LENGTH_LONG).show()
            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_food_detail)
        }

    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onCountCartEvent(event:CounterCartEvent){
        if (event.isSuccess){
            countCartItem()

        }

    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onPopularFoodItemClick(event:PopularFoodItemClick){
        if (event.popularCategoryModel != null){

             dialog!!.show()
             FirebaseDatabase.getInstance().getReference(Common.CATEGORY_REFERENCE)
                 .child(event.popularCategoryModel!!.menu_id!!)
                 .addListenerForSingleValueEvent(object :ValueEventListener{
                     override fun onCancelled(p0: DatabaseError) {
                         dialog!!.dismiss()
                         Toast.makeText(this@HomeActivity,""+p0.message,Toast.LENGTH_LONG).show()
                     }

                     override fun onDataChange(data: DataSnapshot) {
                         if (data.exists())
                         {


                                 //var key:String = data.getKey().toString()
                             Common.category_selected = data.getValue(CategoryModel::class.java)
                             Common.category_selected!!.menu_id = data.key


                             //load food
                             FirebaseDatabase.getInstance().getReference(Common.CATEGORY_REFERENCE)
                                 .child(event.popularCategoryModel!!.menu_id!!)
                                 .child("foods")
                                 .orderByChild("id")
                                 .equalTo(event.popularCategoryModel.food_id)
                                 .limitToLast(1)
                                 .addListenerForSingleValueEvent(object :ValueEventListener{
                                     override fun onCancelled(p0: DatabaseError) {
                                         dialog!!.dismiss()
                                         Toast.makeText(this@HomeActivity,""+p0.message,Toast.LENGTH_LONG).show()
                                     }

                                     override fun onDataChange(p0: DataSnapshot) {
                                         if (p0.exists()){
                                             dialog!!.dismiss()

                                            //for (foodSnapShot in p0.children)

                                              for (fodSnapShot in p0.children){
                                                  Common.foodModelSelected = fodSnapShot.getValue<FoodModel>(FoodModel::class.java)
                                                  Common.foodModelSelected!!.key = fodSnapShot.key

                                              }
                                             navController.navigate(R.id.nav_food_detail)



                                         }else{
                                             dialog!!.dismiss()
                                             Toast.makeText(this@HomeActivity,"item no existe",Toast.LENGTH_LONG).show()

                                         }
                                     }

                                 })

                         }else{
                             dialog!!.dismiss()
                             Toast.makeText(this@HomeActivity,"no existe el item",Toast.LENGTH_LONG).show()
                         }

                     }

                 })
        }

    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onBestDealItemClick(event:BestDealItemClick){
        if (event.bestDealModel != null){

            dialog!!.show()
            FirebaseDatabase.getInstance().getReference(Common.CATEGORY_REFERENCE)
                .child(event.bestDealModel!!.menu_id!!)
                .addListenerForSingleValueEvent(object :ValueEventListener{
                    override fun onCancelled(p0: DatabaseError) {
                        dialog!!.dismiss()
                        Toast.makeText(this@HomeActivity,""+p0.message,Toast.LENGTH_LONG).show()
                    }

                    override fun onDataChange(data: DataSnapshot) {
                        if (data.exists())
                        {

                            Common.category_selected = data.getValue<CategoryModel>(CategoryModel::class.java)
                            Common.category_selected!!.menu_id = data.key
                            //load food
                            FirebaseDatabase.getInstance().getReference(Common.CATEGORY_REFERENCE)
                                .child(event.bestDealModel!!.menu_id!!)
                                .child("foods")
                                .orderByChild("id")
                                .equalTo(event.bestDealModel.food_id)
                                .limitToLast(1)
                                .addListenerForSingleValueEvent(object :ValueEventListener{
                                    override fun onCancelled(p0: DatabaseError) {
                                        dialog!!.dismiss()
                                        Toast.makeText(this@HomeActivity,""+p0.message,Toast.LENGTH_LONG).show()
                                    }

                                    override fun onDataChange(p0: DataSnapshot) {
                                        if (p0.exists()){
                                            dialog!!.dismiss()

                                            for (foodSnapShot in p0.children)
                                            {
                                                Common.foodModelSelected = foodSnapShot.getValue<FoodModel>(FoodModel::class.java)
                                                Common.foodModelSelected!!.key = foodSnapShot.key
                                            }

                                            navController.navigate(R.id.nav_food_detail)

                                        }else{
                                            dialog!!.dismiss()
                                            Toast.makeText(this@HomeActivity,"item no existe",Toast.LENGTH_LONG).show()

                                        }
                                    }

                                })

                        }else{
                            dialog!!.dismiss()
                            Toast.makeText(this@HomeActivity,"no existe el item",Toast.LENGTH_LONG).show()
                        }

                    }

                })
        }

    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onHidenFabEvent(event:HidenFABCart){
        if (event.isHiden){
            fab.hide()

        }else{
            fab.show()
        }

    }


    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onMenuItemBack(event:MenuItemBack){
        menuItemClick = -1
        if(supportFragmentManager.backStackEntryCount > 0)//si hay mas de un fragmento
            //Ahora popBackStack()revierte su última transacción que ha agregado a BackStack.
            supportFragmentManager.popBackStack();

        //Significado de "revertir":
        //intr. Volver una cosa al estado o condición que tuvo antes.


    }

    private fun countCartItem() {
        cartDataSource!!.countItemInCart(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object:SingleObserver<Int>{
                override fun onSuccess(t: Int) {
                    fab.count = t
                }

                override fun onSubscribe(d: Disposable) {
                }

                override fun onError(e: Throwable) {
                    if (!e.message!!.contains("Query returned empty"))
                        Toast.makeText(this@HomeActivity,""+e.message,Toast.LENGTH_LONG).show()
                    else
                        fab.count = 0
                }

            })

    }


}
