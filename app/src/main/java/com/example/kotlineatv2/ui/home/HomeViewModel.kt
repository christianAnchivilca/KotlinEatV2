package com.example.kotlineatv2.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kotlineatv2.Callback.IBestDealLoadCallback
import com.example.kotlineatv2.Callback.IPopularLoadCallback
import com.example.kotlineatv2.Common.Common
import com.example.kotlineatv2.Model.BestDealModel
import com.example.kotlineatv2.Model.PopularCategoryModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeViewModel : ViewModel(), IPopularLoadCallback, IBestDealLoadCallback {


    override fun onBestDealLoadSuccess(popularBestDealModelList: List<BestDealModel>) {
        bestDealListMutableLiveData!!.value = popularBestDealModelList
    }

    override fun onBestDealLoadFailed(message: String) {
        messageError.value = message
    }

    override fun onPopularLoadSuccess(popularModelList: List<PopularCategoryModel>) {
        popularListMutableLiveData!!.value = popularModelList
    }

    override fun onPopularLoadFailed(message: String) {
        messageError.value = message
    }

    /*
    * LiveData es una clase de retención de datos observable. A diferencia de una clase observable regular,
    *  LiveData está optimizada para ciclos de vida, lo que significa que respeta el ciclo de vida de otros componentes de las apps,
    *  como actividades, fragmentos o servicios
    * */

    private var popularListMutableLiveData:MutableLiveData<List<PopularCategoryModel>>? = null
    private var bestDealListMutableLiveData:MutableLiveData<List<BestDealModel>>? = null
    private lateinit var messageError : MutableLiveData<String>
    private var popularLoadCallbackListener:IPopularLoadCallback
    private var bestDealCallback: IBestDealLoadCallback

    val bestDealList:LiveData<List<BestDealModel>>
    get() {
        if (bestDealListMutableLiveData == null){

            bestDealListMutableLiveData = MutableLiveData()
            messageError = MutableLiveData()
            loadBestDealList()
        }

        return bestDealListMutableLiveData!!
    }

    private fun loadBestDealList() {
        val tempList = ArrayList<BestDealModel>()
        val bestDealRef = FirebaseDatabase.getInstance().getReference(Common.BESTDEAL_REFERENCE)
        bestDealRef.addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {

                bestDealCallback.onBestDealLoadFailed(p0.message!!)

            }

            override fun onDataChange(p0: DataSnapshot) {
                for (item in p0!!.children){
                    val model = item.getValue<BestDealModel>(BestDealModel::class.java)
                    tempList.add(model!!)
                }

                bestDealCallback.onBestDealLoadSuccess(tempList)

            }

        })
    }

    val popularList:LiveData<List<PopularCategoryModel>>
    get() {
        if (popularListMutableLiveData == null){
            popularListMutableLiveData = MutableLiveData()
            messageError = MutableLiveData()
            loadPopularLis()
        }
        return popularListMutableLiveData!!
    }

    private fun loadPopularLis() {

        val tempList = ArrayList<PopularCategoryModel>()
        val popularRef = FirebaseDatabase.getInstance().getReference(Common.POPULAR_REFERENCE)


        popularRef.addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {

                popularLoadCallbackListener.onPopularLoadFailed(p0.message!!)

            }

            override fun onDataChange(p0: DataSnapshot) {
                for (item in p0!!.children){
                    val model = item.getValue<PopularCategoryModel>(PopularCategoryModel::class.java)
                    tempList.add(model!!)

                }

                popularLoadCallbackListener.onPopularLoadSuccess(tempList)
            }

        })


    }

    init {
        popularLoadCallbackListener = this
        bestDealCallback = this
    }


}