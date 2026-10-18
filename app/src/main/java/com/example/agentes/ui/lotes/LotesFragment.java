package com.example.agentes.ui.lotes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.agentes.R;
import com.example.agentes.databinding.FragmentLotesBinding;
import com.example.agentes.navigation.NavArgsContract;
import com.example.agentes.ui.EstadoViews;

/**
 * Stub navegable del módulo Lotes (propietario final: Integrante 2).
 * El Integrante 2 reemplaza el contenido de state_content y la lógica,
 * conservando los 4 IDs de estado y el contrato de argumentos.
 */
public class LotesFragment extends Fragment {

    private FragmentLotesBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentLotesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EstadoViews estados = new EstadoViews(
                binding.stateEmpty, binding.stateLoading, binding.stateError, binding.stateContent);
        estados.mostrar(EstadoViews.VACIO);

        consumirArgumentoFormulario();

        binding.btnLotesNuevo.setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.lotes_demo_toast, Toast.LENGTH_SHORT).show());

        binding.btnLotesDemo.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(NavArgsContract.ARG_LOTE_ID, "demo-uuid");
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_lotesFragment_to_loteDetalleFragment, args);
        });
    }

    private void consumirArgumentoFormulario() {
        Bundle args = getArguments();
        if (args != null && args.getBoolean(NavArgsContract.ARG_ABRIR_FORMULARIO, false)) {
            Toast.makeText(requireContext(), R.string.lotes_abrir_formulario_toast, Toast.LENGTH_SHORT).show();
            args.putBoolean(NavArgsContract.ARG_ABRIR_FORMULARIO, false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
