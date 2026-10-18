package com.example.agentes.ui.lotes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.agentes.R;
import com.example.agentes.databinding.FragmentLoteDetalleBinding;
import com.example.agentes.navigation.NavArgsContract;
import com.example.agentes.ui.EstadoViews;

/**
 * Stub navegable del detalle de lote (propietario final: Integrante 2).
 * Valida el argumento lote_id: si falta o está vacío muestra state_error
 * con botón "Volver" (nunca consulta datos ni crashea).
 */
public class LoteDetalleFragment extends Fragment {

    private FragmentLoteDetalleBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentLoteDetalleBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EstadoViews estados = new EstadoViews(
                binding.stateEmpty, binding.stateLoading, binding.stateError, binding.stateContent);

        Bundle args = getArguments();
        String loteId = args == null ? "" : args.getString(NavArgsContract.ARG_LOTE_ID, "");

        if (loteId == null || loteId.isEmpty()) {
            binding.textDetalleError.setText(R.string.detalle_missing_id);
            estados.mostrar(EstadoViews.ERROR);
        } else {
            binding.textDetalleLoteId.setText(getString(R.string.detalle_lote_id_pattern, loteId));
            estados.mostrar(EstadoViews.CONTENIDO);
        }

        binding.btnDetalleBack.setOnClickListener(v -> volver());
        binding.btnDetalleErrorBack.setOnClickListener(v -> volver());
    }

    private void volver() {
        NavHostFragment.findNavController(this).navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
