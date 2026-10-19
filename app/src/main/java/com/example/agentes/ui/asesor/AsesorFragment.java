package com.example.agentes.ui.asesor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.agrocontrol.app.asesor.ChatViewModel;
import com.agrocontrol.app.asesor.ui.ChatAdapter;
import com.example.agentes.R;
import com.example.agentes.databinding.FragmentAsesorBinding;
import com.example.agentes.ui.EstadoViews;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Pestaña "Asesor": el chat con el Asesor IA vive DIRECTAMENTE aquí
 * (placeholder de sugerencias mientras el chat está vacío + historial en
 * RecyclerView + fila de entrada), sin Activitys intermedias. La lógica
 * (envío, respuesta, errores y reintento)
 * está en {@link ChatViewModel}, y las burbujas en {@link ChatAdapter};
 * este fragment solo observa y conecta la UI.
 * Nunca se incluye una clave de API dentro de la app (el backend/mock
 * resuelve la consulta).
 */
public class AsesorFragment extends Fragment {

    private FragmentAsesorBinding binding;
    private ChatViewModel viewModel;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentAsesorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EstadoViews estados = new EstadoViews(
                binding.stateEmpty, binding.stateLoading, binding.stateError, binding.stateContent);
        estados.mostrar(EstadoViews.CONTENIDO);

        // Ámbito de ACTIVIDAD (no `this`): la conversación es del usuario, no de
        // la vista del fragment. `new ViewModelProvider(this)` la ataba al
        // fragment y se perdía al cambiar de pestaña; con la Activity sobrevive
        // a la recreación de la vista y a la navegación entre pestañas.
        viewModel = new ViewModelProvider(requireActivity()).get(ChatViewModel.class);

        // Historial de la conversación en el RecyclerView del propio fragment
        ChatAdapter adapter = new ChatAdapter();
        binding.listaMensajes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listaMensajes.setAdapter(adapter);

        viewModel.getMensajes().observe(getViewLifecycleOwner(), mensajes -> {
            if (binding == null) {
                return;
            }
            boolean vacio = mensajes.isEmpty();
            // Placeholder de sugerencias: solo mientras no hay mensajes en el chat
            binding.textAsesorPlaceholder.setVisibility(vacio ? View.VISIBLE : View.GONE);
            // El botón "Limpiar" solo tiene sentido cuando hay conversación
            binding.btnAsesorLimpiar.setVisibility(vacio ? View.GONE : View.VISIBLE);
            // submitList aplica DiffUtil en background: el scroll se hace al confirmar
            adapter.submitList(mensajes, () -> {
                if (binding != null && !mensajes.isEmpty()) {
                    binding.listaMensajes.scrollToPosition(mensajes.size() - 1);
                }
            });
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            // Aviso breve; el detalle ya queda en la burbuja (EstadoMensaje.ERROR)
            if (error != null && binding != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnAsesorSend.setOnClickListener(v -> enviarMensaje());

        // Borra la conversación (memoria + Supabase) tras confirmar
        binding.btnAsesorLimpiar.setOnClickListener(v -> confirmarLimpiar());

        // Acción IME "send" del teclado virtual
        binding.inputAsesor.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                enviarMensaje();
                return true;
            }
            return false;
        });
    }

    /** Diálogo de confirmación antes de borrar el historial del chat. */
    private void confirmarLimpiar() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.ia_limpiar_titulo)
                .setMessage(R.string.ia_limpiar_mensaje)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.ia_limpiar_confirmar,
                        (dialogo, which) -> viewModel.limpiarConversacion())
                .show();
    }

    private void enviarMensaje() {
        if (binding == null) {
            return;
        }
        String texto = binding.inputAsesor.getText().toString().trim();
        if (texto.isEmpty()) {
            return;
        }
        viewModel.enviar(texto);
        binding.inputAsesor.setText("");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
