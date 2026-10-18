package com.example.agentes.ui.lotes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.agentes.R;
import com.example.agentes.data.LoteRepository;
import com.example.agentes.databinding.FragmentNuevoLoteBinding;
import com.example.agentes.model.Lote;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.Serializable;

/**
 * Formulario para registrar un lote nuevo o editar uno existente.
 *
 * <p>Valida que el nombre no esté vacío, que el cultivo sea café, plátano o aguacate y que
 * las hectáreas sean un número mayor que 0. Si los datos son válidos guarda el lote en
 * Supabase; si no, muestra el error y no guarda.</p>
 *
 * <p>Si se le pasa un {@link Lote} por {@link LotesContrato#ARG_LOTE_EDITAR} (véase
 * {@link LotesContrato#nuevaHojaEdicion}), abre en modo edición: prellena los campos,
 * cambia títulos y al guardar envía un PATCH; al terminar devuelve el lote actualizado
 * con el FragmentResult {@link LotesContrato#ARG_RESULT_LOTE}.</p>
 */
public class NuevoLoteFragment extends BottomSheetDialogFragment {

    public static final String TAG = "NuevoLoteFragment";

    private FragmentNuevoLoteBinding binding;
    private LoteRepository repositorio;
    private Lote loteEditando;
    private boolean guardando;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNuevoLoteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repositorio = new LoteRepository();

        ArrayAdapter<String> adaptadorCultivos = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.cultivos));
        binding.menuCultivo.setAdapter(adaptadorCultivos);

        aplicarModoEdicion();

        binding.botonGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarLote();
            }
        });
        binding.botonCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Si la hoja recibió un lote para editar, prellena el formulario y cambia los textos
     * de título y botón a los de "editar"; si no, conserva el modo "nuevo lote".
     */
    private void aplicarModoEdicion() {
        Bundle arguments = getArguments();
        if (arguments == null) {
            return;
        }
        Serializable argumento = arguments.getSerializable(LotesContrato.ARG_LOTE_EDITAR);
        if (!(argumento instanceof Lote)) {
            return;
        }
        loteEditando = (Lote) argumento;
        binding.textoTituloFormulario.setText(R.string.nuevo_lote_editar_titulo);
        binding.botonGuardar.setText(R.string.nuevo_lote_actualizar);
        binding.entradaNombre.setText(loteEditando.getNombre());
        binding.menuCultivo.setText(loteEditando.getCultivo(), false);
        binding.entradaHectareas.setText(String.valueOf(loteEditando.getHectareas()));
    }

    private void guardarLote() {
        if (guardando) {
            return;
        }
        String nombre = texto(binding.entradaNombre);
        String cultivo = binding.menuCultivo.getText() == null
                ? "" : binding.menuCultivo.getText().toString().trim();
        String hectareasTexto = texto(binding.entradaHectareas);

        limpiarErrores();
        if (!validar(nombre, cultivo, hectareasTexto)) {
            return;
        }

        double hectareas = Double.parseDouble(hectareasTexto.replace(',', '.'));

        if (loteEditando != null) {
            actualizarLoteExistente(nombre, cultivo, hectareas);
        } else {
            crearLoteNuevo(nombre, cultivo, hectareas);
        }
    }

    private void crearLoteNuevo(String nombre, String cultivo, double hectareas) {
        Lote lote = new Lote(nombre, cultivo, hectareas);

        mostrarProgreso(true);
        repositorio.crearLote(lote, new LoteRepository.Callback<Lote>() {
            @Override
            public void onExito(Lote datos) {
                if (binding == null) {
                    return;
                }
                mostrarProgreso(false);
                Toast.makeText(requireContext(), R.string.nuevo_lote_guardado, Toast.LENGTH_SHORT).show();
                getParentFragmentManager().setFragmentResult(LotesContrato.RESULT_NUEVO_LOTE, new Bundle());
                dismiss();
            }

            @Override
            public void onError(String mensaje) {
                mostrarErrorGuardado(mensaje);
            }
        });
    }

    /**
     * Guarda los cambios sobre el lote en edición (PATCH), conservando su id y su dueño.
     * Al terminar devuelve el lote actualizado con {@link LotesContrato#ARG_RESULT_LOTE}.
     */
    private void actualizarLoteExistente(String nombre, String cultivo, double hectareas) {
        Lote lote = new Lote(nombre, cultivo, hectareas);
        lote.setId(loteEditando.getId());
        lote.setUsuarioId(loteEditando.getUsuarioId());

        mostrarProgreso(true);
        repositorio.actualizarLote(lote, new LoteRepository.Callback<Lote>() {
            @Override
            public void onExito(Lote datos) {
                if (binding == null) {
                    return;
                }
                mostrarProgreso(false);
                Toast.makeText(requireContext(), R.string.lote_actualizado, Toast.LENGTH_SHORT).show();
                Bundle resultado = new Bundle();
                resultado.putSerializable(LotesContrato.ARG_RESULT_LOTE, datos);
                getParentFragmentManager().setFragmentResult(LotesContrato.ARG_RESULT_LOTE, resultado);
                dismiss();
            }

            @Override
            public void onError(String mensaje) {
                mostrarErrorGuardado(mensaje);
            }
        });
    }

    /** Muestra el mensaje de error del servidor dentro de la hoja y rehabilita los botones. */
    private void mostrarErrorGuardado(String mensaje) {
        if (binding == null) {
            return;
        }
        mostrarProgreso(false);
        binding.textoError.setText(mensaje);
        binding.textoError.setVisibility(View.VISIBLE);
    }

    /**
     * Comprueba las reglas del formulario. Devuelve {@code true} solo si los datos son válidos.
     */
    private boolean validar(String nombre, String cultivo, String hectareasTexto) {
        if (nombre.isEmpty()) {
            binding.campoNombre.setError(getString(R.string.error_nombre_obligatorio));
            binding.entradaNombre.requestFocus();
            return false;
        }
        if (!Lote.esCultivoValido(cultivo)) {
            binding.campoCultivo.setError(getString(R.string.error_cultivo_obligatorio));
            binding.menuCultivo.requestFocus();
            return false;
        }
        double hectareas;
        try {
            hectareas = Double.parseDouble(hectareasTexto.replace(',', '.'));
        } catch (NumberFormatException e) {
            binding.campoHectareas.setError(getString(R.string.error_hectareas));
            binding.entradaHectareas.requestFocus();
            return false;
        }
        if (!(hectareas > 0)) {
            binding.campoHectareas.setError(getString(R.string.error_hectareas));
            binding.entradaHectareas.requestFocus();
            return false;
        }
        return true;
    }

    private void limpiarErrores() {
        binding.campoNombre.setError(null);
        binding.campoCultivo.setError(null);
        binding.campoHectareas.setError(null);
        binding.textoError.setVisibility(View.GONE);
        binding.textoError.setText("");
    }

    private void mostrarProgreso(boolean visible) {
        guardando = visible;
        binding.progresoFormulario.setVisibility(visible ? View.VISIBLE : View.GONE);
        binding.botonGuardar.setEnabled(!visible);
        binding.botonCancelar.setEnabled(!visible);
    }

    private static String texto(EditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
