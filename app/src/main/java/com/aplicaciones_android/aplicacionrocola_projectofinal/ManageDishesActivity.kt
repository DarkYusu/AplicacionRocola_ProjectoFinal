package com.aplicaciones_android.aplicacionrocola_projectofinal

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.aplicaciones_android.aplicacionrocola_projectofinal.model.MenuDish
import kotlinx.coroutines.launch

class ManageDishesActivity : AppCompatActivity() {

    private lateinit var dishImagePreview: ImageView
    private lateinit var dishSelectImageButton: Button
    private lateinit var dishNameInput: EditText
    private lateinit var dishDescriptionInput: EditText
    private lateinit var dishPriceInput: EditText
    private lateinit var dishStatusText: TextView
    private lateinit var saveDishButton: Button
    private lateinit var clearFormButton: Button
    private lateinit var dishRecycler: RecyclerView
    private lateinit var dishEmptyView: TextView
    private lateinit var dishAdapter: MenuAdminAdapter

    private val menuRepository = MenuRepository()
    private var editingDish: MenuDish? = null
    private var dishImageUri: Uri? = null

    private val getDishImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            dishImageUri = it
            dishImagePreview.load(it)
            dishStatusText.text = getString(R.string.dish_image_selected)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_dishes)

        dishImagePreview = findViewById(R.id.dish_image_preview)
        dishSelectImageButton = findViewById(R.id.dish_select_image_button)
        dishNameInput = findViewById(R.id.dish_name_input)
        dishDescriptionInput = findViewById(R.id.dish_description_input)
        dishPriceInput = findViewById(R.id.dish_price_input)
        dishStatusText = findViewById(R.id.dish_form_status)
        saveDishButton = findViewById(R.id.save_dish_button)
        clearFormButton = findViewById(R.id.clear_form_button)
        dishRecycler = findViewById(R.id.dish_recycler_admin)
        dishEmptyView = findViewById(R.id.dish_list_empty)

        dishRecycler.layoutManager = LinearLayoutManager(this)
        dishAdapter = MenuAdminAdapter(emptyList(), ::onEditDish, ::onDeleteDish)
        dishRecycler.adapter = dishAdapter

        dishSelectImageButton.setOnClickListener { getDishImage.launch("image/*") }
        saveDishButton.setOnClickListener { saveDish() }
        clearFormButton.setOnClickListener { clearDishForm() }

        fetchDishes()
    }

    private fun saveDish() {
        val name = dishNameInput.text.toString().trim()
        val description = dishDescriptionInput.text.toString().trim()
        val price = dishPriceInput.text.toString().toDoubleOrNull() ?: 0.0
        if (name.isEmpty() || description.isEmpty()) {
            dishStatusText.text = getString(R.string.dish_complete_fields)
            return
        }
        val baseDish = editingDish ?: MenuDish()
        val dish = baseDish.copy(name = name, description = description, price = price)

        lifecycleScope.launch {
            val success = menuRepository.upsertDish(dish, dishImageUri)
            if (success) {
                Toast.makeText(this@ManageDishesActivity, getString(if (editingDish == null) R.string.dish_saved else R.string.dish_updated), Toast.LENGTH_SHORT).show()
                clearDishForm()
                fetchDishes()
            } else {
                Toast.makeText(this@ManageDishesActivity, R.string.dish_save_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onEditDish(dish: MenuDish) {
        editingDish = dish
        dishNameInput.setText(dish.name)
        dishDescriptionInput.setText(dish.description)
        dishPriceInput.setText(dish.price.toString())
        dishImagePreview.load(dish.imageUrl)
        dishStatusText.text = getString(R.string.dish_editing_label)
        dishImageUri = null
    }

    private fun onDeleteDish(dish: MenuDish) {
        lifecycleScope.launch {
            val success = menuRepository.deleteDish(dish)
            if (success) {
                Toast.makeText(this@ManageDishesActivity, R.string.dish_deleted, Toast.LENGTH_SHORT).show()
                fetchDishes()
            } else {
                Toast.makeText(this@ManageDishesActivity, R.string.dish_delete_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearDishForm() {
        editingDish = null
        dishNameInput.text?.clear()
        dishDescriptionInput.text?.clear()
        dishPriceInput.text?.clear()
        dishImagePreview.setImageResource(R.mipmap.ic_launcher)
        dishImageUri = null
        dishStatusText.text = getString(R.string.dish_form_status)
    }

    private fun fetchDishes() {
        lifecycleScope.launch {
            val dishes = menuRepository.fetchDishes()
            dishAdapter.submit(dishes)
            dishEmptyView.visibility = if (dishes.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
