package com.example.kotlineatv2.ui.fooddetail

import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.andremion.counterfab.CounterFab
import com.bumptech.glide.Glide
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton
import com.example.kotlineatv2.Common.Common
import com.example.kotlineatv2.Database.CartDataSource
import com.example.kotlineatv2.Database.CartDatabase
import com.example.kotlineatv2.Database.CartItem
import com.example.kotlineatv2.Database.LocalCartDataSource
import com.example.kotlineatv2.EventBus.CounterCartEvent
import com.example.kotlineatv2.EventBus.MenuItemBack
import com.example.kotlineatv2.Model.CommentModel
import com.example.kotlineatv2.Model.FoodModel
import com.example.kotlineatv2.R
import com.example.kotlineatv2.ui.comment.CommentFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import com.google.gson.Gson
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import java.lang.StringBuilder

class FoodDetailFragment : Fragment(),TextWatcher {
    override fun afterTextChanged(editable: Editable?) {

    }

    override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
        chip_group_addon!!.clearCheck()
        chip_group_addon!!.removeAllViews()
        for (addModel in Common.foodModelSelected!!.addon){
            if (addModel.name!!.toLowerCase().contains(charSequence.toString().toLowerCase())){
                val chip =layoutInflater.inflate(R.layout.layout_chip,null,false) as Chip
                chip.text = StringBuilder(addModel!!.name).append("($+").append(addModel.price).append(")").toString()
                chip.setOnCheckedChangeListener{compoundButton,b->
                    if (b){
                        if (Common.foodModelSelected!!.userSelectedAddon == null)
                            Common.foodModelSelected!!.userSelectedAddon = ArrayList()
                        Common.foodModelSelected!!.userSelectedAddon!!.add(addModel)
                    }
                }

                chip_group_addon!!.addView(chip)

            }

        }
    }

    private val compositeDisposable = CompositeDisposable()

    private lateinit var cartDataSource : CartDataSource

    private lateinit var foodDetailViewModel: FoodDetailViewModel

    private lateinit var addOnBottomSheetDialog:BottomSheetDialog

    var img_food:ImageView?=null
    var btnCart : CounterFab?=null
    var btnRating:FloatingActionButton?=null
    var ratingBar:RatingBar?=null
    var foodName:TextView?=null
    var foodPrice:TextView?=null
    var foodDescription:TextView?=null
    var numberButton:ElegantNumberButton?=null
    var bnShowCommet:Button?=null
    var radioGroupSize:RadioGroup?=null
    var rb_medium:RadioButton?=null
    var rb_large:RadioButton?=null
    private var waitingDialog:android.app.AlertDialog?=null
    var img_addon:ImageView?=null
     var chip_group_user_selected:ChipGroup?=null

    //layout addon
    var chip_group_addon:ChipGroup?=null
    private var edt_search_addon:EditText?=null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        foodDetailViewModel = ViewModelProviders.of(this).get(FoodDetailViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_food_detail, container, false)

        initView(root)

        foodDetailViewModel.getMutableLiveDataFood().observe(this, Observer {
            displayInfo(it)

        })
        foodDetailViewModel.getMutableLiveDataComment().observe(this, Observer {
            submitRatingToFirebase(it)
        })


        return root
    }

    private fun submitRatingToFirebase(commentModel: CommentModel?) {

        waitingDialog!!.show()
        //primero , obtenemos una referencia de firebase
         FirebaseDatabase.getInstance().getReference(Common.COMMENT_REFERENCE)
            .child(Common.foodModelSelected!!.id!!)
            .push()
             .setValue(commentModel)
             .addOnCompleteListener{
                 task ->
                 if (task.isSuccessful){
                     addRatingToFood(commentModel!!.ratingValue.toDouble())

                 }
                     waitingDialog!!.dismiss()

             }

    }

    private fun addRatingToFood(ratingValue: Double) {
       // System.out.println("addRating "+Common.category_selected!!.menu_id!!)
        FirebaseDatabase.getInstance()
            .getReference(Common.CATEGORY_REFERENCE) //  SELECT CATEGORY
            .child(Common.category_selected!!.menu_id!!) // SELECT MENU IN CATEGORY
            .child("foods") // select food array
            .child(Common.foodModelSelected!!.key!!) // select key
            // 1 de los 3 métodos para obtener sus datos de Firebase Realtime Database:
            .addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                    waitingDialog!!.dismiss()
                    Toast.makeText(context,""+p0.message,Toast.LENGTH_LONG).show()
                }

                override fun onDataChange(data: DataSnapshot) {
                    if (data.exists()){

                        val foodModel = data.getValue(FoodModel::class.java)
                        foodModel!!.key = Common.foodModelSelected!!.key
                        val sumRating = foodModel.ratingValue.toDouble() + ratingValue
                        val ratingCount = foodModel.ratingCount + 1

                       // val result = sumRating / ratingCount

                        val updateData = HashMap<String,Any>()
                        updateData["ratingValue"] = sumRating
                        updateData["ratingCount"] = ratingCount

                        //update data in variable
                        foodModel.ratingCount = ratingCount
                        foodModel.ratingValue = sumRating

                        data.ref
                            .updateChildren(updateData)
                            .addOnCompleteListener{task ->
                                waitingDialog!!.dismiss()
                                if (task.isSuccessful){
                                    Common.foodModelSelected = foodModel
                                    foodDetailViewModel!!.setFoodModel(foodModel)
                                    Toast.makeText(context,"Gracias por tu calificacion !",Toast.LENGTH_LONG).show()
                                }
                            }


                    }else{
                        waitingDialog!!.dismiss()
                    }

                }

            })




    }

    private fun displayInfo(foodModel:FoodModel?) {

        Glide.with(context!!).load(foodModel!!.image).into(img_food!!)
        foodName!!.text = StringBuilder(foodModel!!.name!!)
        foodPrice!!.text = StringBuilder(foodModel!!.price!!.toString())
        foodDescription!!.text = StringBuilder(foodModel!!.description!!)

        ratingBar!!.rating = foodModel!!.ratingValue.toFloat() / foodModel!!.ratingCount

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


        //POR DEFECTO EL PRIMER RADIO ESTARA SELECCIONADO
        if (radioGroupSize!!.childCount > 0){
            val radioButton1 = radioGroupSize!!.getChildAt(0) as RadioButton
            radioButton1.isChecked = true
        }

    }

    private fun calculateTotalPrice() {
        var totalPrice = Common.foodModelSelected!!.price.toDouble()
        var displayPrice = 0.0

        //Addon
        if (Common.foodModelSelected!!.userSelectedAddon != null && Common.foodModelSelected!!.userSelectedAddon!!.size > 0){

            for (addModel in Common.foodModelSelected!!.userSelectedAddon!!){

                totalPrice += addModel.price!!.toDouble()

            }

        }



        totalPrice += Common.foodModelSelected!!.userSelectedSize!!.price!!.toDouble()
        displayPrice = totalPrice * numberButton!!.number.toInt()
        displayPrice = Math.round(displayPrice * 100.0)/100.0

        foodPrice!!.text = StringBuilder("").append(Common.formatPrice(displayPrice)).toString()

    }

    private fun initView(root:View?) {
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context!!).cartDAO())

        addOnBottomSheetDialog = BottomSheetDialog(context!!,R.style.DialogStyle)
        val layout_user_selected_addon = layoutInflater.inflate(R.layout.layout_addon_display,null)
        chip_group_addon = layout_user_selected_addon.findViewById(R.id.chip_addon_display) as ChipGroup
        edt_search_addon = layout_user_selected_addon.findViewById(R.id.edt_search_addon) as EditText

        addOnBottomSheetDialog.setContentView(layout_user_selected_addon)
        addOnBottomSheetDialog.setOnDismissListener{
            dialogInterface ->

            displayUserSelectedAddon()
            calculateTotalPrice()

        }

        waitingDialog = SpotsDialog.Builder().setContext(context!!).setCancelable(false).build()
        btnCart = root!!.findViewById(R.id.btnCart) as CounterFab
        img_food = root!!.findViewById(R.id.img_food) as ImageView
        btnRating = root!!.findViewById(R.id.btn_rating) as FloatingActionButton
        foodName = root!!.findViewById(R.id.food_name) as TextView
        foodPrice = root!!.findViewById(R.id.food_price) as TextView
        foodDescription = root!!.findViewById(R.id.food_description) as TextView
        numberButton = root!!.findViewById(R.id.number_button) as ElegantNumberButton
        bnShowCommet = root!!.findViewById(R.id.btnShow_comment) as Button
        ratingBar = root!!.findViewById(R.id.ratingBar) as RatingBar
        radioGroupSize = root!!.findViewById(R.id.radio_group_size) as RadioGroup
        rb_medium = root!!.findViewById(R.id.rb_medium) as RadioButton
        rb_large = root!!.findViewById(R.id.rb_large) as RadioButton
        img_addon = root!!.findViewById(R.id.img_addon) as ImageView
        chip_group_user_selected = root!!.findViewById(R.id.chip_group_user_selected_addon) as ChipGroup

        img_addon!!.setOnClickListener {

            if (Common.foodModelSelected!!.addon != null){// existe addon
                displayAllAddOn()
                addOnBottomSheetDialog.show()
            }
        }

        btnRating!!.setOnClickListener {
              showDialogRating()
        }

        bnShowCommet!!.setOnClickListener{
            val commentFragmet = CommentFragment.getInstance()
            commentFragmet.show(activity!!.supportFragmentManager,"CommentFragment")

        }

        btnCart!!.setOnClickListener{


            val cartItem = CartItem()
            cartItem.uid = Common.currentUser!!.uid!!
            cartItem.userPhone = Common.currentUser!!.phone!!
            cartItem.foodId = Common.foodModelSelected!!.id!!
            cartItem.foodName = Common.foodModelSelected!!.name!!
            cartItem.foodImage = Common.foodModelSelected!!.image!!

            cartItem.foodPrice = Common.foodModelSelected!!.price!!.toDouble()
            cartItem.foodQuantity = numberButton!!.number.toInt()

            cartItem.foodExtraPrice =
                Common.calculateExtraPrice(Common.foodModelSelected!!.userSelectedSize!!,Common.foodModelSelected!!.userSelectedAddon)

            if (Common.foodModelSelected!!.userSelectedAddon != null)
                cartItem.foodAddon = Gson().toJson(Common.foodModelSelected!!.userSelectedAddon)
            else
                cartItem.foodAddon = "Default"

            if (Common.foodModelSelected!!.userSelectedSize != null)
                cartItem.foodSize = Gson().toJson(Common.foodModelSelected!!.userSelectedSize)
            else
                cartItem.foodSize = "Default"


            cartDataSource.getItemWithAllOptionsInCart(Common.currentUser!!.uid!!,
                cartItem.foodId,cartItem.foodSize!!,cartItem.foodAddon!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<CartItem> {
                    override fun onSuccess(cartItemFROMDB: CartItem) {

                        if (cartItemFROMDB.equals(cartItem)){

                            //si ya esta registrado en la base de datos, solo Actualizamos
                            cartItemFROMDB.foodExtraPrice = cartItem.foodExtraPrice
                            cartItemFROMDB.foodAddon = cartItem.foodAddon
                            cartItemFROMDB.foodSize = cartItem.foodSize
                            cartItemFROMDB.foodQuantity = cartItemFROMDB.foodQuantity + cartItem.foodQuantity

                            cartDataSource.updateCart(cartItemFROMDB)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(object: SingleObserver<Int> {
                                    override fun onSuccess(t: Int) {

                                        Toast.makeText(context,"Actualizado correctamente",Toast.LENGTH_LONG).show()
                                        EventBus.getDefault().postSticky(CounterCartEvent(true))

                                    }

                                    override fun onSubscribe(d: Disposable) {

                                    }

                                    override fun onError(e: Throwable) {
                                        Toast.makeText(context,"[UPDATE ERROR]"+e.message,Toast.LENGTH_LONG).show()
                                    }

                                })

                        }else{
                            //SI NO ESTA EN LA BASE DE DATOS , LO INSERTAMOS

                            compositeDisposable.add(cartDataSource.insertOrReplace(cartItem)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    Toast.makeText(context,"Agrregado correctamente",Toast.LENGTH_LONG).show()
                                    EventBus.getDefault().postSticky(CounterCartEvent(true))

                                },{
                                        t: Throwable? ->
                                    Toast.makeText(context,"[INSERT ERROR]"+t!!.message,Toast.LENGTH_LONG).show()
                                }))

                        }



                    }

                    override fun onSubscribe(d: Disposable) {
                    }

                    override fun onError(e: Throwable) {
                        if (e.message!!.contains("empty")){

                            compositeDisposable.add(cartDataSource.insertOrReplace(cartItem)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    Toast.makeText(context,"Agrregado correctamente",Toast.LENGTH_LONG).show()
                                    EventBus.getDefault().postSticky(CounterCartEvent(true))

                                },{
                                        t: Throwable? ->
                                    Toast.makeText(context,"[INSERT ERROR]"+t!!.message,Toast.LENGTH_LONG).show()
                                }))

                        }else

                            Toast.makeText(context,"[GET CART ERROR]"+e.message,Toast.LENGTH_LONG).show()

                    }

                })



        }


    }

    private fun displayAllAddOn() {
        if (Common.foodModelSelected!!.addon!!.size > 0){
             chip_group_addon!!.clearCheck()
            chip_group_addon!!.removeAllViews()
            edt_search_addon!!.addTextChangedListener( this)

            for (addModel in Common.foodModelSelected!!.addon){

                    val chip =layoutInflater.inflate(R.layout.layout_chip,null,false) as Chip
                    chip.text = StringBuilder(addModel!!.name).append("(+$").append(addModel.price).append(")").toString()
                    chip.setOnCheckedChangeListener{ compoundButton , b ->
                        if (b){
                            if (Common.foodModelSelected!!.userSelectedAddon == null)
                                Common.foodModelSelected!!.userSelectedAddon = ArrayList()
                            Common.foodModelSelected!!.userSelectedAddon!!.add(addModel)
                        }
                    }

                    chip_group_addon!!.addView(chip)
            }
        }
    }

    private fun displayUserSelectedAddon() {
       if (Common.foodModelSelected!!.userSelectedAddon != null && Common.foodModelSelected!!.userSelectedAddon!!.size >0){

           chip_group_user_selected!!.removeAllViews()
           for (addModel in Common.foodModelSelected!!.userSelectedAddon!!){
               val chip = layoutInflater.inflate(R.layout.layout_chip_with_delete,null,false) as Chip
               chip.text = StringBuilder(addModel!!.name).append("(+$").append(addModel.price).append(")").toString()
               chip.isClickable = false
               chip.setOnCloseIconClickListener{view ->
                   chip_group_user_selected!!.removeView(view)
                   Common.foodModelSelected!!.userSelectedAddon!!.remove(addModel)
                   calculateTotalPrice()
               }
               chip_group_user_selected!!.addView(chip)
           }

       }else{
           chip_group_user_selected!!.removeAllViews()

       }
    }

    private fun showDialogRating() {
        var builder = AlertDialog.Builder(context!!)
        builder.setTitle("Rating Food")
        builder.setMessage("Por favor complete la informacion")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_rating_comment,null)
        val ratingBarComment = itemView.findViewById<RatingBar>(R.id.rating_bar_comment)
        val txtComent = itemView.findViewById<EditText>(R.id.edt_comment)

        builder.setView(itemView)
        builder.setNegativeButton("CANCEL"){dialogInterface,i->dialogInterface.dismiss()}
        builder.setPositiveButton("OK"){
                dialogInterface,i->

                val commentModel = CommentModel()
                commentModel.name = Common.currentUser!!.name
                commentModel.uid = Common.currentUser!!.uid
                commentModel.comment = txtComent.text.toString()
                commentModel.ratingValue =ratingBarComment.rating
                 var serverTimeStamp = HashMap<String,Any>()
            serverTimeStamp["timeStamp"] = ServerValue.TIMESTAMP
            commentModel.commentTimeStamp = serverTimeStamp
            foodDetailViewModel!!.setCommentModel(commentModel)

            }

        val dialog = builder.create()
        dialog.show()

    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }
}