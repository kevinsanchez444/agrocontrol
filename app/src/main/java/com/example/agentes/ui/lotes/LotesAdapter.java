package com.example.agentes.ui.lotes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agentes.R;
import com.example.agentes.databinding.ItemLoteBinding;
import com.example.agentes.model.Lote;

import java.util.ArrayList;
import java.util.List;

/** Muestra cada lote como una tarjeta con su nombre, su cultivo y sus hectáreas. */
public class LotesAdapter extends RecyclerView.Adapter<LotesAdapter.LoteViewHolder> {

    /** Notifica cuando el usuario toca una tarjeta. */
    public interface OnLoteClickListener {
        void onLoteClick(Lote lote);
    }

    private final List<Lote> lotes = new ArrayList<>();
    private final OnLoteClickListener escucha;

    public LotesAdapter(OnLoteClickListener escucha) {
        this.escucha = escucha;
    }

    /** Reemplaza el contenido de la lista. */
    public void setLotes(List<Lote> nuevos) {
        lotes.clear();
        if (nuevos != null) {
            lotes.addAll(nuevos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLoteBinding binding = ItemLoteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new LoteViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LoteViewHolder holder, int position) {
        holder.enlazar(lotes.get(position));
    }

    @Override
    public int getItemCount() {
        return lotes.size();
    }

    class LoteViewHolder extends RecyclerView.ViewHolder {

        private final ItemLoteBinding binding;

        LoteViewHolder(ItemLoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void enlazar(final Lote lote) {
            Context contexto = binding.getRoot().getContext();
            binding.textoNombre.setText(lote.getNombre());
            binding.textoCultivo.setText(lote.getCultivo());
            binding.textoHectareas.setText(
                    contexto.getString(R.string.lote_hectareas_formato, formatearHectareas(lote.getHectareas())));
            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    escucha.onLoteClick(lote);
                }
            });
        }

        private String formatearHectareas(double hectareas) {
            if (hectareas == Math.floor(hectareas)) {
                return String.valueOf((long) hectareas);
            }
            return String.valueOf(hectareas);
        }
    }
}
