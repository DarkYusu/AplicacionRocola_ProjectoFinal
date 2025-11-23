package com.aplicaciones_android.aplicacionrocola_projectofinal

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aplicaciones_android.aplicacionrocola_projectofinal.model.CarouselItem

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar RecyclerView con 6 items de ejemplo
        val recycler = findViewById<RecyclerView>(R.id.carousel_recycler)
        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val sample = listOf(
            CarouselItem("1", "Top Hits", R.drawable.ic_placeholder_music, "Banda A"),
            CarouselItem("2", "Relax", R.drawable.ic_placeholder_music, "Banda B"),
            CarouselItem("3", "Rock Classics", R.drawable.ic_placeholder_music, "Banda C"),
            CarouselItem("4", "Indie", R.drawable.ic_placeholder_music, "Banda D"),
            CarouselItem("5", "Latino", R.drawable.ic_placeholder_music, "Banda E"),
            CarouselItem("6", "Electronic", R.drawable.ic_placeholder_music, "Banda F")
        )

        recycler.adapter = CarouselAdapter(sample)
    }
}