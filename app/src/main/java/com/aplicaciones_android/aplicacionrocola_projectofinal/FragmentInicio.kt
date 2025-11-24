package com.aplicaciones_android.aplicacionrocola_projectofinal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.firebase.firestore.FirebaseFirestore

class FragmentInicio : Fragment() {

    private lateinit var menuImage: ImageView
    private lateinit var rocolaImage: ImageView
    private lateinit var carouselRecycler: RecyclerView
    private lateinit var adapter: MenuImageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_inicio, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Aplicar insets para desplazar contenido bajo la barra de estado
        val root = view.findViewById<View>(R.id.fragment_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        FirebaseUtils.ensureInitialized(requireContext())

        menuImage = view.findViewById(R.id.menu_image)
        rocolaImage = view.findViewById(R.id.rocola_image)
        carouselRecycler = view.findViewById(R.id.carousel_recycler)

        val adminBtn = view.findViewById<Button>(R.id.admin_button)
        adminBtn.setOnClickListener {
            startActivity(Intent(requireContext(), AdminActivity::class.java))
        }

        // Cargar últimas imágenes subidas por admin para banner y rocola
        loadLatestImage("menu_images") { url ->
            url?.let { menuImage.load(it) }
        }
        loadLatestImage("rocola_images") { url ->
            url?.let { rocolaImage.load(it) }
        }

        // Configurar RecyclerView y adapter vacío inicialmente
        carouselRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        adapter = MenuImageAdapter(emptyList())
        carouselRecycler.adapter = adapter

        // Cargar todas las imágenes de menu_images para el recyclerview
        loadAllMenuImages()
    }

    private fun loadLatestImage(collection: String, callback: (String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection(collection)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snaps ->
                if (!snaps.isEmpty) {
                    val doc = snaps.documents[0]
                    val url = doc.getString("imageUrl")
                    callback(url)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    private fun loadAllMenuImages() {
        val db = FirebaseFirestore.getInstance()
        db.collection("menu_images")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snaps ->
                val urls = snaps.documents.mapNotNull { it.getString("imageUrl") }
                adapter.update(urls)
            }
            .addOnFailureListener {
                // no hago nada, dejo el adapter vacío
            }
    }
}
