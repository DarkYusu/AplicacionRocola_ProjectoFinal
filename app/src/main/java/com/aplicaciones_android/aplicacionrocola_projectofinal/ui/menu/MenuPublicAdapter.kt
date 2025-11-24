package com.aplicaciones_android.aplicacionrocola_projectofinal.ui.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.aplicaciones_android.aplicacionrocola_projectofinal.R
import com.aplicaciones_android.aplicacionrocola_projectofinal.model.MenuDish
import java.text.NumberFormat
import java.util.Locale

class MenuPublicAdapter(private var dishes: List<MenuDish>) : RecyclerView.Adapter<MenuPublicAdapter.VH>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.menu_item_image)
        val name: TextView = view.findViewById(R.id.menu_item_name)
        val description: TextView = view.findViewById(R.id.menu_item_description)
        val price: TextView = view.findViewById(R.id.menu_item_price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_menu_dish_public, parent, false)
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
    }

    override fun getItemCount(): Int = dishes.size

    fun submit(newList: List<MenuDish>) {
        dishes = newList
        notifyDataSetChanged()
    }
}
