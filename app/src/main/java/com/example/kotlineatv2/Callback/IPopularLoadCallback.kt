package com.example.kotlineatv2.Callback

import com.example.kotlineatv2.Model.PopularCategoryModel

interface IPopularLoadCallback {

    fun onPopularLoadSuccess(popularModelList:List<PopularCategoryModel>)
    fun onPopularLoadFailed(message:String)
}