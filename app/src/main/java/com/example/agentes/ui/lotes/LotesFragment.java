package com.example.agentes.ui.lotes;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.agentes.R;
import com.example.agentes.data.LoteRepository;
import com.example.agentes.databinding.FragmentLotesBinding;
import com.example.agentes.model.Lote;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;
import java.util.List;

/**
 * Pantalla "Mis lotes": lista los lotes del usuario y permite crear uno nuevo.
 *
 * <p>Forma parte del módulo del Integrante 2 (registro y listado de lotes). La navegación
 * entre pantallas la integra el Integrante 5; ver {@link LotesContrato} y
 * {@code docs/contrato-lotes.md}.</p>
 */
public class LotesFragment extends Fragment {

    private static final String TAG = "LotesFragment";

    private FragmentLotesBinding binding;
    private LotesAdapter adaptador;
    private LoteRepository repositorio;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLotesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repositorio = new LoteRepository();
        adaptador = new LotesAdapter(new LotesAdapter.OnLoteClickListener() {
            @Override
            public void onLoteClick(Lote lote) {
                abrirDetalleLote(lote);
            }
        });

        binding.listaLotes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listaLotes.setAdapter(adaptador);

        binding.botonNuevoLote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirFormularioNuevoLote();
            }
        });

        getChildFragmentManager().setFragmentResultListener(
                LotesContrato.RESULT_NUEVO_LOTE, this, new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                        cargarLotes();
                    }
                });

        cargarLotes();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** Consulta los lotes en Supabase y actualiza la lista y el estado vacío. */
    private void cargarLotes() {
        if (binding == null) {
            return;
        }
        mostrarProgreso(true);
        repositorio.listarLotes(new LoteRepository.Callback<List<Lote>>() {
            @Override
            public void onExito(List<Lote> datos) {
                if (binding == null) {
                    return;
                }
                mostrarProgreso(false);
                adaptador.setLotes(datos);
                actualizarEstadoVacio(datos.isEmpty());
            }

            @Override
            public void onError(String mensaje) {
                if (binding == null) {
                    return;
                }
                mostrarProgreso(false);
                adaptador.setLotes(Collections.<Lote>emptyList());
                actualizarEstadoVacio(true);
                Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void abrirFormularioNuevoLote() {
        // El formulario se muestra como hoja inferior, de modo que el módulo funciona sin
        // tocar el nav_graph. El Integrante 5 puede moverlo a una pantalla propia.
        new NuevoLoteFragment().show(getChildFragmentManager(), NuevoLoteFragment.TAG);
    }

    private void abrirDetalleLote(Lote lote) {
        Bundle argumentos = LotesContrato.argumentosDetalleLote(lote);
        // TODO(Integrante 5): sustituir por la navegación real al detalle del lote:
        //   findNavController().navigate(R.id.accionLotesADetalleLote, argumentos);
        Log.i(TAG, "Lote seleccionado. " + LotesContrato.ARG_LOTE_ID + "=" + argumentos.getString(LotesContrato.ARG_LOTE_ID));
        if (binding != null) {
            Snackbar.make(binding.getRoot(),
                    getString(R.string.lote_seleccionado, lote.getNombre(),
                            argumentos.getString(LotesContrato.ARG_LOTE_ID)),
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private void mostrarProgreso(boolean visible) {
        binding.progresoLotes.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void actualizarEstadoVacio(boolean vacio) {
        binding.textoVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        binding.listaLotes.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }
}
