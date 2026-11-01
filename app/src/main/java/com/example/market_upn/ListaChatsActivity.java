package com.example.market_upn;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListaChatsActivity extends AppCompatActivity {

    private RecyclerView rvListaChats;
    private TextView tvEmptyChats;
    private ProgressBar pbListaChats;
    private ListaChatsAdapter adapter;
    private List<ChatResumen> listaChats;
    private FirebaseFirestore db;
    private String miId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lista_chats);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        rvListaChats = findViewById(R.id.rvListaChats);
        tvEmptyChats = findViewById(R.id.tvEmptyChats);
        pbListaChats = findViewById(R.id.pbListaChats);
        
        findViewById(R.id.btnVolverListaChats).setOnClickListener(v -> finish());

        rvListaChats.setLayoutManager(new LinearLayoutManager(this));
        listaChats = new ArrayList<>();
        adapter = new ListaChatsAdapter(this, listaChats);
        rvListaChats.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        miId = user.getUid();

        cargarChats();
    }

    private void cargarChats() {
        pbListaChats.setVisibility(View.VISIBLE);
        tvEmptyChats.setVisibility(View.GONE);

        db.collection("mensajes")
            .whereEqualTo("remitenteId", miId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<DocumentSnapshot> allDocs = new ArrayList<>(queryDocumentSnapshots.getDocuments());
                
                db.collection("mensajes")
                    .whereEqualTo("receptorId", miId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots2 -> {
                        allDocs.addAll(queryDocumentSnapshots2.getDocuments());
                        procesarMensajes(allDocs);
                    })
                    .addOnFailureListener(e -> pbListaChats.setVisibility(View.GONE));
            })
            .addOnFailureListener(e -> pbListaChats.setVisibility(View.GONE));
    }

    private void procesarMensajes(List<DocumentSnapshot> allDocs) {
        Map<String, ChatResumen> mapaChats = new HashMap<>();

        for (DocumentSnapshot doc : allDocs) {
            Mensaje m = doc.toObject(Mensaje.class);
            if (m == null || m.getRemitenteId() == null || m.getReceptorId() == null || m.getProductoId() == null) continue;

            String otroUsuarioId = m.getRemitenteId().equals(miId) ? m.getReceptorId() : m.getRemitenteId();
            String chatKey = otroUsuarioId + "_" + m.getProductoId();

            ChatResumen existente = mapaChats.get(chatKey);
            
            if (existente == null || (m.getFechaEnvio() != null && existente.getFechaUltimoMensaje() != null && m.getFechaEnvio().after(existente.getFechaUltimoMensaje()))) {
                ChatResumen nuevo = new ChatResumen(otroUsuarioId, m.getProductoId());
                nuevo.setUltimoMensaje(m.getTexto());
                nuevo.setFechaUltimoMensaje(m.getFechaEnvio());
                mapaChats.put(chatKey, nuevo);
            }
        }

        listaChats.clear();
        listaChats.addAll(mapaChats.values());

        Collections.sort(listaChats, (c1, c2) -> {
            if (c1.getFechaUltimoMensaje() == null) return 1;
            if (c2.getFechaUltimoMensaje() == null) return -1;
            return c2.getFechaUltimoMensaje().compareTo(c1.getFechaUltimoMensaje());
        });

        int[] count = {0};
        if (listaChats.isEmpty()) {
            actualizarUI();
        } else {
            for (ChatResumen chat : listaChats) {
                db.collection("usuarios").document(chat.getOtroUsuarioId()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String nombres = doc.getString("nombres");
                            String apellidos = doc.getString("apellidos");
                            if (nombres != null && !nombres.trim().isEmpty()) {
                                String primerNombre = nombres.trim().split("\\s+")[0];
                                String primerApellido = (apellidos != null && !apellidos.trim().isEmpty()) ? apellidos.trim().split("\\s+")[0] : "";
                                String completo = (primerNombre + " " + primerApellido).trim();
                                chat.setOtroUsuarioNombre(completo);
                            } else if (doc.getString("nombre") != null) {
                                chat.setOtroUsuarioNombre(doc.getString("nombre"));
                            }
                        }
                        
                        db.collection("productos").document(chat.getProductoId()).get()
                            .addOnSuccessListener(doc2 -> {
                                if (doc2.exists() && doc2.getString("titulo") != null) {
                                    chat.setTituloProducto(doc2.getString("titulo"));
                                }
                                
                                count[0]++;
                                if (count[0] == listaChats.size()) {
                                    actualizarUI();
                                }
                            })
                            .addOnFailureListener(e -> {
                                count[0]++;
                                if (count[0] == listaChats.size()) {
                                    actualizarUI();
                                }
                            });
                    })
                    .addOnFailureListener(e -> {
                        count[0]++;
                        if (count[0] == listaChats.size()) {
                            actualizarUI();
                        }
                    });
            }
        }
    }

    private void actualizarUI() {
        pbListaChats.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
        if (listaChats.isEmpty()) {
            tvEmptyChats.setVisibility(View.VISIBLE);
        } else {
            tvEmptyChats.setVisibility(View.GONE);
        }
    }
}
