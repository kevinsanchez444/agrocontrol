package com.example.agentes.ui.finanzas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.agentes.databinding.FragmentFinanzasBinding;
import com.example.agentes.ui.EstadoViews;

/**
 * Stub navegable del módulo Finanzas (propietario final: Integrante 3).
 * Muestra el estado vacío del prototipo hasta que haya lotes registrados.
 */
public class FinanzasFragment extends Fragment {

    private FragmentFinanzasBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFinanzasBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EstadoViews estados = new EstadoViews(
                binding.stateEmpty, binding.stateLoading, binding.stateError, binding.stateContent);
        estados.mostrar(EstadoViews.VACIO);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
