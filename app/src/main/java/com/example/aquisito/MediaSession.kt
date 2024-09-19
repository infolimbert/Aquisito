package com.example.aquisito

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import androidx.media.session.MediaButtonReceiver

class MediaSession {

    private var session: MediaSessionCompat? = null
    private var player: SilencePlayer? = null
    private var callback: MediaSessionCompat.Callback? = null

    fun start(context: Context) {
        Model.initializeTTS(context) // Inicializamos TTS
        callback = object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(intent: Intent): Boolean {
                if (intent.action != Intent.ACTION_MEDIA_BUTTON) return false
                val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return false
                Model.onMediaKeyEvent(event, context) // Delegamos el evento a Model para manejar TTS
                return true
            }
        }
        val mediaButtonReceiver = ComponentName(context.applicationContext, MediaButtonReceiver::class.java)
        session = MediaSessionCompat(context.applicationContext, "media session test", mediaButtonReceiver, null).also { session ->
            session.setCallback(callback)
            val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).run {
                setClass(context.applicationContext, MediaButtonReceiver::class.java)
            }
            session.setMediaButtonReceiver(
                PendingIntent.getBroadcast(
                context.applicationContext, 0, mediaButtonIntent, if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) PendingIntent.FLAG_IMMUTABLE else 0))
            session.isActive = true
            // Cambiamos el estado de la sesión a "reproduciendo"
            val playbackState = PlaybackStateCompat.Builder().run {
                setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_PLAY_PAUSE)
                setState(PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f) // Iniciar en PAUSADO
            }.build()
            session.setPlaybackState(playbackState)
        }
        startPlayer(context) // Iniciamos el reproductor de silencio
    }

    fun stop(context: Context) {
        Log.i("MEDIA SESSION", "onDestroy llamado, MediaSession liberada")
        session?.release()
        session = null
        callback = null
        stopPlayer()
        Model.releaseTTS() // Liberamos TTS cuando detenemos la sesión

        //probando
        session?.isActive =false
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).run {
            setClass(context, MediaButtonReceiver::class.java)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, mediaButtonIntent,
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) PendingIntent.FLAG_IMMUTABLE else 0
        )
        pendingIntent.cancel()

    }

    private fun startPlayer(context: Context) {
        player = SilencePlayer(context).also {
            it.playForever() // Reproducimos el audio en silencio continuamente
        }
    }

    private fun stopPlayer() {
        player?.release()
        player = null
    }
}