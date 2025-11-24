package com.aplicaciones_android.aplicacionrocola_projectofinal

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AdminActivity : AppCompatActivity() {

    private lateinit var selectMenuBtn: Button
    private lateinit var selectRocolaBtn: Button
    private lateinit var uploadBtn: Button
    private lateinit var previewImage: ImageView
    private lateinit var statusText: TextView
    private lateinit var manageDishesButton: Button
    private var selectedUri: Uri? = null
    private var targetFolder: String = "menu"

    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            previewImage.load(it)
            statusText.text = getString(R.string.dish_image_selected)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseUtils.ensureInitialized(this)
        setContentView(R.layout.activity_admin)

        // Aplicar insets para desplazar contenido bajo la barra de estado
        val root = findViewById<View>(R.id.admin_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        selectMenuBtn = findViewById(R.id.button_select_menu)
        selectRocolaBtn = findViewById(R.id.button_select_rocola)
        uploadBtn = findViewById(R.id.button_upload)
        previewImage = findViewById(R.id.image_preview)
        statusText = findViewById(R.id.text_status)
        manageDishesButton = findViewById(R.id.manage_dishes_button)

        selectMenuBtn.setOnClickListener {
            targetFolder = "menu"
            getImage.launch("image/*")
        }
        selectRocolaBtn.setOnClickListener {
            targetFolder = "rocola"
            getImage.launch("image/*")
        }

        uploadBtn.setOnClickListener {
            selectedUri?.let { uri ->
                uploadImage(uri, targetFolder)
            } ?: run {
                Toast.makeText(this, "Selecciona una imagen primero", Toast.LENGTH_SHORT).show()
            }
        }

        manageDishesButton.setOnClickListener {
            startActivity(Intent(this, ManageDishesActivity::class.java))
        }
    }

    private fun uploadImage(uri: Uri, folder: String) {
        statusText.text = getString(R.string.upload_button)
        val storageRef = FirebaseStorage.getInstance().reference
        val id = UUID.randomUUID().toString()
        val path = "$folder/$id.jpg"
        val ref = storageRef.child(path)

        val uploadTask = ref.putFile(uri)
        uploadTask.addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { downloadUri ->
                // Guardar metadatos en Firestore si es 'menu' o 'rocola'
                val db = FirebaseFirestore.getInstance()
                val data = hashMapOf(
                    "imageUrl" to downloadUri.toString(),
                    "path" to path,
                    "createdAt" to System.currentTimeMillis()
                )
                val collection = if (folder == "menu") "menu_images" else "rocola_images"
                db.collection(collection).add(data)
                    .addOnSuccessListener {
                        statusText.text = "Subida completa"
                        Toast.makeText(this, "Subida completa", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        statusText.text = "Error guardando metadata: ${e.message}"
                    }
            }
        }.addOnFailureListener { e ->
            statusText.text = "Error subiendo: ${e.message}"
        }
    }
}
