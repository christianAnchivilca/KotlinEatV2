package com.example.kotlineatv2

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.internal.operators.single.SingleObserveOn
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.app_bar_home.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class HomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var  navController :NavController
    private lateinit var cartDataSource : CartDataSource
    private  var drawer:DrawerLayout?=null
    private var dialog:AlertDialog?=null


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
        val navView: NavigationView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_menu, R.id.nav_food_detail,
                R.id.nav_cart
            ), drawer
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        var headerView = navView.getHeaderView(0)
        var txt_user = headerView.findViewById<TextView>(R.id.txt_user)
        Common.setSpanString ("Hey, ",Common.currentUser!!.name,txt_user)

        navView.setNavigationItemSelectedListener(object :NavigationView.OnNavigationItemSelectedListener{
            override fun onNavigationItemSelected(menu: MenuItem): Boolean {
                menu.isChecked = true
                drawer!!.closeDrawers()
                if (menu.itemId == R.id.nav_sign_out){
                    signOut()
                }
                else if(menu.itemId == R.id.nav_home)
                {
                    navController.navigate(R.id.nav_home)

                }
                else if(menu.itemId == R.id.nav_menu)
                {
                    navController.navigate(R.id.nav_menu)

                }
                else if(menu.itemId == R.id.nav_cart)
                {
                    navController.navigate(R.id.nav_cart)

                }


                return true
            }

        })


        countCartItem()

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
