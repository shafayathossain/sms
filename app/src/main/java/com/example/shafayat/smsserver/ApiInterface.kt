package com.example.shafayat.smsserver

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {

    @GET("")
    fun sendSms(@Query("token")token:String, @Query("to")number:String, @Query("message")body:String) : Call<Response>
}