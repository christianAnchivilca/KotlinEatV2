package com.example.kotlineatv2.ui.restaurant

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kotlineatv2.Callback.ICategoryCallabackListener
import com.example.kotlineatv2.Callback.IRestaurantCallbackListener
import com.example.kotlineatv2.Common.Common
import com.example.kotlineatv2.Model.CategoryModel
import com.example.kotlineatv2.Model.RestaurantModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RestaurantViewModel : ViewModel(),IRestaurantCallbackListener {
    override fun onRestaurantLoadSuccess(restaurantModelList: List<RestaurantModel>) {
        restaurantListMutable!!.value = restaurantModelList
    }

    override fun onRestaurantLoadFailed(message: String) {
         messageError.value = message
    }

    /*
    * Por lo general, MutableLiveData se usa en el ViewModel y, luego,
    *  el ViewModel solo expone objetos LiveData inmutables a los observadores.
    */
    private var restaurantListMutable : MutableLiveData<List<RestaurantModel>>?=null
    private var messageError: MutableLiveData<String> = MutableLiveData()
    private val restaurantCallbackListener : IRestaurantCallbackListener
    init {
        restaurantCallbackListener = this
    }


    fun getRestaurantList():MutableLiveData<List<RestaurantModel>>{
        if (restaurantListMutable == null)
        {
            restaurantListMutable = MutableLiveData()
            loadRestaurant()
        }

        return restaurantListMutable!!

    }

    fun loadRestaurant(){
        val tempList = ArrayList<RestaurantModel>()
        val restaurantRef = FirebaseDatabase.getInstance().getReference(Common.RESTAURANT_REFERENCE)
        restaurantRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                restaurantCallbackListener.onRestaurantLoadFailed(p0.message!!)
            }

            override fun onDataChange(data: DataSnapshot) {
                if (data.exists()){
                    for (item in data!!.children){
                        val model = item.getValue<RestaurantModel>(RestaurantModel::class.java)
                        model!!.uid = item.key!!
                        tempList.add(model!!)
                    }
                    if (tempList.size > 0)
                        restaurantCallbackListener.onRestaurantLoadSuccess(tempList)
                    else
                        restaurantCallbackListener.onRestaurantLoadFailed("Lista de restaurant vacia")


                }else
                    restaurantCallbackListener.onRestaurantLoadFailed("No existe el restaurant")

            }

        })


    }

    fun getMessageError ():MutableLiveData<String>{
        return messageError
    }


}