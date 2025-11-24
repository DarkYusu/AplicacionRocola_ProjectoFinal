package com.aplicaciones_android.aplicacionrocola_projectofinal.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aplicaciones_android.aplicacionrocola_projectofinal.R
import com.aplicaciones_android.aplicacionrocola_projectofinal.data.menu.MenuRepository
import com.aplicaciones_android.aplicacionrocola_projectofinal.ui.menu.MenuPublicAdapter
import kotlinx.coroutines.launch

class MenuFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var adapter: MenuPublicAdapter
    private val repository = MenuRepository()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.menu_recycler)
        emptyView = view.findViewById(R.id.menu_empty)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = MenuPublicAdapter(emptyList())
        recyclerView.adapter = adapter

        fetchMenu()
    }

    private fun fetchMenu() {
        viewLifecycleOwner.lifecycleScope.launch {
            val dishes = repository.fetchDishes()
            adapter.submit(dishes)
            emptyView.visibility = if (dishes.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
