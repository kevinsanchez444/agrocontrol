package com.example.agentes.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.agentes.R;
import com.example.agentes.databinding.FragmentHomeBinding;
import com.example.agentes.navigation.NavArgsContract;
import com.example.agentes.ui.EstadoViews;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EstadoViews estados = new EstadoViews(
                binding.stateEmpty, binding.stateLoading, binding.stateError, binding.stateContent);
        estados.mostrar(EstadoViews.CONTENIDO);

        // Contador fijo hasta que el Integrante 1/2 exponga el dato real
        binding.textHomeLotesCount.setText(getString(R.string.home_lotes_count_pattern, 0));

        binding.tileLotes.setOnClickListener(v -> navegarA(R.id.lotesFragment, null));
        binding.tileFinanzas.setOnClickListener(v -> navegarA(R.id.finanzasFragment, null));
        binding.tileAsesor.setOnClickListener(v -> navegarA(R.id.asesorFragment, null));
        binding.tileNuevoLote.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean(NavArgsContract.ARG_ABRIR_FORMULARIO, true);
            navegarA(R.id.lotesFragment, args);
        });
    }

    private void navegarA(int destino, Bundle args) {
        NavOptions opciones = new NavOptions.Builder()
                .setPopUpTo(R.id.homeFragment, false)
                .setLaunchSingleTop(true)
                .build();
        NavHostFragment.findNavController(this).navigate(destino, args, opciones);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
