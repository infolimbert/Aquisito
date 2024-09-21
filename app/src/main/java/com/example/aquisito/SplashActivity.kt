package com.example.aquisito

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.aquisito.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity(){

    private lateinit var splashBinding: ActivitySplashBinding
    private lateinit var mediaPlayer: MediaPlayer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       splashBinding= ActivitySplashBinding.inflate(layoutInflater)
        setContentView(splashBinding.root)

        // Cargar la animación desde res/anim
        val scaleUpAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_up)

        // Aplicar la animación a la ImageView
        splashBinding.splashLogo.startAnimation(scaleUpAnimation)


        // Inicializa y reproduce el sonido
        mediaPlayer = MediaPlayer.create(this, R.raw.introapp) // Reemplaza 'splash_sound' con tu archivo
        mediaPlayer.start()


        // Después de la animación, pasar a la MainActivity
        scaleUpAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}


            override fun onAnimationEnd(animation: Animation?) {

                Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                finish()

                }, 3500) // 3000 milisegundos = 3 segundos
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

    }

    override fun onDestroy() {
        super.onDestroy()
        // Libera el MediaPlayer cuando se cierra la actividad
        if (this::mediaPlayer.isInitialized) {
            Log.d("MEDIA INTRO","Finalizando el intro")
            mediaPlayer.release()
        }
    }
}