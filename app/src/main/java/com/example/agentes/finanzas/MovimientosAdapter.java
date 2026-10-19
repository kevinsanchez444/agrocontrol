package com.example.agentes.finanzas;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agentes.R;
import com.example.agentes.databinding.ItemMovimientoBinding;

import java.util.ArrayList;
import java.util.List;

/** Lista del historial de movimientos de un lote. */
public class MovimientosAdapter extends RecyclerView.Adapter<MovimientosAdapter.Holder> {

    private final List<Movimiento> items = new ArrayList<>();

    public void setItems(List<Movimiento> nuevos) {
        items.clear();
        items.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemMovimientoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Movimiento m = items.get(position);
        h.binding.txtCategoria.setText(m.getCategoria());

        String detalle = FormatoUtil.fechaVisible(m.getFecha());
        if (!m.getDescripcion().isEmpty()) {
            detalle += " · " + m.getDescripcion();
        }
        h.binding.txtDetalle.setText(detalle);

        boolean ingreso = m.esIngreso();
        String monto = FormatoUtil.moneda(ingreso ? m.getMonto() : -m.getMonto());
        h.binding.txtMonto.setText(ingreso ? "+" + monto : monto);
        h.binding.txtMonto.setTextColor(ContextCompat.getColor(
                h.binding.getRoot().getContext(),
                ingreso ? R.color.agro_ingreso : R.color.agro_gasto));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemMovimientoBinding binding;

        Holder(ItemMovimientoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
