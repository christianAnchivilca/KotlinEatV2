package com.example.kotlineatv2.Callback

import com.example.kotlineatv2.Database.CartItem
import com.example.kotlineatv2.Model.CategoryModel

interface ISearchCategoryCallbackListener {
    fun onSearchFound(category:CategoryModel,cartItem: CartItem)
    fun onSearchNotFound(message:String)

}