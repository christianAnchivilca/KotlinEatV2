package com.example.kotlineatv2.Adapter

import android.content.Context
import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton
import com.example.kotlineatv2.Common.Common
import com.example.kotlineatv2.Database.CartDataSource
import com.example.kotlineatv2.Database.CartDatabase
import com.example.kotlineatv2.Database.CartItem
import com.example.kotlineatv2.Database.LocalCartDataSource
import com.example.kotlineatv2.EventBus.UpdateItemInCart
import com.example.kotlineatv2.Model.AddonModel
import com.example.kotlineatv2.Model.SizeModel
import com.example.kotlineatv2.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.disposables.CompositeDisposable
import org.greenrobot.eventbus.EventBus


class MyCartAdapter (internal var context: Context,internal var listCartItem:List<CartItem>):
    RecyclerView.Adapter<MyCartAdapter.MyViewHolder>(){

    internal var compositeDisposable:CompositeDisposable
    internal var cartDataSource:CartDataSource
    val gson:Gson

    init {
        compositeDisposable = CompositeDisposable()
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context!!).cartDAO())
        gson = Gson()

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_cart_item,parent,false))
    }

    override fun getItemCount(): Int {
       return listCartItem.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(listCartItem.get(position).foodImage).into(holder.img_car)
        holder.txt_food_name.text = StringBuilder(listCartItem[position].foodName!!)
        holder.txt_food_price.text = StringBuilder("").append(listCartItem[position].foodPrice + listCartItem[position].foodExtraPrice)
        holder.number_count.number = listCartItem[position].foodQuantity.toString()

        if(listCartItem[position].foodSize != null)
        {
            if(listCartItem[position].foodSize.equals("Default")) holder.txt_food_size.text =
                java.lang.StringBuilder("Size:Default ")
            else
            {
                // get sizeModel objeto
                val sizeModel=gson.fromJson<SizeModel>(listCartItem[position].foodSize,object:TypeToken<SizeModel>(){}.type)
                holder.txt_food_size.text = java.lang.StringBuilder("Size: ").append(sizeModel.name)

            }

        }

        if (listCartItem[position].foodAddon != null){
            if (listCartItem[position].foodAddon.equals("Default")) holder.txt_food_addon.text =
                java.lang.StringBuilder("Addon: Default")
            else{
                val addonModel = gson.fromJson<List<AddonModel>>(listCartItem[position].foodAddon,object:TypeToken<List<AddonModel>>(){}.type)
                holder.txt_food_addon.text = java.lang.StringBuilder("Addon: ").append(Common.getListAddon(addonModel))
            }
        }





        holder.number_count.setOnValueChangeListener{view,oldValue,newValue->
            listCartItem[position].foodQuantity = newValue

            EventBus.getDefault().postSticky(UpdateItemInCart(listCartItem[position]))
        }



    }

    fun getItemAtPosition(pos: Int): CartItem {
        return listCartItem[pos]

    }


    inner class MyViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){
        lateinit var img_car:ImageView
        lateinit var txt_food_name:TextView
        lateinit var txt_food_price:TextView
        lateinit var txt_food_addon:TextView
        lateinit var txt_food_size:TextView
        lateinit var number_count:ElegantNumberButton


        init {
            img_car = itemView.findViewById(R.id.img_cart) as ImageView
            txt_food_name = itemView.findViewById(R.id.txt_cart_food_name) as TextView
            txt_food_price = itemView.findViewById(R.id.txt_cart_food_price) as TextView
            txt_food_size = itemView.findViewById(R.id.txt_cart_food_size) as TextView
            txt_food_addon = itemView.findViewById(R.id.txt_cart_food_addon) as TextView
            number_count = itemView.findViewById(R.id.number_cart_button) as ElegantNumberButton
        }

    }
}