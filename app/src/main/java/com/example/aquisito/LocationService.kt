package com.example.aquisito

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationService: Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    companion object {
        const val CHANNEL_ID = "LocationServiceChannel"
    }


    private val locationRequest: LocationRequest by lazy {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(2000)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("LocationService", "Service iniciado....................")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        initializeLocationCallBack()
        startLocationUpdates()

    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aquisito")
            .setContentText("Usando la ubicacion en segundo plano")
            .setSmallIcon(R.drawable.ic_person_location) //reemplazar con mi icono de la app
            .build()

        //iniciar el servicio en primer plano
        startForeground(1, notification)

        //conttinua ejecutando las actualizaciones de ubicacion
        startLocationUpdates()

        return super.onStartCommand(intent, flags, startId)
    }


    private fun initializeLocationCallBack(){
        locationCallback = object : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult){
                for (location in locationResult.locations){
                    convertLocationAddress(location)
                    Log.d("LocationService", "Lat: ${location.latitude}, Lng: ${location.longitude}")
                }
            }
        }
    }


    private  fun startLocationUpdates(){
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }catch (e: SecurityException){
            Log.e("LocationService", "Error de permisos: ${e.message}")
        }
    }

    private fun convertLocationAddress(location : Location){
        val geocoder = Geocoder(this, java.util.Locale.getDefault())
        try {
            val address = geocoder.getFromLocation(location.latitude, location.longitude,1)
            if (!address.isNullOrEmpty()){
                val addressText = if (address[0].thoroughfare !=null){
                    "${address[0].thoroughfare}, ${address[0].locality}, ${address[0].countryName}"
                } else {
                    "Sin nombre designado, ${address[0].locality}, ${address[0].countryName}"
                }
                sendLocationUpdate(location.latitude, location.longitude, addressText)
            }else{
                sendLocationUpdate(location.latitude, location.longitude, "No hay direccion")
            }
        }catch (e: Exception){
            Log.e("LocationService", "Error al convertir ubicación: ${e.message}")
        }
    }

    private  fun sendLocationUpdate(lat: Double, lng: Double, address: String ){
        val intent = Intent("LocationUpdate")
        intent.putExtra("latitud",lat)
        intent.putExtra("longitud",lng)
        intent.putExtra("address",address)
        sendBroadcast(intent)  // Enviar un broadcast que el fragmento escuchará

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null  // Este servicio no se enlaza, solo se inicia
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)  // Detener las actualizaciones cuando se destruye el servicio
    }

}