package com.example.aquisito

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.aquisito.databinding.ActivitySplashBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity(){

    private lateinit var splashBinding: ActivitySplashBinding
    private lateinit var mediaPlayer: MediaPlayer

    //firebase inicializacion
    private lateinit var auth: FirebaseAuth
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private lateinit var mAuthListener: FirebaseAuth.AuthStateListener


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

       splashBinding= ActivitySplashBinding.inflate(layoutInflater)
        setContentView(splashBinding.root)



        // Inicializa FirebaseAuth
        auth = FirebaseAuth.getInstance()
        // Configurar el lanzador de Firebase UI
        initSignInLauncher()




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
              /*  val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                finish()*/
               checkAuthentication() // Verifica si el usuario está autenticado

                }, 3500) // 3000 milisegundos = 3 segundos
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

    }

    // Inicializa el lanzador para Firebase UI
    private fun initSignInLauncher() {
        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val response = IdpResponse.fromResultIntent(result.data)
            if (result.resultCode == Activity.RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    Toast.makeText(this, "Bienvenido ${user.displayName}", Toast.LENGTH_SHORT).show()
                    goToMainActivity() // Usuario autenticado, pasa a MainActivity
                }
            } else {
                if (response == null) {
                    Toast.makeText(this, "Inicio de sesión cancelado.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al iniciar sesión: ${response.error?.errorCode}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    // Verifica el estado de autenticación y lanza Firebase UI si el usuario no está autenticado
    private fun checkAuthentication() {
        val user = auth.currentUser
        if (user == null) {
            val providers = arrayListOf(
                AuthUI.IdpConfig.GoogleBuilder().build()
            )
            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
            signInLauncher.launch(signInIntent)
        } else {
            goToMainActivity() // Usuario ya autenticado, pasa a MainActivity
        }
    }

    // Navegar a MainActivity
    private fun goToMainActivity() {
        val intent = Intent(this@SplashActivity, MainActivity::class.java)
        startActivity(intent)
        finish() // Finaliza el SplashActivity para que no pueda volver
    }

    // Agregar el AuthStateListener en onStart
    override fun onStart() {
        super.onStart()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                Log.d("SplashActivity", "Usuario autenticado: ${user.email}")
            } else {
                Log.d("SplashActivity", "Usuario no autenticado")
            }
        }
        auth.addAuthStateListener(mAuthListener)
    }

    // Remover el AuthStateListener en onStop
    override fun onStop() {
        super.onStop()
        if (this::mAuthListener.isInitialized) {
            auth.removeAuthStateListener(mAuthListener)
        }
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