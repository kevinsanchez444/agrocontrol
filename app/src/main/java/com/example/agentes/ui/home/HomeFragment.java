package com.example.agentes.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.agentes.R;
import com.example.agentes.data.LoteRepository;
import com.example.agentes.data.SupabaseConfig;
import com.example.agentes.databinding.FragmentHomeBinding;
import com.example.agentes.model.Lote;
import com.example.agentes.navigation.NavArgsContract;
import com.example.agentes.ui.EstadoViews;

import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private FragmentHomeBinding binding;
    private LoteRepository repositorio;

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

        repositorio = new LoteRepository();

        EstadoViews estados = new EstadoViews(
                binding.stateEmpty, binding.stateLoading, binding.stateError, binding.stateContent);
        estados.mostrar(EstadoViews.CONTENIDO);

        // Saludo personalizado con el nombre de la sesión (estático durante la sesión)
        String nombre = SupabaseConfig.getNombreUsuario();
        if (nombre == null || nombre.trim().isEmpty()) {
            nombre = "Agricultor";
        }
        binding.textHomeGreeting.setText(getString(R.string.home_greeting_pattern, nombre));

        binding.tileLotes.setOnClickListener(v -> navegarA(R.id.lotesFragment, null));
        binding.tileFinanzas.setOnClickListener(v -> navegarA(R.id.finanzasFragment, null));
        binding.tileAsesor.setOnClickListener(v -> navegarA(R.id.asesorFragment, null));
        binding.tileNuevoLote.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean(NavArgsContract.ARG_ABRIR_FORMULARIO, true);
            navegarA(R.id.lotesFragment, args);
        });
    }

    /**
     * Consulta el número real de lotes del usuario y refresca el contador del inicio.
     * Se invoca también en {@link #onResume()} para que el dato se actualice cada vez
     * que el usuario vuelve a Home (p. ej. tras crear o editar un lote).
     */
    private void actualizarContador() {
        if (binding == null) {
            return;
        }
        repositorio.listarLotes(new LoteRepository.Callback<List<Lote>>() {
            @Override
            public void onExito(List<Lote> datos) {
                if (binding == null) {
                    return;
                }
                binding.textHomeLotesCount.setText(
                        getString(R.string.home_lotes_count_pattern, datos.size()));
            }

            @Override
            public void onError(String mensaje) {
                // Sin Snackbar en Home para no spamear: se deja/muestra 0 y se loguea el motivo.
                Log.w(TAG, "No se pudo actualizar el contador de lotes: " + mensaje);
                if (binding == null) {
                    return;
                }
                binding.textHomeLotesCount.setText(getString(R.string.home_lotes_count_pattern, 0));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        actualizarContador();
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
