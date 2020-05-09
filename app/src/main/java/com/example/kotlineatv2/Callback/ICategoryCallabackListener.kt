package com.example.kotlineatv2.Callback

import com.example.kotlineatv2.Model.CategoryModel
import com.example.kotlineatv2.Model.PopularCategoryModel

interface ICategoryCallabackListener {

    fun onCategoryLoadSuccess(categoryModelList:List<CategoryModel>)
    fun onCategoryLoadFailed(message:String)
}