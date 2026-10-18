package com.example.agentes.ui.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.agentes.R;
import com.example.agentes.data.AuthRepository;
import com.example.agentes.databinding.FragmentRecuperarPasswordBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet de recuperación de contraseña: pide el correo y dispara el
 * enlace de restablecimiento de Supabase Auth.
 */
public class RecuperarPasswordFragment extends BottomSheetDialogFragment {

    private FragmentRecuperarPasswordBinding binding;
    private AuthRepository repositorio;
    private boolean enCurso = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRecuperarPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repositorio = new AuthRepository();

        binding.botonCancelar.setOnClickListener(v -> dismiss());

        binding.botonEnviar.setOnClickListener(v -> enviarEnlace());
    }

    private void enviarEnlace() {
        if (enCurso) {
            return;
        }

        String correo = textoDe(binding.campoCorreo.getEditText()).trim();

        binding.campoCorreo.setError(null);
        binding.textoError.setVisibility(View.GONE);

        if (!LoginValidador.esCorreoValido(correo)) {
            binding.campoCorreo.setError(getString(R.string.login_error_correo_invalido));
            binding.campoCorreo.requestFocus();
            return;
        }

        mostrarCarga(true);

        repositorio.recuperarContrasena(correo, new AuthRepository.CallbackEnvio() {
            @Override
            public void onEnviado() {
                mostrarCarga(false);
                // Supabase responde 200 aunque el correo no exista (privacidad).
                binding.textoError.setTextColor(
                        getResources().getColor(R.color.agro_primary, requireContext().getTheme()));
                binding.textoError.setText(R.string.recuperar_exito);
                binding.textoError.setVisibility(View.VISIBLE);
                binding.botonEnviar.setEnabled(false);
                binding.campoCorreo.setEnabled(false);
            }

            @Override
            public void onError(int tipoError, String detalle) {
                mostrarCarga(false);
                binding.textoError.setTextColor(
                        getResources().getColor(R.color.agro_error, requireContext().getTheme()));
                binding.textoError.setText(tipoError == AuthRepository.ERROR_RED
                        ? R.string.recuperar_error_red : R.string.recuperar_error_servidor);
                binding.textoError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void mostrarCarga(boolean cargando) {
        enCurso = cargando;
        binding.botonEnviar.setEnabled(!cargando);
        binding.progressCarga.setVisibility(cargando ? View.VISIBLE : View.GONE);
        binding.botonEnviar.setText(
                cargando ? R.string.recuperar_en_curso : R.string.recuperar_boton);
    }

    private static String textoDe(android.widget.EditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
