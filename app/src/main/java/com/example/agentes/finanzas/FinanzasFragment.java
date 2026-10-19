package com.example.agentes.finanzas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.agentes.R;
import com.example.agentes.databinding.DialogMovimientoBinding;
import com.example.agentes.databinding.FragmentFinanzasBinding;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.UUID;

/**
 * Detalle financiero de un lote (Integrante 3).
 *
 * Argumentos de navegación (ver nav_graph.xml):
 *  - loteId     (String) identificador del lote seleccionado.
 *  - loteNombre (String) nombre que se muestra en el encabezado.
 */
public class FinanzasFragment extends Fragment {

    public static final String ARG_LOTE_ID = "loteId";
    public static final String ARG_LOTE_NOMBRE = "loteNombre";

    private FragmentFinanzasBinding binding;
    private MovimientoRepository repositorio;
    private MovimientosAdapter adapter;
    private String loteId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFinanzasBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        loteId = args != null ? args.getString(ARG_LOTE_ID, "") : "";
        String nombre = args != null ? args.getString(ARG_LOTE_NOMBRE, "") : "";
        // Integración: el destino finanzasFragment (bottom nav / Home) no envía argumentos;
        // en ese caso se muestra el título del destino como encabezado.
        binding.txtLoteNombre.setText(nombre.isEmpty() ? getString(R.string.fin_title) : nombre);

        // Para conectar Supabase: reemplazar esta línea por la implementación remota.
        repositorio = new LocalMovimientoRepository(requireContext());

        adapter = new MovimientosAdapter();
        binding.rvMovimientos.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMovimientos.setAdapter(adapter);

        binding.btnRegistrar.setOnClickListener(v -> mostrarFormulario());
        cargar();
    }

    private void cargar() {
        repositorio.listarPorLote(loteId, new MovimientoRepository.Callback<List<Movimiento>>() {
            @Override
            public void onExito(List<Movimiento> lista) {
                if (binding == null) {
                    return;
                }
                mostrar(lista);
            }

            @Override
            public void onError(String mensaje) {
                if (binding != null) {
                    Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void mostrar(List<Movimiento> lista) {
        Balance balance = CalculadoraBalance.calcular(lista);
        long saldo = balance.getSaldo();

        binding.txtSaldo.setText(FormatoUtil.moneda(saldo));
        binding.txtSaldo.setTextColor(ContextCompat.getColor(requireContext(),
                saldo < 0 ? R.color.agro_gasto : R.color.agro_ingreso));
        binding.txtIngresos.setText(getString(R.string.finanzas_ingresos_fmt,
                FormatoUtil.moneda(balance.getIngresos())));
        binding.txtGastos.setText(getString(R.string.finanzas_gastos_fmt,
                FormatoUtil.moneda(balance.getGastos())));

        adapter.setItems(CalculadoraBalance.ordenarPorFechaDesc(lista));
        binding.txtVacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void mostrarFormulario() {
        final DialogMovimientoBinding form = DialogMovimientoBinding.inflate(getLayoutInflater());
        final String[] fecha = { FormatoUtil.hoyIso() };
        form.etFecha.setText(FormatoUtil.fechaVisible(fecha[0]));

        form.toggleTipo.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                cargarCategorias(form, tipoSeleccionado(form));
            }
        });
        form.toggleTipo.check(R.id.btn_tipo_gasto);
        cargarCategorias(form, Movimiento.TIPO_GASTO);

        form.etFecha.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.finanzas_fecha)
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();
            picker.addOnPositiveButtonClickListener(seleccion -> {
                fecha[0] = FormatoUtil.isoDesdeUtcMillis(seleccion);
                form.etFecha.setText(FormatoUtil.fechaVisible(fecha[0]));
            });
            picker.show(getChildFragmentManager(), "fecha_movimiento");
        });

        final AlertDialog dialogo = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.finanzas_registrar)
                .setView(form.getRoot())
                .setNegativeButton(R.string.finanzas_cancelar, null)
                .setPositiveButton(R.string.finanzas_guardar, null)
                .create();

        // Se sobreescribe el click para no cerrar el diálogo si hay errores de validación.
        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    if (guardar(form, fecha[0])) {
                        dialogo.dismiss();
                    }
                }));
        dialogo.show();
    }

    private void cargarCategorias(DialogMovimientoBinding form, String tipo) {
        List<String> categorias = CategoriasMovimiento.paraTipo(tipo);
        form.actCategoria.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, categorias));
        form.actCategoria.setText("", false);
        form.tilCategoria.setError(null);
    }

    private String tipoSeleccionado(DialogMovimientoBinding form) {
        return form.toggleTipo.getCheckedButtonId() == R.id.btn_tipo_ingreso
                ? Movimiento.TIPO_INGRESO : Movimiento.TIPO_GASTO;
    }

    /** Valida y guarda. Devuelve true si se guardó. */
    private boolean guardar(DialogMovimientoBinding form, String fechaIso) {
        String tipo = tipoSeleccionado(form);
        String categoria = String.valueOf(form.actCategoria.getText()).trim();
        String textoMonto = String.valueOf(form.etMonto.getText()).trim();
        String descripcion = String.valueOf(form.etDescripcion.getText()).trim();

        boolean valido = true;

        if (!CategoriasMovimiento.paraTipo(tipo).contains(categoria)) {
            form.tilCategoria.setError(getString(R.string.finanzas_error_categoria));
            valido = false;
        } else {
            form.tilCategoria.setError(null);
        }

        long monto = 0;
        try {
            monto = Long.parseLong(textoMonto);
        } catch (NumberFormatException e) {
            monto = 0;
        }
        if (monto <= 0) {
            form.tilMonto.setError(getString(R.string.finanzas_error_monto));
            valido = false;
        } else {
            form.tilMonto.setError(null);
        }

        if (!valido) {
            return false;
        }

        Movimiento nuevo = new Movimiento(UUID.randomUUID().toString(), loteId, tipo,
                categoria, monto, descripcion, fechaIso, System.currentTimeMillis());
        repositorio.agregar(nuevo, new MovimientoRepository.Callback<Movimiento>() {
            @Override
            public void onExito(Movimiento guardado) {
                if (binding == null) {
                    return;
                }
                cargar();
                Snackbar.make(binding.getRoot(), R.string.finanzas_guardado,
                        Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String mensaje) {
                if (binding != null) {
                    Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
                }
            }
        });
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
