package com.example.kotlineatv2.Callback

import com.example.kotlineatv2.Model.CommentModel
import com.example.kotlineatv2.Model.OrderModel

interface ILoadOrderCallbackListener {
    fun onLoadOrderSuccess(orderList:List<OrderModel>)
    fun onLoadOrderFailed(message:String)
}