package com.example.kotlineatv2.Model

class UserModel {
    var uid:String?=null
    var name:String?=null
    var address:String?=null
    var phone:String?=null


    constructor(){}

    constructor(uid: String?, name: String?, address: String?, phone: String?) {
        this.uid = uid
        this.name = name
        this.address = address
        this.phone = phone
    }


}