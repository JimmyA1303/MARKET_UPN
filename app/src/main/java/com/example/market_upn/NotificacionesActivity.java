package com.example.market_upn;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class NotificacionesActivity extends AppCompatActivity {

    private RecyclerView rvNotificaciones;
    private LinearLayout layoutVacioNotificaciones;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private NotificacionAdapter adapter;
    private List<Notificacion> listaNotificaciones;

    private List<Notificacion> listaPersonales = new ArrayList<>();
    private List<Notificacion> listaGlobales = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notificaciones);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvNotificaciones = findViewById(R.id.rvNotificaciones);
        layoutVacioNotificaciones = findViewById(R.id.layoutVacioNotificaciones);

        listaNotificaciones = new ArrayList<>();
        adapter = new NotificacionAdapter(listaNotificaciones, this, this::onNotificacionClick);
        rvNotificaciones.setLayoutManager(new LinearLayoutManager(this));
        rvNotificaciones.setAdapter(adapter);

        findViewById(R.id.btnVolverNotificaciones).setOnClickListener(v -> finish());

        cargarNotificacionesPersonales();
        obtenerCarreraYCargarGlobales();
    }


    private void obtenerCarreraYCargarGlobales() {
        FirebaseUser usuarioActual = mAuth.getCurrentUser();
        if (usuarioActual == null) return;

        final String miId = usuarioActual.getUid();

        db.collection("usuarios").document(miId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isFinishing() || isDestroyed() || !documentSnapshot.exists()) return;

                    String carrera = documentSnapshot.getString("carrera");
                    if (carrera == null || carrera.isEmpty()) return;

                    String categoria = NotificacionesPush.categoriaPorCarrera(carrera);

                    if (categoria == null || categoria.isEmpty()) {
                        categoria = carrera;
                    }

                    cargarNotificacionesGlobales(categoria, miId);
                });
    }

    private void cargarNotificacionesGlobales(String categoria, String miId) {
        String topico = Topicos.deCategoria(categoria);

        db.collection("notificaciones_publicaciones")
                .document(topico)
                .collection("avisos")
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    listaGlobales.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {

                        if (miId.equals(doc.getString("autorId"))) continue;

                        Notificacion noti = doc.toObject(Notificacion.class);
                        if (noti != null) {
                            noti.setId(doc.getId());
                            listaGlobales.add(noti);
                        }
                    }

                    actualizarListaUnificada();
                });
    }


    private void cargarNotificacionesPersonales() {
        FirebaseUser usuarioActual = mAuth.getCurrentUser();
        if (usuarioActual == null) return;

        String miId = usuarioActual.getUid();

        db.collection("usuarios")
                .document(miId)
                .collection("notificaciones")
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error al cargar notificaciones.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        listaPersonales.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Notificacion noti = doc.toObject(Notificacion.class);
                            if (noti != null) {
                                noti.setId(doc.getId());
                                listaPersonales.add(noti);
                            }
                        }
                        actualizarListaUnificada();
                    }
                });
    }

    private void actualizarListaUnificada() {
        listaNotificaciones.clear();
        listaNotificaciones.addAll(listaPersonales);
        listaNotificaciones.addAll(listaGlobales);

        Collections.sort(listaNotificaciones, (n1, n2) -> {
            if (n1.getFechaCreacion() == null && n2.getFechaCreacion() == null) return 0;
            if (n1.getFechaCreacion() == null) return 1;
            if (n2.getFechaCreacion() == null) return -1;
            return n2.getFechaCreacion().compareTo(n1.getFechaCreacion());
        });

        adapter.notifyDataSetChanged();

        if (listaNotificaciones.isEmpty()) {
            rvNotificaciones.setVisibility(View.GONE);
            layoutVacioNotificaciones.setVisibility(View.VISIBLE);
        } else {
            rvNotificaciones.setVisibility(View.VISIBLE);
            layoutVacioNotificaciones.setVisibility(View.GONE);
        }
    }


    private void onNotificacionClick(Notificacion notificacion) {
        FirebaseUser usuarioActual = mAuth.getCurrentUser();
        if (usuarioActual == null) return;

        String miId = usuarioActual.getUid();
        String tipo = notificacion.getTipo();
        String productoId = notificacion.getReferenciaId();

        if (notificacion.getId() != null && !"PUBLICACION".equals(tipo)) {
            db.collection("usuarios")
                    .document(miId)
                    .collection("notificaciones")
                    .document(notificacion.getId())
                    .update("leida", true);
        }

        if ("MENSAJE".equals(tipo)) {
            abrirChatDesdeMensaje(productoId, miId);

        } else if ("INTERCAMBIO".equals(tipo)) {
            abrirChatDesdeIntercambio(productoId, miId);

        } else if ("PUBLICACION".equals(tipo)) {
            if (productoId != null && !productoId.isEmpty()) {
                Intent intent = new Intent(this, detalle_producto.class);
                intent.putExtra("productoId", productoId);
                startActivity(intent);
            } else {
                startActivity(new Intent(this, Principal.class));
            }
        }
    }

    private void abrirChatDesdeMensaje(String productoId, String miId) {
        if (productoId == null || productoId.isEmpty()) {
            startActivity(new Intent(this, ListaChatsActivity.class));
            return;
        }

        db.collection("mensajes")
                .whereEqualTo("productoId", productoId)
                .whereEqualTo("receptorId", miId)
                .get()
                .addOnSuccessListener(resultado -> {
                    String remitenteId = null;
                    Date masReciente = null;

                    for (DocumentSnapshot d : resultado.getDocuments()) {
                        Mensaje m = d.toObject(Mensaje.class);
                        if (m == null || m.getRemitenteId() == null) continue;

                        Date fecha = m.getFechaEnvio();

                        if (remitenteId == null
                                || (fecha != null && (masReciente == null
                                || fecha.after(masReciente)))) {
                            remitenteId = m.getRemitenteId();
                            masReciente = fecha;
                        }
                    }

                    if (remitenteId == null) {
                        startActivity(new Intent(this, ListaChatsActivity.class));
                        return;
                    }

                    abrirChat(remitenteId, productoId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "No se pudo abrir el chat.",
                            Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, ListaChatsActivity.class));
                });
    }

    private void abrirChatDesdeIntercambio(String productoId, String miId) {
        if (productoId == null || productoId.isEmpty()) {
            startActivity(new Intent(this, ListaChatsActivity.class));
            return;
        }

        db.collection("intercambios")
                .whereEqualTo("productoId", productoId)
                .whereEqualTo("vendedorId", miId)
                .get()
                .addOnSuccessListener(resultado -> {
                    String ofertanteId = null;
                    Date masReciente = null;

                    for (DocumentSnapshot d : resultado.getDocuments()) {
                        String id = d.getString("ofertanteId");
                        if (id == null) continue;

                        Date fecha = d.getDate("fechaCreacion");

                        if (ofertanteId == null
                                || (fecha != null && (masReciente == null
                                || fecha.after(masReciente)))) {
                            ofertanteId = id;
                            masReciente = fecha;
                        }
                    }

                    if (ofertanteId == null) {
                        startActivity(new Intent(this, ListaChatsActivity.class));
                        return;
                    }

                    abrirChat(ofertanteId, productoId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "No se pudo abrir el chat.",
                            Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, ListaChatsActivity.class));
                });
    }

    private void abrirChat(String otroId, String productoId) {
        db.collection("productos")
                .document(productoId)
                .get()
                .addOnCompleteListener(task -> {
                    String titulo = "";

                    if (task.isSuccessful() && task.getResult() != null) {
                        String t = task.getResult().getString("titulo");
                        if (t != null) titulo = t;
                    }

                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("receptorId", otroId);
                    intent.putExtra("productoId", productoId);
                    intent.putExtra("nombreVendedor", "");
                    intent.putExtra("tituloProducto", titulo);
                    startActivity(intent);
                });
    }
}