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
import com.google.android.material.snackbar.Snackbar;

/**
 * Pantalla de inicio de sesión de AgroControl.
 *
 * <p>Credenciales: <b>nombre visible + correo electrónico</b> (la tabla
 * {@code usuarios} de Supabase no tiene contraseñas). Acciones: iniciar
 * sesión, casilla "Recordarme" y enlace "¿No estás registrado? Regístrate",
 * que abre el bottom sheet de registro.</p>
 */
public class LoginFragment extends Fragment {

    private static final String RESULTADO_REGISTRO = "registro_exitoso";

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
        if (SupabaseConfig.haySesion() || SupabaseConfig.restaurarSesion(requireContext())) {
            navegarAApp();
            return;
        }

        binding.checkboxRecordarme.setChecked(true);

        binding.botonIniciarSesion.setOnClickListener(v -> intentarLogin());

        binding.textoRegistrarse.setOnClickListener(v ->
                new RegistrarseFragment().show(getParentFragmentManager(), "registro"));

        // Tras registrarse NO se entra a la app: se avisa "Usuario registrado"
        // y el usuario debe escribir su nombre y correo para iniciar sesión.
        getParentFragmentManager().setFragmentResultListener(
                RESULTADO_REGISTRO, getViewLifecycleOwner(), (clave, datos) -> {
                    limpiarErrores();
                    binding.campoNombre.getEditText().setText("");
                    binding.campoCorreo.getEditText().setText("");
                    binding.campoNombre.requestFocus();
                    Snackbar.make(binding.getRoot(), R.string.registro_exitoso,
                            Snackbar.LENGTH_LONG).show();
                });
    }

    private void intentarLogin() {
        if (enCurso) {
            return;
        }

        String nombre = textoDe(binding.campoNombre.getEditText());
        String correo = textoDe(binding.campoCorreo.getEditText());

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

        mostrarCarga(true);

        repositorio.iniciarSesion(correo.trim(), new AuthRepository.CallbackConsulta() {
            @Override
            public void onResultado(String usuarioId, String nombreRegistrado) {
                mostrarCarga(false);
                if (!LoginValidador.nombresCoinciden(nombre, nombreRegistrado)) {
                    binding.campoNombre.setError(getString(R.string.login_error_nombre_no_coincide));
                    binding.campoNombre.requestFocus();
                    return;
                }
                SupabaseConfig.setSesion("", "", usuarioId, nombre.trim(),
                        correo.trim().toLowerCase(java.util.Locale.ROOT));
                SupabaseConfig.guardarSesion(requireContext(),
                        binding.checkboxRecordarme.isChecked());
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
        if (tipoError == AuthRepository.ERROR_NO_REGISTRADO) {
            return getString(R.string.login_error_no_registrado);
        }
        if (tipoError == AuthRepository.ERROR_PERMISOS) {
            return getString(R.string.login_error_permisos);
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
