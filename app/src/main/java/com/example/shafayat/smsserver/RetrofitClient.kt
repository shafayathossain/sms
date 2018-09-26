package com.example.shafayat.smsserver

import com.google.gson.GsonBuilder
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Retrofit



class RetrofitClient{

    private val BASE_URL = "http://sms.greenweb.com.bd/api.php/"
    lateinit var retrofit : Retrofit

    private fun RetrofitApiClient() {}


}