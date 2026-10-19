package com.example.agentes.finanzas;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Formato de dinero (COP) y fechas. Sin dependencias de Android para poder probarlo con JUnit. */
public final class FormatoUtil {

    private FormatoUtil() { }

    /** Ej.: -300000 → "-$300.000 COP". */
    public static String moneda(long valor) {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.US);
        simbolos.setGroupingSeparator('.');
        String numero = new DecimalFormat("#,##0", simbolos).format(Math.abs(valor));
        return (valor < 0 ? "-" : "") + "$" + numero + " COP";
    }

    /** Fecha local de hoy en formato ISO yyyy-MM-dd. */
    public static String hoyIso() {
        Calendar c = Calendar.getInstance();
        return iso(c);
    }

    /** Convierte los millis UTC que entrega MaterialDatePicker a yyyy-MM-dd. */
    public static String isoDesdeUtcMillis(long millisUtc) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(millisUtc);
        return iso(c);
    }

    /** "2026-10-18" → "18/10/2026". Si el formato no es válido, devuelve el texto tal cual. */
    public static String fechaVisible(String iso) {
        if (iso == null) {
            return "";
        }
        String[] p = iso.split("-");
        if (p.length != 3) {
            return iso;
        }
        return p[2] + "/" + p[1] + "/" + p[0];
    }

    private static String iso(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }
}
