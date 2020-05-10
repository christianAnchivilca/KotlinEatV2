package com.example.kotlineatv2.Remote

import com.example.kotlineatv2.Model.BraintreeToken
import com.example.kotlineatv2.Model.BraintreeTransaction
import io.reactivex.Observable
import retrofit2.http.Field
import retrofit2.http.GET
import retrofit2.http.POST


interface ICloudFunctions{

    @GET("token")
    fun getToken():Observable<BraintreeToken>

    @POST("checkout")
    fun submitPayment(@Field("amount") amount:Double,
                      @Field("payment_method_nonce") nonce:String):Observable<BraintreeTransaction>

}