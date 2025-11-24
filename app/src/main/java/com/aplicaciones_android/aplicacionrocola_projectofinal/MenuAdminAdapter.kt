package com.aplicaciones_android.aplicacionrocola_projectofinal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.aplicaciones_android.aplicacionrocola_projectofinal.model.MenuDish
import java.text.NumberFormat
import java.util.Locale

class MenuAdminAdapter(
    private var dishes: List<MenuDish>,
    private val onEdit: (MenuDish) -> Unit,
    private val onDelete: (MenuDish) -> Unit
) : RecyclerView.Adapter<MenuAdminAdapter.VH>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.dish_image)
        val name: TextView = view.findViewById(R.id.dish_name)
        val description: TextView = view.findViewById(R.id.dish_description)
        val price: TextView = view.findViewById(R.id.dish_price)
        val edit: View = view.findViewById(R.id.edit_button)
        val delete: View = view.findViewById(R.id.delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_menu_dish_admin, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val dish = dishes[position]
        holder.name.text = dish.name
        holder.description.text = dish.description
        holder.price.text = currencyFormat.format(dish.price)
        holder.image.load(dish.imageUrl) {
            placeholder(R.mipmap.ic_launcher)
            error(R.mipmap.ic_launcher)
        }
        holder.edit.setOnClickListener { onEdit(dish) }
        holder.delete.setOnClickListener { onDelete(dish) }
    }

    override fun getItemCount(): Int = dishes.size

    fun submit(newList: List<MenuDish>) {
        dishes = newList
        notifyDataSetChanged()
    }
}
