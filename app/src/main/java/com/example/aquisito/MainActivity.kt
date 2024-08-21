package com.example.aquisito

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.aquisito.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // Variable para el binding de la actividad principal
    private lateinit var mBinding: ActivityMainBinding


    private val locationFragment = LocationFragment()
    private val routeFragment = RouteFragment()
    private val configFragment = ConfigFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)


        mBinding.bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_location -> {
                    replaceFragment(locationFragment, getString(R.string.location_title))
                    true
                }
                R.id.action_route -> {
                    replaceFragment(routeFragment, getString(R.string.route_title))
                    true
                }
                R.id.action_config -> {
                    replaceFragment(configFragment, getString(R.string.config_title))
                    true
                }
                else -> false
            }
        }

        // Establecer la pestaña inicial
        mBinding.bottomNav.selectedItemId = R.id.action_location
    }

    private fun replaceFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.hostFragment, fragment)
            .commit()

        mBinding.toolbar.title = title
        supportActionBar?.title = title
    }

}