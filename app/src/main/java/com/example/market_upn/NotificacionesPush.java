package com.example.market_upn;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.google.firebase.messaging.FirebaseMessaging;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;


public class NotificacionesPush {

    public static final String CANAL_ID = "nuevos_anuncios";

    public static final String[] CATEGORIAS = {"Ingeniería", "Ciencias", "Arquitectura", "Salud"};

    public static String topicDe(String categoria) {
        String sinTildes = Normalizer.normalize(categoria, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return "cat_" + sinTildes.toLowerCase(Locale.ROOT).replace(" ", "_");
    }

    public static void crearCanal(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_ID, "Nuevos anuncios", NotificationManager.IMPORTANCE_HIGH);
            canal.setDescription("Avisos de anuncios nuevos en tus categorías de interés");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(canal);
        }
    }

    public static void sincronizar(List<String> intereses) {
        for (String categoria : CATEGORIAS) {
            if (intereses.contains(categoria)) {
                FirebaseMessaging.getInstance().subscribeToTopic(topicDe(categoria));
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(topicDe(categoria));
            }
        }
    }

    public static void cancelarTodas() {
        for (String categoria : CATEGORIAS) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topicDe(categoria));
        }
    }

    public static String categoriaPorCarrera(String carrera) {
        if (carrera == null) return null;
        String c = carrera.toLowerCase(Locale.ROOT);
        if (c.startsWith("ingenier")) return "Ingeniería";
        if (c.startsWith("arquitect")) return "Arquitectura";
        if (c.startsWith("psicolog")) return "Salud";
        return null;
    }
}