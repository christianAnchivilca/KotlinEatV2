package com.example.kotlineatv2.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlineatv2.Callback.IRecyclerItemClickListener
import com.example.kotlineatv2.Model.RestaurantModel
import com.example.kotlineatv2.R
import org.w3c.dom.Text

class MyRestaurantAdapter(internal var context: Context,
                          internal var listRestaurant:List<RestaurantModel>):RecyclerView.Adapter<MyRestaurantAdapter.MyViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_restaurant,parent,false))
    }

    override fun getItemCount(): Int {
        return listRestaurant.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        Glide.with(context).load(listRestaurant.get(position).imageUrl).into(holder.restaurant_image!!)
        holder.restaurant_name!!.setText(listRestaurant.get(position).name)
        holder.restaurant_address!!.setText(listRestaurant.get(position).address)
        holder.setListener(object:IRecyclerItemClickListener{
            override fun onItemClick(view: View, position: Int) {
              //code late
            }

        })

    }

    inner class MyViewHolder(itemView:View):RecyclerView.ViewHolder(itemView), View.OnClickListener {


        var restaurant_image: ImageView?=null
        var restaurant_address : TextView?=null
        var restaurant_name : TextView?=null
        internal var listener: IRecyclerItemClickListener?=null

        fun setListener(iReclycler: IRecyclerItemClickListener){

            this.listener = iReclycler
        }

        override fun onClick(view: View?) {

            listener!!.onItemClick(view!!,adapterPosition)
        }


        init {
            restaurant_name = itemView.findViewById(R.id.txt_restaurant_name) as TextView
            restaurant_address = itemView.findViewById(R.id.txt_restaurant_address) as TextView
            restaurant_image = itemView.findViewById(R.id.img_restaurant) as ImageView
            itemView.setOnClickListener(this)
        }

    }
}