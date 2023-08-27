package com.example.prov_extraclase

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.prov_extraclase.databinding.ActivityMainBinding
import org.w3c.dom.Text
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private lateinit var binding: ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val apiKey = "30567643d06757fa18c0f0e3a67edc20"
    private val TAG = "CHECK_RESPONSE"
    private val baseUrl = "https://api.openweathermap.org/data/2.5/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ponemos el texto en los textViews
        binding.btnSelectLocation.setText(R.string.SelectLocation)
        binding.lblSelectedLocation.setText(R.string.SelectedLocation)
        binding.lblPlaceName.setText(R.string.PlaceName)
        binding.lblCurrentWeather.setText(R.string.CurrentWeather)
        binding.lblCurrentTemperature.setText(R.string.CurrentTemperature)

        // Configurar boton para buscar ubicación
        binding.btnSelectLocation.setOnClickListener {
                openMap()
            }
    }

    // Esta funcion abrirá la segunda pantalla
    fun openMap() {
        val intent = Intent(this, MapsActivity::class.java)
        // Este metodo será el encarga de regresar los datos que necesitamos de la segunda pantalla
        startActivityForResult(intent, 1)
    }

    // Metodo encargado de recibir un resultado basado en el codigo de la solicitud (request)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                val latitude = data?.getStringExtra("latitud")?.toDouble()
                val longitude = data?.getStringExtra("longitud")?.toDouble()
                binding.lblPlaceName.text = data?.getStringExtra("nombreLugar")

                getWeatherStatus(latitude, longitude, apiKey)
            }
        }
    }

    // Este metodo se encargará de obtener los datos del clima
    private fun getWeatherStatus(latitud: Double?, longitud: Double?, apiKey: String) {

        // Definimos la otra mitad del URL que será usado para obtener los datos
        val url = "weather?lat=$latitud&lon=$longitud&appid=$apiKey&units=metric"

        // Creamos el request
        val api = Retrofit.Builder()
                // Url
            .baseUrl(baseUrl)
                // Conversion de tipo JSON
            .addConverterFactory(GsonConverterFactory.create())
            .build()
                // Asociamos el create con el interface
            .create(WeatherAPI::class.java)

        // Llamamos a la funcion de getWeather de la interface y que queremos datos de tipo WeatherResponse
        // que serán llenados con la respuesta del API
        api.getWeather(url).enqueue(object: Callback<WeatherResponse> {

            // Si la respuesta del API es satisfactoria, se llama a la clase WeatherResponse para
            // llenar los datos de la respuesta del API
            override fun onResponse(
                call: Call<WeatherResponse>,
                response: Response<WeatherResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        // Se llaman a los TextViews y se le asignan los datos que recibimos del API
                        binding.lblWeather.text = it.weather[0].description.toString()
                        binding.lblTemperature.text = it.main.temp.toString() + " °C"
                    }
                }
            }

            // En caso de que ocurra un fallo de respuesta, el usuario recibira un mensaje en su pantalla
            // diciendo que hubo un error y se imprimirá en el Logcat el tipo de error
            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Toast.makeText(applicationContext, "Hubo un error al obtener los datos, por favor intenta más tarde", Toast.LENGTH_SHORT).show()
                Log.i(TAG, "onFailure: ${t.message}")
            }

        })
    }
}