package com.aplicaciones_android.aplicacionrocola_projectofinal

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Cargar fragmento inicio
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FragmentInicio())
                .commit()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.bringToFront()
        bottomNav.selectedItemId = R.id.nav_inicio
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, FragmentInicio())
                        .commit()
                    true
                }
                R.id.nav_buscar -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BuscarFragment())
                        .commit()
                    true
                }
                R.id.nav_playlist -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, PlaylistFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}