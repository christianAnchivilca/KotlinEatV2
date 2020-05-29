package com.example.kotlineatv2.Common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.kotlineatv2.Model.*
import com.example.kotlineatv2.R
import com.example.kotlineatv2.Services.MyFCMServices
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
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

    fun updateToken(context:Context, token: String) {
        FirebaseDatabase.getInstance()
            .getReference(Common.TOKEN_REF)
            .child(Common.currentUser!!.uid!!)
            .setValue(TokenModel(Common.currentUser!!.phone!!,token))
            .addOnFailureListener{
                e->Toast.makeText(context,""+e.message,Toast.LENGTH_LONG).show()
            }


    }

    fun showNotification(context:Context, id: Int, title: String?, content: String?,intent:Intent?) {
        var pendingIntent:PendingIntent? = null
        if (intent != null)
            pendingIntent = PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT)
        val NOTIFICATION_CHANNEL_ID = "edmt.dev.eatitv2"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID,"Eat It v2",
                NotificationManager.IMPORTANCE_DEFAULT)

            notificationChannel.description="Eat It v2"
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
            notificationChannel.lightColor = (Color.RED)
            notificationChannel.vibrationPattern = longArrayOf(0,1000,500,1000)

            notificationManager.createNotificationChannel(notificationChannel)
        }

        val builder = NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID)
        builder.setContentTitle(title!!).setContentText(content)
            .setAutoCancel(true)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources,R.drawable.ic_restaurant_menu_black_24dp))
        if (pendingIntent != null)
            builder.setContentIntent(pendingIntent)
        val notification = builder.build()
        notificationManager.notify(id,notification)


    }

    const val NOTI_CONTENT: String = "content"
    const val NOTI_TITLE: String = "title"
    const val TOKEN_REF: String = "Tokens"
    var authorizeToken: String?=null
    var currentToken: String?=""
    const val ORDER_REFERENCE: String = "Order"
    const val COMMENT_REFERENCE: String = "Comments"
    val USER_REFERENCE:String = "Users"
    var currentUser:UserModel? = null
    const val POPULAR_REFERENCE:String = "MostPopular"
    const val BESTDEAL_REFERENCE:String = "BestDeals"
    val DEFAULT_COLUMN_COUNT:Int = 0
    val FULL_WIDTH_COLUMN:Int = 1
    const val CATEGORY_REFERENCE:String = "Category"
    var category_selected:CategoryModel?=null
    var foodModelSelected:FoodModel?=null
}