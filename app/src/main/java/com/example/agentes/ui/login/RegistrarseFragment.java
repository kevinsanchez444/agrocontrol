package com.example.agentes.ui.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.agentes.R;
import com.example.agentes.data.AuthRepository;
import com.example.agentes.databinding.FragmentRegistroBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet de registro: pide nombre y correo (las credenciales del MVP)
 * y crea la fila en la tabla {@code usuarios} de Supabase. Al terminar avisa
 * a {@link LoginFragment} con un FragmentResult para entrar directo a la app.
 */
public class RegistrarseFragment extends BottomSheetDialogFragment {

    private static final String RESULTADO_REGISTRO = "registro_exitoso";

    private FragmentRegistroBinding binding;
    private AuthRepository repositorio;
    private boolean enCurso = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegistroBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repositorio = new AuthRepository();

        binding.botonCancelar.setOnClickListener(v -> dismiss());

        binding.botonRegistrarse.setOnClickListener(v -> registrar());
    }

    private void registrar() {
        if (enCurso) {
            return;
        }

        String nombre = textoDe(binding.campoNombre.getEditText()).trim();
        String correo = textoDe(binding.campoCorreo.getEditText()).trim();

        binding.campoNombre.setError(null);
        binding.campoCorreo.setError(null);
        binding.textoError.setVisibility(View.GONE);

        if (!LoginValidador.esNombreValido(nombre)) {
            binding.campoNombre.setError(getString(R.string.login_error_nombre));
            binding.campoNombre.requestFocus();
            return;
        }
        if (!LoginValidador.esCorreoValido(correo)) {
            binding.campoCorreo.setError(getString(R.string.login_error_correo_invalido));
            binding.campoCorreo.requestFocus();
            return;
        }

        mostrarCarga(true);

        repositorio.registrarse(nombre, correo, new AuthRepository.CallbackRegistro() {
            @Override
            public void onRegistrado(String usuarioId) {
                mostrarCarga(false);
                Bundle datos = new Bundle();
                datos.putString("usuario_id", usuarioId);
                datos.putString("nombre", nombre);
                datos.putString("email", correo);
                getParentFragmentManager().setFragmentResult(RESULTADO_REGISTRO, datos);
                dismiss();
            }

            @Override
            public void onError(int tipoError, String detalle) {
                mostrarCarga(false);
                binding.textoError.setText(mensajeDe(tipoError));
                binding.textoError.setVisibility(View.VISIBLE);
            }
        });
    }

    private String mensajeDe(int tipoError) {
        if (tipoError == AuthRepository.ERROR_YA_REGISTRADO) {
            return getString(R.string.registro_error_ya_registrado);
        }
        if (tipoError == AuthRepository.ERROR_PERMISOS) {
            return getString(R.string.login_error_permisos);
        }
        if (tipoError == AuthRepository.ERROR_RED) {
            return getString(R.string.registro_error_red);
        }
        return getString(R.string.registro_error_servidor);
    }

    private void mostrarCarga(boolean cargando) {
        enCurso = cargando;
        binding.botonRegistrarse.setEnabled(!cargando);
        binding.progressCarga.setVisibility(cargando ? View.VISIBLE : View.GONE);
        binding.botonRegistrarse.setText(
                cargando ? R.string.registro_en_curso : R.string.registro_boton);
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
