package com.example.agentes.ui.lotes;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.navigation.fragment.NavHostFragment;

import com.example.agentes.R;
import com.example.agentes.data.LoteRepository;
import com.example.agentes.databinding.FragmentLoteDetalleBinding;
import com.example.agentes.model.Lote;
import com.example.agentes.navigation.NavArgsContract;
import com.example.agentes.ui.EstadoViews;

/**
 * Pantalla de detalle del lote.
 *
 * <p>Valida el argumento lote_id: si falta o está vacío muestra state_error con botón
 * "Volver" (nunca consulta datos ni crashea). En estado CONTENIDO pinta las
 * especificaciones del lote (nombre, cultivo en chip y hectáreas) enviadas por
 * {@link LotesContrato#argumentosDetalleLote}, además del lote_id.</p>
 *
 * <p>Desde el estado CONTENIDO también permite editar el lote (hoja inferior en modo
 * edición, que devuelve el lote actualizado vía
 * {@link LotesContrato#ARG_RESULT_LOTE} y se pinta al instante) y eliminarlo
 * (con diálogo de confirmación; al eliminar vuelve a la lista).</p>
 */
public class LoteDetalleFragment extends Fragment {

    private FragmentLoteDetalleBinding binding;
    private LoteRepository repositorio;
    private Lote lote;

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

        repositorio = new LoteRepository();
        EstadoViews estados = new EstadoViews(
                binding.stateEmpty, binding.stateLoading, binding.stateError, binding.stateContent);

        Bundle args = getArguments();
        String loteId = args == null ? "" : args.getString(NavArgsContract.ARG_LOTE_ID, "");

        if (loteId == null || loteId.isEmpty()) {
            lote = null;
            binding.textDetalleError.setText(R.string.detalle_missing_id);
            estados.mostrar(EstadoViews.ERROR);
        } else {
            lote = new Lote();
            lote.setId(loteId);
            lote.setNombre(args.getString(LotesContrato.ARG_LOTE_NOMBRE, ""));
            lote.setCultivo(args.getString(LotesContrato.ARG_LOTE_CULTIVO, ""));
            lote.setHectareas(parsearHectareas(args.getString(LotesContrato.ARG_LOTE_HECTARES, "")));
            mostrarLote();
            estados.mostrar(EstadoViews.CONTENIDO);
        }

        getChildFragmentManager().setFragmentResultListener(
                LotesContrato.ARG_RESULT_LOTE, this, new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                        // La hoja de edición ya mostró su toast; aquí solo se repinta el detalle.
                        Lote actualizado =
                                (Lote) result.getSerializable(LotesContrato.ARG_RESULT_LOTE);
                        if (actualizado != null) {
                            lote = actualizado;
                            mostrarLote();
                        }
                    }
                });

        binding.btnDetalleBack.setOnClickListener(v -> volver());
        binding.btnDetalleErrorBack.setOnClickListener(v -> volver());

        binding.btnDetalleEditar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lote == null) {
                    return;
                }
                LotesContrato.nuevaHojaEdicion(lote)
                        .show(getChildFragmentManager(), NuevoLoteFragment.TAG);
            }
        });
        binding.btnDetalleEliminar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmarEliminacion();
            }
        });
    }

    /** Pinta en la vista los datos del lote actual ({@link #lote}). */
    private void mostrarLote() {
        if (binding == null || lote == null) {
            return;
        }
        String nombre = lote.getNombre();
        String cultivo = lote.getCultivo();
        binding.textDetalleNombre.setText(
                nombre == null || nombre.isEmpty() ? getString(R.string.detalle_empty) : nombre);
        binding.textDetalleCultivo.setText(
                cultivo == null || cultivo.isEmpty() ? getString(R.string.detalle_dato_vacio) : cultivo);
        binding.textDetalleHectareas.setText(formatearHectareas(lote.getHectareas()));
        binding.textDetalleLoteId.setText(getString(R.string.detalle_lote_id_pattern, lote.getId()));
    }

    /** Pide confirmación antes de borrar; sin ella no se elimina nada. */
    private void confirmarEliminacion() {
        if (lote == null || lote.getId() == null || lote.getId().isEmpty()) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.eliminar_titulo)
                .setMessage(getString(R.string.eliminar_mensaje, lote.getNombre()))
                .setPositiveButton(R.string.eliminar_confirmar, (dialog, which) -> eliminarLote())
                .setNegativeButton(R.string.nuevo_lote_cancelar, null)
                .show();
    }

    /** Elimina el lote en Supabase; al éxito vuelve a la lista, al error reavisa y reactiva los botones. */
    private void eliminarLote() {
        binding.btnDetalleEditar.setEnabled(false);
        binding.btnDetalleEliminar.setEnabled(false);
        repositorio.eliminarLote(lote, new LoteRepository.Callback<Void>() {
            @Override
            public void onExito(Void datos) {
                if (binding != null) {
                    Toast.makeText(requireContext(), R.string.lote_eliminado, Toast.LENGTH_SHORT).show();
                }
                volver();
            }

            @Override
            public void onError(String mensaje) {
                if (binding == null) {
                    return;
                }
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
                binding.btnDetalleEditar.setEnabled(true);
                binding.btnDetalleEliminar.setEnabled(true);
            }
        });
    }

    /**
     * Convierte el texto crudo de hectáreas a número; si viene vacío o no es numérico
     * devuelve 0 (la vista ya formatea el resultado).
     */
    private static double parsearHectareas(String hectareasCrudas) {
        try {
            return Double.parseDouble(hectareasCrudas);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Pinta las hectáreas con el mismo formato que la lista ({@code lote_hectareas_formato},
     * sin decimales si son enteras).
     */
    private String formatearHectareas(double valor) {
        String sinDecimales = valor == Math.floor(valor)
                ? String.valueOf((long) valor)
                : String.valueOf(valor);
        return getString(R.string.lote_hectareas_formato, sinDecimales);
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
