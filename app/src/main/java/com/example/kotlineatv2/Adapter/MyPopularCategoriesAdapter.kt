package com.example.kotlineatv2.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.bumptech.glide.Glide
import com.example.kotlineatv2.Callback.IRecyclerItemClickListener
import com.example.kotlineatv2.EventBus.PopularFoodItemClick
import com.example.kotlineatv2.Model.PopularCategoryModel
import com.example.kotlineatv2.R
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.layout_popular_categories_item.view.*
import org.greenrobot.eventbus.EventBus

class MyPopularCategoriesAdapter(internal var context: Context,
                                 internal var popularCategoryModels:List<PopularCategoryModel>) :
    RecyclerView.Adapter<MyPopularCategoriesAdapter.MyViewHolder>(){


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_popular_categories_item,parent,false))

    }

    override fun getItemCount(): Int {
       return popularCategoryModels.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(popularCategoryModels.get(position).image).into(holder.category_image!!)
        holder.txt_category_name!!.setText(popularCategoryModels.get(position).name)

        holder.setListener(object :IRecyclerItemClickListener{
            override fun onItemClick(view: View, position: Int) {
                EventBus.getDefault().postSticky(PopularFoodItemClick(popularCategoryModels[position]))

            }

        })



    }


    inner class MyViewHolder(itemView:View):RecyclerView.ViewHolder(itemView),View.OnClickListener{
        override fun onClick(v: View?) {
            listener!!.onItemClick(v!!,adapterPosition)
        }


        var txt_category_name : TextView?=null


        var category_image: CircleImageView?=null

        internal var listener: IRecyclerItemClickListener?=null

        fun setListener(iReclycler: IRecyclerItemClickListener){

            this.listener = iReclycler
        }



       init {


           txt_category_name = itemView.findViewById(R.id.txt_category_name) as TextView
           category_image = itemView.findViewById(R.id.category_image) as CircleImageView
           itemView.setOnClickListener(this)

       }


    }


}