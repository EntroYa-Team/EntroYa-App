package com.example.entroya.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.entroya.data.model.Usuario

class UsuarioSpinnerAdapter(context: Context, usuarios: List<Usuario>) :
    ArrayAdapter<Usuario>(context, android.R.layout.simple_spinner_item, usuarios) {

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            android.R.layout.simple_spinner_item, parent, false
        )
        val textView = view.findViewById<TextView>(android.R.id.text1)
        val usuario = getItem(position)
        textView.text = "${usuario?.nombre} (ID: ${usuario?.id})"
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            android.R.layout.simple_spinner_dropdown_item, parent, false
        )
        val textView = view.findViewById<TextView>(android.R.id.text1)
        val usuario = getItem(position)
        textView.text = "${usuario?.nombre} - ${usuario?.rol} (ID: ${usuario?.id})"
        return view
    }
}