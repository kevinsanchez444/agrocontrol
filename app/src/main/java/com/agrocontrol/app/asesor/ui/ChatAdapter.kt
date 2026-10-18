package com.agrocontrol.app.asesor.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agrocontrol.app.R
import com.agrocontrol.app.asesor.data.EstadoMensaje
import com.agrocontrol.app.asesor.data.MensajeChat

class ChatAdapter : ListAdapter<MensajeChat, ChatAdapter.ViewHolder>(DIF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = if (viewType == 0) {
            inflater.inflate(R.layout.item_message_user, parent, false)
        } else {
            inflater.inflate(R.layout.item_message_ia, parent, false)
        }
        return ViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).esUsuario) 0 else 1
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val texto = itemView.findViewById<android.widget.TextView>(R.id.tvTexto)

        fun bind(mensaje: MensajeChat) {
            texto.text = mensaje.texto
        }
    }

    companion object {
        private val DIF_CALLBACK = object : DiffUtil.ItemCallback<MensajeChat>() {
            override fun areItemsTheSame(oldItem: MensajeChat, newItem: MensajeChat): Boolean =
                oldItem.texto == newItem.texto && oldItem.esUsuario == newItem.esUsuario

            override fun areContentsTheSame(oldItem: MensajeChat, newItem: MensajeChat): Boolean =
                oldItem == newItem
        }
    }
}
