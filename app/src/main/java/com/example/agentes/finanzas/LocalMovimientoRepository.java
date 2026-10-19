package com.example.agentes.finanzas;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Guarda los movimientos en el dispositivo (SharedPreferences, JSON). Sobrevive al cierre de la app. */
public class LocalMovimientoRepository implements MovimientoRepository {

    private static final String PREFS = "agrocontrol_finanzas";
    private static final String CLAVE = "movimientos";

    private final SharedPreferences prefs;

    public LocalMovimientoRepository(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public void listarPorLote(String loteId, Callback<List<Movimiento>> callback) {
        try {
            List<Movimiento> resultado = new ArrayList<>();
            for (Movimiento m : leerTodos()) {
                if (m.getLoteId().equals(loteId)) {
                    resultado.add(m);
                }
            }
            callback.onExito(resultado);
        } catch (JSONException e) {
            callback.onError("No se pudieron leer los movimientos.");
        }
    }

    @Override
    public void agregar(Movimiento movimiento, Callback<Movimiento> callback) {
        try {
            List<Movimiento> todos = leerTodos();
            todos.add(movimiento);
            JSONArray arreglo = new JSONArray();
            for (Movimiento m : todos) {
                arreglo.put(aJson(m));
            }
            prefs.edit().putString(CLAVE, arreglo.toString()).apply();
            callback.onExito(movimiento);
        } catch (JSONException e) {
            callback.onError("No se pudo guardar el movimiento.");
        }
    }

    private List<Movimiento> leerTodos() throws JSONException {
        List<Movimiento> lista = new ArrayList<>();
        JSONArray arreglo = new JSONArray(prefs.getString(CLAVE, "[]"));
        for (int i = 0; i < arreglo.length(); i++) {
            JSONObject o = arreglo.getJSONObject(i);
            lista.add(new Movimiento(
                    o.getString("id"),
                    o.getString("loteId"),
                    o.getString("tipo"),
                    o.getString("categoria"),
                    o.getLong("monto"),
                    o.optString("descripcion", ""),
                    o.getString("fecha"),
                    o.optLong("creadoEn", 0L)));
        }
        return lista;
    }

    private JSONObject aJson(Movimiento m) throws JSONException {
        return new JSONObject()
                .put("id", m.getId())
                .put("loteId", m.getLoteId())
                .put("tipo", m.getTipo())
                .put("categoria", m.getCategoria())
                .put("monto", m.getMonto())
                .put("descripcion", m.getDescripcion())
                .put("fecha", m.getFecha())
                .put("creadoEn", m.getCreadoEn());
    }
}
