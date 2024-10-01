package com.example.aquisito

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
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
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class LocationService: Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    companion object {
        const val CHANNEL_ID = "LocationServiceChannel"
    }


    private val locationRequest: LocationRequest by lazy {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(3000)
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
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Servicio de ubicación")
            .setContentText("El servicio de ubicación se está ejecutando")
            .setSmallIcon(R.drawable.ic_person_location)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        // Tu lógica del servicio aquí

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
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
        val geocoder = Geocoder(this, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(location.latitude,location.longitude,1,
            object : Geocoder.GeocodeListener{

                override fun onGeocode(addresses: MutableList<Address>) {
                    if (addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val addressText = if (address.thoroughfare != null) {
                            "${address.thoroughfare}, ${address.locality}, ${address.countryName}"
                        } else {
                            "Calle sin nombre designado, ${address.locality}, ${address.countryName}"
                        }
                        sendLocationUpdate(location.latitude, location.longitude, addressText)
                    } else {sendLocationUpdate(location.latitude, location.longitude, "No hay dirección")
                    }
                }

                override fun onError(errorMessage: String?) {
                    Log.e("LocationService", "Error al convertir ubicación: $errorMessage")
                    sendLocationUpdate(location.latitude, location.longitude, "Error al obtener la dirección")
                }
            })
        }else{
            // Código que se ejecuta en versiones anteriores de Android
            val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!address.isNullOrEmpty( ) ) {
                val addressText = if (address[0].thoroughfare !=null)
                {"${address[0].thoroughfare}, ${address[0].locality}, ${address[0].countryName} "
                } else {
                    "Calle sin nombre designado, ${address[0].locality} ,  ${address[0].countryName}"
                }
                sendLocationUpdate(location. latitude,  location.longitude, addressText)
            }else {
                sendLocationUpdate(location. latitude,  location.longitude, "No hay direccion")
            }
        }
    }

    private  fun sendLocationUpdate(lat: Double, lng: Double, address: String){
        fetchNearbyPOI(lat, lng) { poiName ->
            val intent = Intent("LocationUpdate")
            intent.putExtra("latitud", lat)
            intent.putExtra("longitud", lng)
            intent.putExtra("address", address)
            intent.putExtra("namePOI", poiName)
            sendBroadcast(intent)  // Enviar un broadcast que el fragmento escuchará
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null  // Este servicio no se enlaza, solo se inicia
    }

    private fun fetchNearbyPOI(lat: Double, lng: Double, onResult: (String) -> Unit){
        // URL de Overpass API con latitud y longitud dinámicas
        val radius= 15 //en metros
        val url = "https://overpass-api.de/api/interpreter?data=[out:json];node(around:20,$lat,$lng)[leisure=park];node(around:$radius,$lat,$lng)[amenity~\"university|place_of_worship|bank\"];out;"

        // usar okHttp para hcer la solicitud HTTP
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()

            if (response.isSuccessful && responseData!=null){
                try {
                    val jsonObject = JSONObject(responseData)
                    val elementArray = jsonObject.getJSONArray("elements")

                    if (elementArray.length()>0){
                        val firstPOI = elementArray.getJSONObject(0)
                        val name = firstPOI.optJSONObject("tags")?.optString("name", "Nombre no disponible")
                        onResult(name ?: "Nombre no disponible")
                    }else {
                    onResult("No se encontraron POIs cercanos.")
                }
            }catch (e: Exception) {
                    e.printStackTrace()
                    onResult("Error al procesar la respuesta.")
            }
            } else {
                onResult("Error en la solicitud.")
            }
            }
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                onResult("Error en la conexión.")
            }
        })

    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)  // Detener las actualizaciones cuando se destruye el servicio
    }

}