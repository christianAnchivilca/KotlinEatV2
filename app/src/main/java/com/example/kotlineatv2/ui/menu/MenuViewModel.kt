package com.example.kotlineatv2.ui.menu

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kotlineatv2.Callback.ICategoryCallabackListener
import com.example.kotlineatv2.Common.Common

import com.example.kotlineatv2.Model.CategoryModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MenuViewModel : ViewModel(), ICategoryCallabackListener {

    override fun onCategoryLoadSuccess(categoryModelList: List<CategoryModel>) {
        categoriesListMutable!!.value = categoryModelList
    }

    override fun onCategoryLoadFailed(message: String) {
        messageError.value = message
    }

    /*
    * Por lo general, MutableLiveData se usa en el ViewModel y, luego,
    *  el ViewModel solo expone objetos LiveData inmutables a los observadores.
    */

    private var categoriesListMutable : MutableLiveData<List<CategoryModel>>?=null
    private var messageError:MutableLiveData<String> = MutableLiveData()
    private val categoryCallbackListener : ICategoryCallabackListener

    init {
        categoryCallbackListener = this
    }

    fun getCategoryList():MutableLiveData<List<CategoryModel>>{
        if (categoriesListMutable == null)
        {
            categoriesListMutable = MutableLiveData()
            loadCategory()
        }

        return categoriesListMutable!!

    }



    fun getMessageError ():MutableLiveData<String>{
        return messageError
    }

    fun loadCategory(){
        val tempList = ArrayList<CategoryModel>()
        val categoryRef = FirebaseDatabase.getInstance().getReference(Common.CATEGORY_REFERENCE)
        categoryRef.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                categoryCallbackListener.onCategoryLoadFailed(p0.message!!)
            }

            override fun onDataChange(data: DataSnapshot) {
                for (item in data!!.children){
                    val model = item.getValue<CategoryModel>(CategoryModel::class.java)
                    model!!.menu_id = item.key
                    tempList.add(model!!)
                }

                categoryCallbackListener.onCategoryLoadSuccess(tempList)
            }

        })


    }




}