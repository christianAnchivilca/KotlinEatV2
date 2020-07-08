package com.example.kotlineatv2.Adapter

import android.content.Context
import android.media.Image
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlineatv2.R
import com.example.kotlineatv2.Model.AddonModel
import com.example.kotlineatv2.Database.CartItem
import com.example.kotlineatv2.Model.SizeModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.StringBuilder

class MyOrderDetailAdapter (internal var context:Context,internal
var cartItemList:MutableList<CartItem>):RecyclerView.Adapter<MyOrderDetailAdapter.MyViewHolder>() {

    val gson : Gson = Gson()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_order_detail_item,parent,false))
    }

    override fun getItemCount(): Int {
        return cartItemList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        Glide.with(context).load(cartItemList[position].foodImage).into(holder.img_foood!!)

        holder.txt_food_quantity!!.setText(StringBuilder("Cantidad: ")
            .append(cartItemList[position].foodQuantity))
        holder.txt_food_name!!.setText(cartItemList[position].foodName)

        val sizeModel:SizeModel = gson.fromJson(cartItemList[position].foodSize,object:TypeToken<SizeModel?>(){}.type)
        if (sizeModel != null) holder.txt_food_size!!.setText(StringBuilder("Tamaño: ").append(sizeModel.name))

        if (!cartItemList[position].foodAddon.equals("Default"))
        {
            val addonModels: List<AddonModel> = gson.fromJson(cartItemList[position].foodAddon,object:TypeToken<List<AddonModel?>?>(){}.type)
            val addonString = StringBuilder()
            if (addonModels != null)
            {
                for (addonModel in addonModels) addonString.append(addonModel.name).append(",")
                addonString.delete(addonString.length-1,addonString.length)// remove last ","
                holder.txt_food_addon!!.setText(StringBuilder("Addon: ").append(addonString))

            }
        }
        else
            holder.txt_food_addon!!.setText(StringBuilder("Adicional: Default"))




    }


    inner class MyViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){

        var img_foood :ImageView ?=null
        var txt_food_name :TextView ?=null
        var txt_food_addon :TextView ?=null
        var txt_food_size :TextView ?=null
        var txt_food_quantity :TextView ?=null

        init {
            img_foood = itemView.findViewById(R.id.img_food_image) as ImageView
            txt_food_name = itemView.findViewById(R.id.txt_food_name) as TextView
            txt_food_addon = itemView.findViewById(R.id.txt_food_addon) as TextView
            txt_food_size = itemView.findViewById(R.id.txt_size) as TextView
            txt_food_quantity = itemView.findViewById(R.id.txt_food_quantity) as TextView

        }



    }


}
