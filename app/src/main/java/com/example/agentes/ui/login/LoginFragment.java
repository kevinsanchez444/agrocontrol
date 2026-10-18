package com.example.agentes.ui.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.agentes.R;
import com.example.agentes.data.AuthRepository;
import com.example.agentes.data.SupabaseConfig;
import com.example.agentes.databinding.FragmentLoginBinding;

/**
 * Pantalla de inicio de sesión de AgroControl.
 *
 * <p>Campos: nombre visible, correo y contraseña; acciones: iniciar sesión,
 * "Recordarme" (persiste la sesión en SharedPreferences) y enlace de
 * recuperación de contraseña. La autenticación va contra Supabase Auth.</p>
 */
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthRepository repositorio;
    private boolean enCurso = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repositorio = new AuthRepository();

        // Sesión guardada con "Recordarme": entrar directo a la app.
        if (SupabaseConfig.haySesion()) {
            navegarAApp();
            return;
        }

        binding.checkboxRecordarme.setChecked(true);

        binding.botonIniciarSesion.setOnClickListener(v -> intentarLogin());

        binding.textoOlvideContrasena.setOnClickListener(v ->
                new RecuperarPasswordFragment().show(
                        getParentFragmentManager(), "recuperar_password"));
    }

    private void intentarLogin() {
        if (enCurso) {
            return;
        }

        String nombre = textoDe(binding.campoNombre.getEditText());
        String correo = textoDe(binding.campoCorreo.getEditText());
        String contrasena = textoDe(binding.campoContrasena.getEditText());

        limpiarErrores();

        if (!LoginValidador.esNombreValido(nombre)) {
            mostrarErrorCampo(binding.campoNombre, R.string.login_error_nombre);
            return;
        }
        if (correo.isEmpty()) {
            mostrarErrorCampo(binding.campoCorreo, R.string.login_error_correo_obligatorio);
            return;
        }
        if (!LoginValidador.esCorreoValido(correo)) {
            mostrarErrorCampo(binding.campoCorreo, R.string.login_error_correo_invalido);
            return;
        }
        if (contrasena.isEmpty()) {
            mostrarErrorCampo(binding.campoContrasena, R.string.login_error_contrasena_obligatoria);
            return;
        }
        if (!LoginValidador.esContrasenaValida(contrasena)) {
            mostrarErrorCampo(binding.campoContrasena, R.string.login_error_contrasena_corta);
            return;
        }

        mostrarCarga(true);

        repositorio.iniciarSesion(correo.trim(), contrasena, new AuthRepository.CallbackSesion() {
            @Override
            public void onSesionIniciada(String accessToken, String refreshToken, String usuarioId) {
                // El nombre es local (identificador visible); el correo lo aporta Supabase.
                SupabaseConfig.setSesion(accessToken, refreshToken, usuarioId,
                        nombre.trim(), correo.trim());
                SupabaseConfig.guardarSesion(requireContext(),
                        binding.checkboxRecordarme.isChecked());
                mostrarCarga(false);
                navegarAApp();
            }

            @Override
            public void onError(int tipoError, String detalle) {
                mostrarCarga(false);
                binding.textoError.setText(mensajeDe(tipoError));
                binding.textoError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void limpiarErrores() {
        binding.campoNombre.setError(null);
        binding.campoCorreo.setError(null);
        binding.campoContrasena.setError(null);
        binding.textoError.setVisibility(View.GONE);
    }

    private void mostrarErrorCampo(com.google.android.material.textfield.TextInputLayout campo,
                                    int mensaje) {
        campo.setError(getString(mensaje));
        campo.requestFocus();
    }

    private void mostrarCarga(boolean cargando) {
        enCurso = cargando;
        binding.botonIniciarSesion.setEnabled(!cargando);
        binding.progressCarga.setVisibility(cargando ? View.VISIBLE : View.GONE);
        binding.botonIniciarSesion.setText(
                cargando ? R.string.login_en_curso : R.string.login_boton);
    }

    private String mensajeDe(int tipoError) {
        if (tipoError == AuthRepository.ERROR_CREDENCIALES) {
            return getString(R.string.login_error_credenciales);
        }
        if (tipoError == AuthRepository.ERROR_RED) {
            return getString(R.string.login_error_red);
        }
        return getString(R.string.login_error_servidor);
    }

    private void navegarAApp() {
        NavHostFragment.findNavController(LoginFragment.this)
                .navigate(R.id.action_loginFragment_to_FirstFragment);
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
