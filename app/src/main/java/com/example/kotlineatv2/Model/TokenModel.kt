package com.example.kotlineatv2.Model

class TokenModel {
    var uid:String?=null
    var token:String?=null

    constructor(){}

    constructor(uid:String,token:String){
        this.uid = uid
        this.token =  token

    }
}