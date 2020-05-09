package com.example.kotlineatv2.Callback

import android.view.View

interface IRecyclerItemClickListener {
    fun onItemClick(view:View,position:Int)
}