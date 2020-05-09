package com.example.kotlineatv2.Callback

import com.example.kotlineatv2.Model.BestDealModel
import com.example.kotlineatv2.Model.PopularCategoryModel

interface IBestDealLoadCallback {

    fun onBestDealLoadSuccess(popularBestDealModelList:List<BestDealModel>)
    fun onBestDealLoadFailed(message:String)

}