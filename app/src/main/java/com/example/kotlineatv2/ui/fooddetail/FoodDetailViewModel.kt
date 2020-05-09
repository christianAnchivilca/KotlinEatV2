package com.example.kotlineatv2.ui.fooddetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kotlineatv2.Common.Common
import com.example.kotlineatv2.Model.CommentModel
import com.example.kotlineatv2.Model.FoodModel

class FoodDetailViewModel : ViewModel() {

    private var mutableLiveDataFood:MutableLiveData<FoodModel>? = null
    private var mutableLiveDataCommentModel:MutableLiveData<CommentModel>? = null

    init {
        mutableLiveDataCommentModel = MutableLiveData()
    }

    fun getMutableLiveDataFood():MutableLiveData<FoodModel>{
        if(mutableLiveDataFood == null)
            mutableLiveDataFood = MutableLiveData()
        mutableLiveDataFood!!.value = Common.foodModelSelected
        return mutableLiveDataFood!!

    }


    fun getMutableLiveDataComment():MutableLiveData<CommentModel>{
        if(mutableLiveDataCommentModel == null)
            mutableLiveDataCommentModel = MutableLiveData()

        return mutableLiveDataCommentModel!!

    }

    fun setCommentModel(commentModel: CommentModel) {
        if (mutableLiveDataCommentModel != null)

            mutableLiveDataCommentModel!!.value = commentModel


    }

    fun setFoodModel(foodModel: FoodModel) {
        if (mutableLiveDataFood != null)
            mutableLiveDataFood!!.value = foodModel

    }

}