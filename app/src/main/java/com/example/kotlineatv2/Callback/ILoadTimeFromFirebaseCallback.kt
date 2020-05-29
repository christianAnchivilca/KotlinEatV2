package com.example.kotlineatv2.Callback

import com.example.kotlineatv2.Model.OrderModel
import com.example.kotlineatv2.Model.PopularCategoryModel

interface ILoadTimeFromFirebaseCallback {

    fun onLoadTimeSuccess(order:OrderModel,estimatedTimeMs:Long)
    fun onLoadTimeFailed(message:String)
}