package com.example.kotlineatv2.Services

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.kotlineatv2.Common.Common
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.*

class MyFCMServices : FirebaseMessagingService(){

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Common.updateToken(this,token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val dataRecv = remoteMessage.data
        if (dataRecv != null ){

           if (dataRecv[Common.IS_SEND_IMAGE] != null && dataRecv[Common.IS_SEND_IMAGE].equals("true"))
           {
               Glide.with(this).asBitmap()
                   .load(dataRecv[Common.IMAGE_URL])
                   .into(object:CustomTarget<Bitmap>(){
                       override fun onLoadCleared(placeholder: Drawable?) {

                       }

                       override fun onResourceReady(
                           resource: Bitmap,
                           transition: Transition<in Bitmap>?
                       ) {

                           Common.showNotification(this@MyFCMServices,Random().nextInt(),
                               dataRecv[Common.NOTI_TITLE],
                               dataRecv[Common.NOTI_CONTENT],
                               resource,
                               null)

                       }

                   })


           }else{
               Common.showNotification(this,Random().nextInt(),
                   dataRecv[Common.NOTI_TITLE],
                   dataRecv[Common.NOTI_CONTENT],null)
           }

        }
    }
}