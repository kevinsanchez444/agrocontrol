package com.example.agentes.ui.lotes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.agentes.R;
import com.example.agentes.data.LoteRepository;
import com.example.agentes.databinding.FragmentLotesBinding;
import com.example.agentes.model.Lote;
import com.example.agentes.navigation.NavArgsContract;
import com.example.agentes.ui.EstadoViews;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;
import java.util.List;

/**
 * Pantalla "Mis lotes": lista los lotes del usuario en Supabase y permite crear uno nuevo.
 *
 * <p>Es el modulo del Integrante 2 (model Lote, LoteRepository, LotesAdapter y
 * {@link NuevoLoteFragment}) integrado en el destino {@code lotesFragment} del nav_graph
 * principal. Se conservan los 4 IDs de estado y el contrato de argumentos
 * ({@link NavArgsContract#ARG_ABRIR_FORMULARIO}) definidos por la navegacion.</p>
 */
public class LotesFragment extends Fragment {

    private static final String TAG = "LotesFragment";

    private FragmentLotesBinding binding;
    private EstadoViews estados;
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
        estados = new EstadoViews(
                binding.stateEmpty, binding.stateLoading, binding.stateError, binding.stateContent);

        adaptador = new LotesAdapter(new LotesAdapter.OnLoteClickListener() {
            @Override
            public void onLoteClick(Lote lote) {
                abrirDetalleLote(lote);
            }
        });
        binding.listaLotes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listaLotes.setAdapter(adaptador);

        binding.btnLotesNuevo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirFormularioNuevoLote();
            }
        });
        binding.btnLotesReintentar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cargarLotes();
            }
        });

        getChildFragmentManager().setFragmentResultListener(
                LotesContrato.RESULT_NUEVO_LOTE, this, new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                        cargarLotes();
                    }
                });

        consumirArgumentoFormulario();
        cargarLotes();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** Consulta los lotes en Supabase y actualiza la lista y los estados (carga/vacio/error). */
    private void cargarLotes() {
        if (binding == null) {
            return;
        }
        estados.mostrar(EstadoViews.CARGA);
        repositorio.listarLotes(new LoteRepository.Callback<List<Lote>>() {
            @Override
            public void onExito(List<Lote> datos) {
                if (binding == null) {
                    return;
                }
                adaptador.setLotes(datos);
                estados.mostrar(datos.isEmpty() ? EstadoViews.VACIO : EstadoViews.CONTENIDO);
            }

            @Override
            public void onError(String mensaje) {
                if (binding == null) {
                    return;
                }
                adaptador.setLotes(Collections.<Lote>emptyList());
                estados.mostrar(EstadoViews.ERROR);
                Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void abrirFormularioNuevoLote() {
        // El formulario se muestra como hoja inferior, de modo que el modulo funciona sin
        // tocar el nav_graph (contrato original del Integrante 2).
        new NuevoLoteFragment().show(getChildFragmentManager(), NuevoLoteFragment.TAG);
    }

    /** Abre el detalle del lote mediante la accion declarada en el nav_graph. */
    private void abrirDetalleLote(Lote lote) {
        Bundle argumentos = LotesContrato.argumentosDetalleLote(lote);
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_lotesFragment_to_loteDetalleFragment, argumentos);
    }

    /**
     * Respeta el contrato de navegacion: si Home pidio abrir el formulario
     * (argumento {@code abrir_formulario}), se abre la hoja inferior una sola vez.
     */
    private void consumirArgumentoFormulario() {
        Bundle args = getArguments();
        if (args != null && args.getBoolean(NavArgsContract.ARG_ABRIR_FORMULARIO, false)) {
            args.putBoolean(NavArgsContract.ARG_ABRIR_FORMULARIO, false);
            abrirFormularioNuevoLote();
        }
    }
}
