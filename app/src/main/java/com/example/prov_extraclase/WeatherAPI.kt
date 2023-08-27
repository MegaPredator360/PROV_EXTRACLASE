package com.example.prov_extraclase

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface WeatherAPI {
    // Tipo de solicitud que queremos hacer en el API
    @GET
    // Regresará los datos del API
    fun getWeather(@Url url: String): Call<WeatherResponse>
}