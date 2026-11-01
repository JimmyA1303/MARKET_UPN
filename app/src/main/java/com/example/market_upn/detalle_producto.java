package com.example.market_upn;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class detalle_producto extends AppCompatActivity {

    private static final int MOSTAZA = Color.parseColor("#D99B00");
    // Al volver de la coordinación, si la compra se hizo, se cierra este detalle
    private final ActivityResultLauncher<Intent> lanzadorCoordinacion =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) finish();
                    });

    private ImageView ivImagen;
    private TextView tvTitulo, tvPrecio, tvPrecioBarra, tvPrecioOriginal,
            tvDescuento, tvStats, tvChipEstado, tvChipCategoria,
            tvChipCiclo, tvDescripcion, tvVendedor, tvVendedorInicial,
            tvVendedorCarrera, tvVendedorCodigo, tvEntregaTitulo,
            tvAnalisisTexto, tvFlechaAnalisis;
    private View cardAnalisis, cardIntercambio, btnContactar;
    private MaterialButton btnComprar;

    private EditText etIntercambio, etIntercambioDetalle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_producto);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ivImagen = findViewById(R.id.ivDetalleImagen);
        tvTitulo = findViewById(R.id.tvDetalleTitulo);
        tvPrecio = findViewById(R.id.tvDetallePrecio);
        tvPrecioBarra = findViewById(R.id.tvDetallePrecioBarra);
        tvPrecioOriginal = findViewById(R.id.tvDetallePrecioOriginal);
        tvDescuento = findViewById(R.id.tvDetalleDescuento);
        tvStats = findViewById(R.id.tvDetalleStats);
        tvChipEstado = findViewById(R.id.tvChipEstado);
        tvChipCategoria = findViewById(R.id.tvChipCategoria);
        tvChipCiclo = findViewById(R.id.tvChipCiclo);
        tvDescripcion = findViewById(R.id.tvDetalleDescripcion);
        tvVendedor = findViewById(R.id.tvDetalleVendedor);
        tvVendedorInicial = findViewById(R.id.tvVendedorInicial);
        tvVendedorCarrera = findViewById(R.id.tvVendedorCarrera);
        tvVendedorCodigo = findViewById(R.id.tvVendedorCodigo);
        tvEntregaTitulo = findViewById(R.id.tvEntregaTitulo);
        tvAnalisisTexto = findViewById(R.id.tvAnalisisTexto);
        tvFlechaAnalisis = findViewById(R.id.tvFlechaAnalisis);
        cardAnalisis = findViewById(R.id.cardAnalisis);
        cardIntercambio = findViewById(R.id.cardIntercambio);
        etIntercambio = findViewById(R.id.etIntercambio);
        etIntercambioDetalle = findViewById(R.id.etIntercambioDetalle);
        btnContactar = findViewById(R.id.btnDetalleContactar);
        btnComprar = findViewById(R.id.btnDetalleComprar);

        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());

        findViewById(R.id.headerAnalisis).setOnClickListener(v -> {
            boolean abierto = tvAnalisisTexto.getVisibility() == View.VISIBLE;
            tvAnalisisTexto.setVisibility(abierto ? View.GONE : View.VISIBLE);
            tvFlechaAnalisis.setText(abierto ? "▼" : "▲");
        });

        String productoId = getIntent().getStringExtra("productoId");

        if (productoId == null || productoId.isEmpty()) {
            Toast.makeText(this, "Producto no encontrado",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        cargarProducto(productoId);
    }

    // ====================== CARGA DEL PRODUCTO ======================

    private void cargarProducto(String productoId) {
        FirebaseFirestore.getInstance()
                .collection("productos")
                .document(productoId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this,
                                "Este producto ya no está disponible",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    Producto p = doc.toObject(Producto.class);

                    if (p == null) {
                        finish();
                        return;
                    }

                    p.setId(doc.getId());
                    mostrarDatos(p, doc);
                    cargarVendedor(p);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Error al cargar el producto",
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void mostrarDatos(Producto p, DocumentSnapshot doc) {
        tvTitulo.setText(seguro(p.getTitulo()));
        ponerImagen(p);

        String estado = seguro(p.getEstado()).trim();
        if (estado.isEmpty()) {
            tvChipEstado.setVisibility(View.GONE);
        } else {
            tvChipEstado.setText(textoEstado(estado));
            tvChipEstado.setBackgroundTintList(
                    ColorStateList.valueOf(colorEstado(estado)));
        }

        String categoria = seguro(p.getCategoria()).trim();
        tvChipCategoria.setText(categoria.isEmpty() ? "Sin categoría" : categoria);

        String ciclo = doc.getString("ciclo");
        if (ciclo != null && !ciclo.trim().isEmpty()) {
            tvChipCiclo.setText(ciclo.trim());
            tvChipCiclo.setVisibility(View.VISIBLE);
        }

        double precio = p.getPrecio();
        String textoPrecio = String.format(Locale.US, "S/ %.2f", precio);
        tvPrecio.setText(textoPrecio);
        tvPrecioBarra.setText(textoPrecio);

        Double original = doc.getDouble("precioOriginal");
        if (original != null && original > precio && precio >= 0) {
            long pct = Math.round((original - precio) / original * 100);

            tvPrecioOriginal.setText(String.format(Locale.US, "S/ %.2f", original));
            tvPrecioOriginal.setPaintFlags(
                    tvPrecioOriginal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvPrecioOriginal.setVisibility(View.VISIBLE);

            tvDescuento.setText("-" + pct + "% vs precio nuevo");
            tvDescuento.setVisibility(View.VISIBLE);

            tvAnalisisTexto.setText(String.format(Locale.US,
                    "Este producto cuesta %d%% menos que su precio nuevo "
                            + "(S/ %.2f). Ahorras S/ %.2f comprándolo a un "
                            + "estudiante de la comunidad UPN.",
                    pct, original, original - precio));
            cardAnalisis.setVisibility(View.VISIBLE);
        }

        Long favoritos = doc.getLong("favoritos");
        Long vistas = doc.getLong("vistas");
        if (favoritos != null || vistas != null) {
            StringBuilder sb = new StringBuilder();
            if (favoritos != null) sb.append("♡ ").append(favoritos);
            if (favoritos != null && vistas != null) sb.append("   ");
            if (vistas != null) sb.append("👁 ").append(vistas);
            tvStats.setText(sb.toString());
            tvStats.setVisibility(View.VISIBLE);
        }

        String sede = seguro(p.getSede()).trim();
        tvEntregaTitulo.setText(sede.isEmpty()
                ? "📍  Entrega en campus"
                : "📍  Entrega en campus · " + sede);

        String descripcion = seguro(p.getDescripcion()).trim();
        tvDescripcion.setText(descripcion.isEmpty()
                ? "El vendedor no agregó una descripción."
                : descripcion);

        String codigo = codigoDesdeCorreo(p.getVendedor());
        tvVendedor.setText(codigo.isEmpty() ? "Vendedor" : codigo);
        tvVendedorInicial.setText(codigo.isEmpty() ? "V" : codigo.substring(0, 1));
        tvVendedorCodigo.setText(codigo.isEmpty() ? "" : "Código  " + codigo);

        Boolean aceptaTrueque = doc.getBoolean("aceptaIntercambio");
        FirebaseUser actual = FirebaseAuth.getInstance().getCurrentUser();
        boolean esMio = actual != null && p.getUsuarioId() != null
                && actual.getUid().equals(p.getUsuarioId());
        boolean vendido = Boolean.TRUE.equals(doc.getBoolean("vendido"));

        if (!esMio && !vendido && !Boolean.FALSE.equals(aceptaTrueque)) {
            cardIntercambio.setVisibility(View.VISIBLE);

            findViewById(R.id.btnEnviarPropuesta).setOnClickListener(v ->
                    enviarPropuesta(p));
        }

        btnContactar.setOnClickListener(v -> abrirChat(p, null));

        // ----- Botón "Comprar ahora" -----
        if (esMio) {
            // No tiene sentido comprar tu propio producto
            btnComprar.setVisibility(View.GONE);
        } else if (vendido) {
            btnComprar.setEnabled(false);
            btnComprar.setText("Producto vendido");
        } else {
            btnComprar.setOnClickListener(v -> abrirCoordinacion(p));
        }
    }

    private void abrirCoordinacion(Producto p) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            Toast.makeText(this, "Inicia sesión para comprar",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (usuario.getUid().equals(p.getUsuarioId())) {
            Toast.makeText(this, "No puedes comprar tu propio producto",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, coordinacion_entrega.class);
        intent.putExtra("productoId", p.getId());
        lanzadorCoordinacion.launch(intent);
    }
    private void registrarCompra(Producto p, String compradorId) {
        btnComprar.setEnabled(false);
        btnComprar.setText("Procesando...");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ref = db.collection("productos").document(p.getId());

        // Transacción: evita que dos personas compren el mismo producto a la vez.
        // Devuelve true si la compra se registró y false si ya estaba vendido.
        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(ref);

            if (Boolean.TRUE.equals(snap.getBoolean("vendido"))) {
                return false;
            }

            Map<String, Object> datos = new HashMap<>();
            datos.put("vendido", true);
            datos.put("compradorId", compradorId);
            datos.put("fechaVenta", FieldValue.serverTimestamp());
            transaction.update(ref, datos);
            return true;
        }).addOnSuccessListener(registrada -> {
            if (isFinishing() || isDestroyed()) return;

            if (!registrada) {
                btnComprar.setText("Producto vendido");
                cardIntercambio.setVisibility(View.GONE);
                Toast.makeText(this, "Este producto ya no está disponible",
                        Toast.LENGTH_LONG).show();
                return;
            }

            btnComprar.setText("Comprado ✓");
            cardIntercambio.setVisibility(View.GONE);

            Toast.makeText(this,
                    "¡Compra registrada! Coordina el encuentro por chat",
                    Toast.LENGTH_LONG).show();

            abrirChat(p, "Hola, acabo de comprar \"" + seguro(p.getTitulo())
                    + "\". ¿Coordinamos el encuentro en el campus?");
        }).addOnFailureListener(e -> {
            if (isFinishing() || isDestroyed()) return;

            btnComprar.setEnabled(true);
            btnComprar.setText("Comprar ahora");
            Toast.makeText(this,
                    "No se pudo registrar la compra. Intenta de nuevo.",
                    Toast.LENGTH_LONG).show();
        });
    }

    // ============================ VENDEDOR ============================

    private void cargarVendedor(Producto p) {
        String uid = p.getUsuarioId();
        if (uid == null || uid.trim().isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .get()
                .addOnSuccessListener(u -> {
                    if (!u.exists() || isFinishing() || isDestroyed()) return;

                    String nombres = seguro(u.getString("nombres")).trim();
                    String apellidos = seguro(u.getString("apellidos")).trim();
                    String carrera = seguro(u.getString("carrera")).trim();

                    String primerNombre = nombres.isEmpty() ? ""
                            : nombres.split("\\s+")[0];
                    String primerApellido = apellidos.isEmpty() ? ""
                            : apellidos.split("\\s+")[0];
                    String completo = (primerNombre + " " + primerApellido).trim();

                    if (!completo.isEmpty()) {
                        tvVendedor.setText(completo);
                        tvVendedorInicial.setText(
                                completo.substring(0, 1).toUpperCase(Locale.getDefault()));
                    }

                    if (!carrera.isEmpty()) {
                        tvVendedorCarrera.setText(carrera);
                        tvVendedorCarrera.setVisibility(View.VISIBLE);
                    }

                    String codigo = seguro(u.getString("codigo")).trim();
                    if (codigo.isEmpty()) {
                        codigo = seguro(u.getString("codigoEstudiante")).trim();
                    }
                    if (!codigo.isEmpty()) {
                        tvVendedorCodigo.setText("Código  " + codigo);
                    }
                });
    }

    // ========================== INTERCAMBIO ==========================

    private void enviarPropuesta(Producto p) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            Toast.makeText(this, "Inicia sesión para proponer un intercambio",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String vendedorId = p.getUsuarioId();

        if (vendedorId == null || vendedorId.trim().isEmpty()) {
            Toast.makeText(this, "No se encontró la cuenta del vendedor",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String oferta = etIntercambio.getText().toString().trim();
        String detalle = etIntercambioDetalle.getText().toString().trim();

        if (oferta.isEmpty()) {
            etIntercambio.setError("Indica qué material ofreces a cambio");
            etIntercambio.requestFocus();
            return;
        }

        if (detalle.length() < 10) {
            etIntercambioDetalle.setError(
                    "Describe el estado y la cantidad (mínimo 10 caracteres)");
            etIntercambioDetalle.requestFocus();
            return;
        }

        final MaterialButton btn = findViewById(R.id.btnEnviarPropuesta);
        btn.setEnabled(false);
        btn.setText("Enviando...");

        String correo = usuario.getEmail() != null ? usuario.getEmail() : "";
        String nombreOfertante = correo.contains("@")
                ? correo.substring(0, correo.indexOf('@')).toUpperCase(Locale.getDefault())
                : "Un estudiante";

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> propuesta = new HashMap<>();
        propuesta.put("productoId", p.getId());
        propuesta.put("tituloProducto", p.getTitulo());
        propuesta.put("vendedorId", vendedorId);
        propuesta.put("ofertanteId", usuario.getUid());
        propuesta.put("ofertanteCorreo", correo);
        propuesta.put("oferta", oferta);
        propuesta.put("detalle", detalle);
        propuesta.put("estado", "PENDIENTE");
        propuesta.put("fechaCreacion", FieldValue.serverTimestamp());

        db.collection("intercambios")
                .add(propuesta)
                .addOnSuccessListener(ref -> {
                    Notificacion aviso = new Notificacion(
                            "Nueva propuesta de intercambio",
                            nombreOfertante + " te ofrece \"" + oferta
                                    + "\" por \"" + seguro(p.getTitulo()) + "\"",
                            "INTERCAMBIO",
                            p.getId(),
                            false);

                    db.collection("usuarios")
                            .document(vendedorId)
                            .collection("notificaciones")
                            .add(aviso);

                    etIntercambio.setText("");
                    etIntercambioDetalle.setText("");
                    btn.setText("Propuesta enviada ✓");

                    Toast.makeText(this,
                            "Propuesta enviada. Coordina la entrega por chat.",
                            Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    btn.setEnabled(true);
                    btn.setText("Enviar propuesta");
                    Toast.makeText(this,
                            "No se pudo enviar la propuesta. Intenta de nuevo.",
                            Toast.LENGTH_LONG).show();
                });
    }

    // ============================== CHAT ==============================

    private void abrirChat(Producto p, String mensajeInicial) {
        FirebaseUser usuarioActual = FirebaseAuth.getInstance().getCurrentUser();

        if (usuarioActual == null) {
            Intent login = new Intent(this, MainActivity.class);
            login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(login);
            finish();
            return;
        }

        String vendedorId = p.getUsuarioId();

        if (vendedorId == null || vendedorId.trim().isEmpty()) {
            Toast.makeText(this, "No se encontró la cuenta del vendedor",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (usuarioActual.getUid().equals(vendedorId)) {
            Toast.makeText(this, "No puedes chatear contigo mismo",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("receptorId", vendedorId);
        intent.putExtra("productoId", p.getId());
        intent.putExtra("nombreVendedor", p.getVendedor());
        intent.putExtra("tituloProducto", p.getTitulo());

        if (mensajeInicial != null) {
            intent.putExtra("mensajeInicial", mensajeInicial);
        }

        startActivity(intent);
    }

    // ============================ UTILIDADES ============================

    private void ponerImagen(Producto p) {
        String base64 = p.getImagenBase64();

        if (base64 != null && !base64.trim().isEmpty()) {
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                if (bmp != null) {
                    ivImagen.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivImagen.setImageBitmap(bmp);
                    return;
                }
            } catch (IllegalArgumentException ignorado) {
            }
        }

        ivImagen.setScaleType(ImageView.ScaleType.CENTER);
        ivImagen.setImageResource(android.R.drawable.ic_menu_gallery);
        ivImagen.setColorFilter(MOSTAZA);
        ivImagen.setAlpha(0.65f);
    }

    private String seguro(String t) {
        return t == null ? "" : t;
    }

    private String codigoDesdeCorreo(String vendedor) {
        String v = seguro(vendedor).trim();
        if (v.contains("@")) v = v.substring(0, v.indexOf('@'));
        return v.toUpperCase(Locale.getDefault());
    }

    private String textoEstado(String estado) {
        return estado.equalsIgnoreCase("Estado regular") ? "Regular" : estado;
    }

    private int colorEstado(String estado) {
        switch (estado.toLowerCase(Locale.ROOT)) {
            case "nuevo":
                return Color.parseColor("#16A34A");
            case "como nuevo":
                return Color.parseColor("#0D9488");
            case "buen estado":
                return Color.parseColor("#D97706");
            default:
                return Color.parseColor("#64748B");
        }
    }
}