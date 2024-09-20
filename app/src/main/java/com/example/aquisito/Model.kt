package com.example.aquisito

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import java.util.Locale

    object Model {

        private var locationFragment: LocationFragment? = null


        // Pasamos el LocationFragment desde el cual queremos llamar a speakLocation()
        fun setLocationFragment(fragment: LocationFragment) {
            locationFragment = fragment
        }


        private val _lastMediaEvent: Subject<String> = BehaviorSubject.createDefault("")
    val lastMediaEvent: Observable<String> = _lastMediaEvent

    // Maneja los eventos de teclas multimedia y verifica si es el botón de "Play"
    fun onMediaKeyEvent(event: KeyEvent, context: Context) {
        Log.i("MEDIA SESSION SAMPLE", "Media key $event")
        _lastMediaEvent.onNext(event.toString())

        // Solo activamos TTS cuando el botón es presionado (ACTION_DOWN)
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    Log.d("TTS", "Botón de Play presionado, reproduciendo texto")
                    //speakText(context, "Botón de Play presionado, reproduciendo texto")
                    locationFragment?.speakLocation() // Llamamos a la función speakLocation() del LocationFragment
                }

                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    Log.d("TTS", "Botón de Pause presionado, reproduciendo texto")
                   // speakText(context, "Botón de Pause presionado, reproduciendo texto")
                    locationFragment?.speakLocation() // Llamamos a la función speakLocation() del LocationFragment
                }
            }
        }
    }

}