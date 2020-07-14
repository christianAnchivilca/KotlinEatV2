package com.example.kotlineatv2.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlineatv2.Callback.IRecyclerItemClickListener
import com.example.kotlineatv2.Common.Common
import com.example.kotlineatv2.Database.*
import com.example.kotlineatv2.EventBus.CounterCartEvent
import com.example.kotlineatv2.EventBus.FoodItemClick

import com.example.kotlineatv2.Model.FoodModel
import com.example.kotlineatv2.R
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus


class MyFoodListAdapter (internal var context: Context,
                           internal var foodList:List<FoodModel>) :
    RecyclerView.Adapter<MyFoodListAdapter.MyViewHolder>(){

    private val compositeDisposable:CompositeDisposable
    private val cartDataSource : CartDataSource

    init {

        compositeDisposable = CompositeDisposable()
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context).cartDAO())


    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context!!).inflate(R.layout.layout_food_item,parent,false))
    }

    override fun getItemCount(): Int {
        return foodList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(foodList.get(position).image).into(holder.img_food_image!!)
        holder.txt_food_name!!.setText(foodList.get(position).name)
        holder.txt_food_price!!.setText(foodList.get(position).price.toString())
        //Event
        holder.setListener(object : IRecyclerItemClickListener {
            override fun onItemClick(view: View, position: Int) {
               Common.foodModelSelected = foodList.get(position)
                Common.foodModelSelected!!.key = position.toString()
                EventBus.getDefault().postSticky(FoodItemClick(true,foodList.get(position)))

            }

        })

        holder.img_cart!!.setOnClickListener{
            val cartItem = CartItem()
            cartItem.uid = Common.currentUser!!.uid!!
            cartItem.categoryId = Common.category_selected!!.menu_id!!
            cartItem.userPhone = Common.currentUser!!.phone!!
            cartItem.foodId = foodList.get(position).id!!
            cartItem.foodName = foodList.get(position).name!!
            cartItem.foodImage = foodList.get(position).image!!
            cartItem.foodPrice = foodList.get(position).price!!.toDouble()
            cartItem.foodQuantity = 1
            cartItem.foodExtraPrice = 0.0
            cartItem.foodAddon = "Default"
            cartItem.foodSize = "Default"

            cartDataSource.getItemWithAllOptionsInCart(Common.currentUser!!.uid!!,
                cartItem.categoryId,
                cartItem.foodId,cartItem.foodSize!!,cartItem.foodAddon!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object :SingleObserver<CartItem>{
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
                                .subscribe(object:SingleObserver<Int>{
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

    fun onStop(){
        //Atómicamente borra el contenedor, luego desecha todos los Desechables previamente contenidos.
        if (compositeDisposable != null)
           compositeDisposable.clear()
    }



    inner class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        override fun onClick(v: View?) {
            listener!!.onItemClick(v!!,adapterPosition)
        }


        var txt_food_name : TextView? = null
        var txt_food_price:TextView? = null
        var img_food_image: ImageView? = null
        var img_favorite: ImageView? = null
        var img_cart: ImageView? = null

        internal var listener: IRecyclerItemClickListener?=null

        fun setListener(iReclycler: IRecyclerItemClickListener){

            this.listener = iReclycler
        }


        init {
            txt_food_name = itemView.findViewById(R.id.txt_food_name) as TextView
            txt_food_price = itemView.findViewById(R.id.txt_food_price) as TextView
            img_food_image = itemView.findViewById(R.id.img_food_image) as ImageView
            img_favorite = itemView.findViewById(R.id.img_favorite) as ImageView
            img_cart = itemView.findViewById(R.id.img_quick_cart) as ImageView
            itemView.setOnClickListener(this)

        }

    }
}