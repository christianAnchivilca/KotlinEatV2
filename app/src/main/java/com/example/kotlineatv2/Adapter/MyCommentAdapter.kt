package com.example.kotlineatv2.Adapter

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlineatv2.Model.CommentModel
import com.example.kotlineatv2.R
import java.util.zip.Inflater

class MyCommentAdapter(internal var context:Context,
                       internal var commentList:List<CommentModel>) : RecyclerView.Adapter<MyCommentAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyCommentAdapter.MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context!!).inflate(R.layout.layout_comment_item,parent,false))
    }

    override fun getItemCount(): Int {
        return commentList.size
    }

    override fun onBindViewHolder(holder: MyCommentAdapter.MyViewHolder, position: Int) {

        val commentTimeStamp = commentList.get(position).commentTimeStamp!!["timeStamp"]!!.toString().toLong()

        holder.text_comment_name!!.text = commentList.get(position).name
        holder.text_comment_date!!.text = DateUtils.getRelativeTimeSpanString(commentTimeStamp)
        holder.txt_coment!!.text = commentList.get(position).comment
        holder.rating_bar!!.rating = commentList.get(position).ratingValue
    }

    inner class MyViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){

        var text_comment_name:TextView?=null
        var text_comment_date:TextView?=null
        var txt_coment:TextView?=null
        var rating_bar:RatingBar?=null
        init {
            text_comment_name = itemView.findViewById<TextView>(R.id.text_comment_name)
            text_comment_date = itemView.findViewById<TextView>(R.id.text_comment_date)
            txt_coment = itemView.findViewById<TextView>(R.id.txt_coment)
            rating_bar = itemView.findViewById(R.id.ratting_bar) as RatingBar
        }



    }
}