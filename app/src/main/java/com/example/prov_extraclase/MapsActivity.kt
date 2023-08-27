package com.example.prov_extraclase

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.prov_extraclase.databinding.ActivityMapsBinding

import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException
import java.util.Locale


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    // Variables para obtener ubicacion actual
    private lateinit var currentLocation: Location
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Se definen con un valor numerico unico para identificar el resultado de la solicitud
    private val permissionCode = 101
    private val requestLocationCode = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // "fusedLocationProviderClient" se usará para obtener la ultima ubicación conocida por el dispositivo
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        binding.mapsMenu.setTitle(R.string.SelectLocation)

        // Configuramos el boton de obtener ubicacion
        binding.btnCurrentLocation.setOnClickListener {
            getLocation()
        }

        // Configuramos el toolbar
        setSupportActionBar(binding.mapsMenu)

        // Agregamos el boton de regresar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Cambiar el color del icono del botón de navegación hacia atrás programáticamente
        val upArrow = ContextCompat.getDrawable(this, R.drawable.baseline_arrow_back_24)
        upArrow?.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_ATOP)
        supportActionBar?.setHomeAsUpIndicator(upArrow)

        // Funcion para ubicacion actual o seleccionada
        getLocation()
    }

    // Este metodo maneja el evento del botón de navegación hacia atrás
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Este metodo obtendrá la ubicacion actual del dispositivo
    fun getLocation() {

        // Se verifica si el usuario tiene los permisos de ubicacion
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                permissionCode
            )
            return

            // Se verificará si el GPS está activo
        } else if (!locationActivated()) {
            showLocationDialog()
            return
        }

        // Si la ubicacion fue activada se recibirá la ubicacion actual del usuario
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location

                // Se enviarán los datos del mapa al evento onMapReady()
                val mapFragment = supportFragmentManager
                    .findFragmentById(R.id.map) as SupportMapFragment
                mapFragment.getMapAsync(this)
            }
        }
        return
    }

    // Este metodo obtendra la respuesta para obtener los permisos de la ubicacion dependiendo del
    // codigo de la solicitud (request)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Si el usuario le da permisos de ubicación a la aplicacion
        when (requestCode) {
            permissionCode -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Si el usuario da permisos para usar la ubicacion se inicializará
                // nuevamente el metodo de getLocation()
                getLocation()
            }
        }
    }

    // Este metodo verificará si la ubicación o el GPS está activo
    fun locationActivated(): Boolean {
        val managerUbicacion = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return managerUbicacion.isProviderEnabled(LocationManager.GPS_PROVIDER) || managerUbicacion.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    // Este metodo mostrará el dialogo se solicitud de activar la ubicacion
    fun showLocationDialog() {
        val locationRequestBuilder = LocationSettingsRequest.Builder()
            .addLocationRequest(LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            })
        val settingsClient = LocationServices.getSettingsClient(this)
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(locationRequestBuilder.build())

        // Si la ubicación está activada, se llama a la función "getLocation()"
        locationSettingsResponseTask.addOnSuccessListener {
            getLocation()
        }

        // Si la ubicacion está desactivada, mostrará el dialogo
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Muestra el cuadro de diálogo de solicitud de ubicación del sistema
                    exception.startResolutionForResult(this, requestLocationCode)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Maneja el error al mostrar el cuadro de diálogo
                    sendEx.printStackTrace()
                }
            }
        }
    }

    // Este evento maneja la respuesta de una actividad basado en el codigo de la solicitud (request)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Si el usuario activa la ubicacion
        if (requestCode == requestLocationCode) {
            // Si el usuario permitió la activacion del GPS, se verificará si es así mediante el
            // metodo de locationActivated() y de ser así se inicializará nuevamente el metodo de
            // getLocation()
            if (locationActivated()) {
                getLocation()
            } else {
                Toast.makeText(applicationContext, R.string.PermissionNeeded, Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    // Este evento cargará los datos en el mapa
    override fun onMapReady(googleMap: GoogleMap) {
        // Mantendrá las coordenadas de la latitud y longitud de la ubicacíon
        val latitudLongitud = LatLng(currentLocation.latitude, currentLocation.longitude)
        // Marcador que se mostrará en el mapa
        val marker = MarkerOptions().position(latitudLongitud)

        // Moverá la camara del mapa hacia la ubicación señalada
        googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latitudLongitud))
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latitudLongitud, 7f))

        // Elimina el marcador existente, si es que hay proveniente del listener
        googleMap?.clear()

        // Añadirá el marcador en el mapa
        googleMap?.addMarker(marker)

        // Será el encargado de recibir el nombre de la ubicacion y mostrarlo al usuario en la
        // primera pantalla
        val locationName =
            getLocationName(this, currentLocation.latitude, currentLocation.longitude)

        // Se retornan los valores al MainActivity
        val intent = Intent()
        intent.putExtra("latitud", currentLocation.latitude.toString())
        intent.putExtra("longitud", currentLocation.longitude.toString())
        intent.putExtra("nombreLugar", locationName)
        setResult(RESULT_OK, intent);

        // Se establece el listener para el usuario seleccione una ubicación en el mapa
        googleMap?.setOnMapClickListener { newLocation ->

            // Elimina el marcador existente
            googleMap?.clear()

            // Se agrega un nuevo marcador en la ubicacion seleccionada
            val marker = MarkerOptions().position(newLocation)
            googleMap?.addMarker(marker)

            // Será el encargado de recibir el nombre de la ubicacion seleccionada y mostrarlo al usuario en la
            // primera pantalla
            val locationName =
                getLocationName(this, newLocation.latitude, newLocation.longitude)

            // Se retornan los valores al MainActivity
            val intent = Intent()
            intent.putExtra("latitud", newLocation.latitude.toString())
            intent.putExtra("longitud", newLocation.longitude.toString())
            intent.putExtra("nombreLugar", locationName)
            setResult(RESULT_OK, intent);
        }
    }

    // Este metodo obtendrá el nombre del lugar seleccionado basado en las coordenadas
    fun getLocationName(context: Context, latitude: Double, longitude: Double): String {

        // El metodo Geocoder de realizar procesos de geocodificación y geodecodificación
        // En este caso, geodecodificación para obtener el nombre de la ubicacion
        val geocoder = Geocoder(context, Locale.getDefault())
        var locationName = ""

        try {
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address: Address = addresses[0]
                // Se obtendrá el nombre del lugar
                locationName = address.getAddressLine(0)
            } else {
                // Si no se encontró nombre de la ubicación
                locationName = R.string.getLocationNameNotFound.toString()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            locationName = R.string.getLocationNameError.toString()
        }

        return locationName
    }
}