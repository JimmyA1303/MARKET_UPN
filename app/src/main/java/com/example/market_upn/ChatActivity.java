package com.example.market_upn;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText etMensaje;
    private RecyclerView rvMensajesChat;
    private MensajeAdapter mensajeAdapter;
    private List<Mensaje> listaMensajes;
    private TextView tvNombreContacto;
    private TextView tvProductoChat;

    private String receptorId;
    private String productoId;
    private String nombreVendedor;
    private String tituloProducto;
    private String miId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser usuarioActual = mAuth.getCurrentUser();

        if (usuarioActual == null) {
            finish();
            return;
        }
        miId = usuarioActual.getUid();

        receptorId = getIntent().getStringExtra("receptorId");
        productoId = getIntent().getStringExtra("productoId");
        nombreVendedor = getIntent().getStringExtra("nombreVendedor");
        tituloProducto = getIntent().getStringExtra("tituloProducto");

        tvNombreContacto = findViewById(R.id.tvNombreContacto);
        tvProductoChat = findViewById(R.id.tvProductoChat);
        etMensaje = findViewById(R.id.etMensaje);
        rvMensajesChat = findViewById(R.id.rvMensajesChat);
        
        listaMensajes = new ArrayList<>();
        mensajeAdapter = new MensajeAdapter(listaMensajes, miId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMensajesChat.setLayoutManager(layoutManager);
        rvMensajesChat.setAdapter(mensajeAdapter);

        if (nombreVendedor != null && !nombreVendedor.isEmpty() && !nombreVendedor.contains("@") && !nombreVendedor.equalsIgnoreCase("Cargando...")) {
            tvNombreContacto.setText(nombreVendedor);
        } else if (receptorId != null && !receptorId.isEmpty()) {
            cargarNombreContacto(receptorId);
        }

        if (tituloProducto != null && !tituloProducto.isEmpty()) {
            tvProductoChat.setText("Interés en: " + tituloProducto);
        }

        String mensajeInicial = getIntent().getStringExtra("mensajeInicial");
        if (mensajeInicial != null && !mensajeInicial.isEmpty()) {
            etMensaje.setText(mensajeInicial);
        }

        findViewById(R.id.btnVolverChat).setOnClickListener(v -> finish());
        findViewById(R.id.btnEnviarMensaje).setOnClickListener(v -> enviarMensaje());

        if (receptorId != null && productoId != null && !receptorId.isEmpty() && !productoId.isEmpty()) {
            cargarMensajesEnTiempoReal();
        }
    }

    private void enviarMensaje() {
        String texto = etMensaje.getText().toString().trim();
        if (texto.isEmpty()) return;

        if (receptorId == null || productoId == null || receptorId.isEmpty() || productoId.isEmpty()) {
            Toast.makeText(this, "Error: Datos de conversación inválidos", Toast.LENGTH_SHORT).show();
            return;
        }

        Mensaje mensaje = new Mensaje(miId, receptorId, texto, productoId);

        db.collection("mensajes")
                .add(mensaje)
                .addOnSuccessListener(documentReference -> {
                    etMensaje.setText("");
                    generarNotificacionDeMensaje(texto);
                })
                .addOnFailureListener(e -> {
                    String errorMsg = (e != null && e.getMessage() != null) ? e.getMessage() : "Error desconocido";
                    Toast.makeText(ChatActivity.this, "Error al enviar: " + errorMsg, Toast.LENGTH_LONG).show();
                });
    }

    private void generarNotificacionDeMensaje(String textoMensaje) {
        String nombreRemitente = "Alguien";
        Notificacion noti = new Notificacion(
                "Nuevo mensaje sobre tu anuncio",
                "Te escribieron: " + textoMensaje,
                "MENSAJE",
                productoId,
                false
        );

        db.collection("usuarios")
                .document(receptorId)
                .collection("notificaciones")
                .add(noti);
    }

    private void cargarMensajesEnTiempoReal() {
        db.collection("mensajes")
                .whereEqualTo("productoId", productoId)
                .orderBy("fechaEnvio", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(ChatActivity.this, "Error al cargar mensajes: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value == null) return;

                    listaMensajes.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Mensaje m = doc.toObject(Mensaje.class);
                        if (m != null && m.getRemitenteId() != null && m.getReceptorId() != null) {
                            boolean esMio = m.getRemitenteId().equals(miId) && m.getReceptorId().equals(receptorId);
                            boolean esDelOtro = m.getRemitenteId().equals(receptorId) && m.getReceptorId().equals(miId);

                            if (esMio || esDelOtro) {
                                listaMensajes.add(m);
                            }
                        }
                    }
                    mensajeAdapter.notifyDataSetChanged();
                    if (!listaMensajes.isEmpty()) {
                        rvMensajesChat.scrollToPosition(listaMensajes.size() - 1);
                    }
                });
    }

    private void cargarNombreContacto(String uid) {
        db.collection("usuarios").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && !isFinishing() && !isDestroyed()) {
                        String nombres = doc.getString("nombres");
                        String apellidos = doc.getString("apellidos");
                        if (nombres != null && !nombres.trim().isEmpty()) {
                            String primerNombre = nombres.trim().split("\\s+")[0];
                            String primerApellido = (apellidos != null && !apellidos.trim().isEmpty()) ? apellidos.trim().split("\\s+")[0] : "";
                            String completo = (primerNombre + " " + primerApellido).trim();
                            tvNombreContacto.setText(completo.isEmpty() ? "Contacto" : completo);
                        } else if (doc.getString("nombre") != null) {
                            tvNombreContacto.setText(doc.getString("nombre"));
                        }
                    }
                });
    }
}