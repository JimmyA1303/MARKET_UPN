package com.example.market_upn;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;


public class MiFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) return;

        Map<String, Object> datos = new HashMap<>();
        datos.put("fcmToken", token);
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(usuario.getUid())
                .set(datos, SetOptions.merge());
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage mensaje) {
        Map<String, String> datos = mensaje.getData();
        if (datos.isEmpty()) return;

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) return;

        if (usuario.getUid().equals(datos.get("usuarioId"))) return;

        String titulo = datos.get("titulo") != null ? datos.get("titulo") : "UPN Market";
        String detalle = datos.get("detalle") != null ? datos.get("detalle") : "";

        Map<String, Object> registro = new HashMap<>();
        registro.put("titulo", titulo);
        registro.put("detalle", detalle);
        registro.put("categoria", datos.get("categoria"));
        registro.put("productoId", datos.get("productoId"));
        registro.put("leida", false);
        registro.put("fecha", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(usuario.getUid())
                .collection("notificaciones")
                .add(registro);

        mostrarNotificacion(titulo, detalle);
    }

    private void mostrarNotificacion(String titulo, String detalle) {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificacionesPush.crearCanal(this);

        Intent intent = new Intent(this, Principal.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, NotificacionesPush.CANAL_ID)
                        .setSmallIcon(R.drawable.ic_notifications)
                        .setColor(Color.parseColor("#FFB914"))
                        .setContentTitle(titulo)
                        .setContentText(detalle)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(detalle))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
    }
}