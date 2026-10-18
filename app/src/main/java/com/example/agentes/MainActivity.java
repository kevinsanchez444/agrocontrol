package com.example.agentes;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.agentes.databinding.ActivityMainBinding;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        // Destinos de nivel superior: sin flecha "up" en el toolbar
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.homeFragment,
                R.id.lotesFragment,
                R.id.finanzasFragment,
                R.id.asesorFragment
        ).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.content.bottomNav, navController);

        // El login es pantalla completa: se ocultan el toolbar y el menú inferior.
        // (La plantilla traía también un FAB, pero no existe en los layouts: se omite.)
        navController.addOnDestinationChangedListener((controlador, destino, argumentos) -> {
            boolean esLogin = destino.getId() == R.id.loginFragment;
            binding.toolbar.setVisibility(esLogin ? View.GONE : View.VISIBLE);
            binding.content.bottomNav.setVisibility(esLogin ? View.GONE : View.VISIBLE);
        });

        // -------------------------------------------------------------
        // PRUEBA DE CONEXIÓN A SUPABASE
        // Cambia "tu_tabla" por el nombre real de una tabla en tu base de datos
        // -------------------------------------------------------------
        obtenerDatosDeTabla("categorias");
    }

    // Método para consultar datos en Supabase
    private void obtenerDatosDeTabla(String nombreTabla) {
        Request request = SupabaseClient.crearPeticionObtener(nombreTabla);
        OkHttpClient client = SupabaseClient.getClient();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SUPABASE", "Error al conectar: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // try-with-resources: cierra siempre el body y lee el mensaje real del servidor
                try (Response r = response) {
                    String cuerpo = r.body() != null ? r.body().string() : "";
                    if (r.isSuccessful()) {
                        Log.d("SUPABASE", "Respuesta recibida: " + cuerpo);
                    } else {
                        // El cuerpo trae el motivo exacto: p. ej. "permission denied for table",
                        // "Invalid API key", "relation ... does not exist"
                        Log.e("SUPABASE", "Error del servidor. Código: " + r.code() + " | " + cuerpo);
                    }
                }
            }
        });
    }

    // Método para guardar/insertar registros en Supabase
    private void insertarDatosEnTabla(String nombreTabla, String jsonDatos) {
        Request request = SupabaseClient.crearPeticionInsertar(nombreTabla, jsonDatos);
        OkHttpClient client = SupabaseClient.getClient();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SUPABASE", "Error al insertar: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response r = response) {
                    String cuerpo = r.body() != null ? r.body().string() : "";
                    if (r.isSuccessful()) {
                        Log.d("SUPABASE", "Registro guardado exitosamente: " + cuerpo);
                    } else {
                        Log.e("SUPABASE", "Error al guardar. Código: " + r.code() + " | " + cuerpo);
                    }
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
