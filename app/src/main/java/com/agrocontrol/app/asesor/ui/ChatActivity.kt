package com.agrocontrol.app.asesor.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.agentes.databinding.ActivityChatBinding
import com.agrocontrol.app.asesor.ChatViewModel

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        adapter = ChatAdapter()
        binding.rvMensajes.layoutManager = LinearLayoutManager(this)
        binding.rvMensajes.adapter = adapter

        viewModel.mensajes.observe(this) { mensajes ->
            adapter.submitList(mensajes)
            binding.rvMensajes.scrollToPosition(adapter.itemCount - 1)
        }

        viewModel.error.observe(this) { error ->
            if (error != null) {
                binding.tvError.text = error
                binding.layoutError.visibility = View.VISIBLE
            } else {
                binding.layoutError.visibility = View.GONE
            }
        }

        binding.btnEnviar.setOnClickListener {
            val texto = binding.etMensaje.text.toString().trim()
            if (texto.isNotEmpty()) {
                viewModel.enviar(texto)
                binding.etMensaje.text.clear()
            }
        }

        binding.btnReintentar.setOnClickListener {
            viewModel.reintentarUltima()
            binding.layoutError.visibility = View.GONE
        }
    }
}
