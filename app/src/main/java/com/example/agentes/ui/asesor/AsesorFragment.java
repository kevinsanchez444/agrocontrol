package com.example.agentes.ui.asesor;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.agrocontrol.app.asesor.ui.ChatActivity;
import com.example.agentes.databinding.FragmentAsesorBinding;
import com.example.agentes.ui.EstadoViews;

/**
 * Stub navegable del módulo Asesor IA (propietario final: Integrante 4).
 * Estado inicial: chat "vacío" con la burbuja de bienvenida del prototipo;
 * el input queda deshabilitado (nunca se incluye una clave de API dentro de la app)
 * y el botón de enviar abre la pantalla de chat real (ChatActivity,
 * integrada desde feature/asesor-ia).
 */
public class AsesorFragment extends Fragment {

    private FragmentAsesorBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentAsesorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EstadoViews estados = new EstadoViews(
                binding.stateEmpty, binding.stateLoading, binding.stateError, binding.stateContent);
        estados.mostrar(EstadoViews.VACIO);

        // Integración feature/asesor-ia: el botón de enviar abre el chat real (ChatActivity)
        binding.btnAsesorSend.setEnabled(true);
        binding.btnAsesorSend.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ChatActivity.class)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
