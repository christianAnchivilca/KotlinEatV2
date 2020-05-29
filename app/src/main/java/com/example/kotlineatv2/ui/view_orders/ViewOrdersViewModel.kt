package com.example.kotlineatv2.ui.view_orders

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kotlineatv2.Model.OrderModel

class ViewOrdersViewModel : ViewModel() {

    val mutableLiveDataOrderList : MutableLiveData<List<OrderModel>>
    init {
        mutableLiveDataOrderList = MutableLiveData()
    }
    fun setMutableLiveDataOrderList(orderList:List<OrderModel>){
        mutableLiveDataOrderList.value = orderList

    }


}