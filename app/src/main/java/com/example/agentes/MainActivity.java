package com.example.agentes;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.agentes.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

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
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // El login es pantalla completa: se ocultan el toolbar y el FAB de la plantilla.
        navController.addOnDestinationChangedListener((controlador, destino, argumentos) -> {
            boolean esLogin = destino.getId() == R.id.loginFragment;
            binding.toolbar.setVisibility(esLogin ? View.GONE : View.VISIBLE);
            binding.fab.setVisibility(esLogin ? View.GONE : View.VISIBLE);
        });

        // Botón flotante original
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Probando conexión con Supabase...", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .setAction("Action", null).show();
            }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}