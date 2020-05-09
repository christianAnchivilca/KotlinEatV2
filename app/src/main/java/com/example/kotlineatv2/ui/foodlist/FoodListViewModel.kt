package com.example.kotlineatv2.ui.foodlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kotlineatv2.Common.*
import com.example.kotlineatv2.Model.FoodModel

class FoodListViewModel : ViewModel() {

    private var mutableFoodModelListaData : MutableLiveData<List<FoodModel>>? = null

    fun getMutableFoodModelListData(): MutableLiveData<List<FoodModel>>{
        if (mutableFoodModelListaData == null){
            mutableFoodModelListaData = MutableLiveData()
            mutableFoodModelListaData!!.value = Common.category_selected!!.foods

        }

            return mutableFoodModelListaData!!
    }


}