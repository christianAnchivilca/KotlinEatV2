package com.example.kotlineatv2.Common

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.widget.TextView
import com.example.kotlineatv2.Model.*
import com.google.firebase.database.DatabaseReference
import java.lang.StringBuilder
import java.lang.reflect.Type
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.random.Random

object Common {
    fun formatPrice(price: Double): String {


        if (price != 0.toDouble()) {
            val df = DecimalFormat("#,##0.00")
            df.roundingMode = RoundingMode.HALF_UP
            val finalPrice = StringBuilder(df.format(price)).toString()
            return finalPrice.replace(".", ",")

        }else
            return "0,00"



    }

    fun calculateExtraPrice(userSelectedSize: SizeModel,
                            userSelectedAddon: MutableList<AddonModel>?): Double {
        var result :Double = 0.0


        if (userSelectedSize == null && userSelectedAddon == null)
            return 0.0
        else if (userSelectedSize == null){

            for (addModel in userSelectedAddon!!)
                result += addModel.price.toDouble()
             return result

        }else if(userSelectedAddon == null){
            result = userSelectedSize!!.price.toDouble()
            return result
        }else{

            result = userSelectedSize!!.price.toDouble()
            for (addModel in userSelectedAddon!!)
                result += addModel.price.toDouble()
            return result
        }
    }

    fun setSpanString(welcome: String, name: String?, txtUser: TextView?) {

        val builder = SpannableStringBuilder()
        builder.append(welcome)
        val txtSpannable = SpannableString(name)
        val boldSpan = StyleSpan(Typeface.BOLD)
        txtSpannable.setSpan(boldSpan,0,name!!.length,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.append(txtSpannable)
        txtUser!!.setText(builder,TextView.BufferType.SPANNABLE)



    }

    fun createOrderNumber(): String {
        return StringBuilder()
            .append(System.currentTimeMillis())
            .append(Math.abs(Random.nextInt()))
            .toString()

    }

    fun buildToken(authorizeToken: String?): String {
        return StringBuilder("Bearer").append(" ").append(authorizeToken).toString()
    }

    fun getDateOfWeek(get: Int): String {
        when(get){
            1 -> return "Lunes"
            2 -> return "Martes"
            3 -> return "Miercoles"
            4 -> return "Jueves"
            5 -> return "Viernes"
            6 -> return "Sabado"
            7 -> return "Domingo"
            else -> return "Unknow"


        }

    }

    fun convertStatusToText(orderStatus: Int): String {
        when(orderStatus){

            0->return "Placed"
            1->return "Shipping"
            2->return "Shipped"
            -1->return "Cancelled"
            else -> return "Unknow"

        }

    }

    var authorizeToken: String?=null
    var currentToken: String?=""
    val ORDER_REFERENCE: String = "Order"
    val COMMENT_REFERENCE: String = "Comments"
    val USER_REFERENCE:String = "Users"
    var currentUser:UserModel? = null
    val POPULAR_REFERENCE:String = "MostPopular"
    val BESTDEAL_REFERENCE:String = "BestDeals"
    val DEFAULT_COLUMN_COUNT:Int = 0
    val FULL_WIDTH_COLUMN:Int = 1
    val CATEGORY_REFERENCE:String = "Category"
    var category_selected:CategoryModel?=null
    var foodModelSelected:FoodModel?=null
}