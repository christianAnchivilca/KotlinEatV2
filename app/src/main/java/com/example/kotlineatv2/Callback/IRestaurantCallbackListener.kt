package com.example.kotlineatv2.Callback

import com.example.kotlineatv2.Model.CategoryModel
import com.example.kotlineatv2.Model.RestaurantModel

interface IRestaurantCallbackListener {
    fun onRestaurantLoadSuccess(restaurantModelList:List<RestaurantModel>)
    fun onRestaurantLoadFailed(message:String)
}