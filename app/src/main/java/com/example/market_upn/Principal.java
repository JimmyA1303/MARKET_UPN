
package com.example.market_upn;

import android.Manifest;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Principal extends AppCompatActivity {

    private static final int AZUL = Color.parseColor("#102A43");
    private static final int GRIS = Color.parseColor("#64748B");
    private static final int AMARILLO = Color.parseColor("#FFB914");
    private static final int MOSTAZA = Color.parseColor("#D99B00");
    private static final int CREMA = Color.parseColor("#FFF2D4");
    private static final int BORDE = Color.parseColor("#E8EDF3");

    private static final int ALTO_TARJETA_DP = 270;
    private static final int ALTO_PORTADA_DP = 145;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView tvSinResultados;
    private TextView tvContador;
    private LinearLayout contenedorProductos;

    private final List<Producto> productos = new ArrayList<>();
    private final List<String> intereses = new ArrayList<>();

    private ActivityResultLauncher<String> permisoNotificaciones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_principal);

        final View header = findViewById(R.id.headerPrincipal);
        final int headerPadTop = header.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {
                    Insets sb = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());

                    v.setPadding(sb.left, 0, sb.right, sb.bottom);

                    header.setPadding(
                            header.getPaddingLeft(),
                            headerPadTop + sb.top,
                            header.getPaddingRight(),
                            header.getPaddingBottom());

                    return insets;
                });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser usuario = mAuth.getCurrentUser();

        if (usuario == null) {
            irAlLogin();
            return;
        }

        tvSinResultados = findViewById(R.id.tvSinResultados);
        tvContador = findViewById(R.id.tvContador);
        contenedorProductos = findViewById(R.id.contenedorProductos);

        TextView tvSaludo = findViewById(R.id.tvSaludo);

        permisoNotificaciones = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                concedido -> {
                    if (!concedido) {
                        Toast.makeText(this,
                                "Puedes activar las notificaciones desde los ajustes",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        NotificacionesPush.crearCanal(this);
        pedirPermisoNotificaciones();
        cargarIntereses(usuario);
        cargarNombreUsuario(usuario, tvSaludo);
        escucharMensajesNuevos(usuario);

        findViewById(R.id.btnNotificaciones).setOnClickListener(v ->
                startActivity(new Intent(
                        Principal.this, NotificacionesActivity.class)));

        findViewById(R.id.btnInicio).setOnClickListener(v ->
                cargarProductos());

        findViewById(R.id.btnBuscar).setOnClickListener(v ->
                abrirBusqueda());

        findViewById(R.id.btnPublicar).setOnClickListener(v ->
                startActivity(new Intent(Principal.this, Publicar.class)));

        findViewById(R.id.btnChats).setOnClickListener(v -> {
            Intent intent = new Intent(
                    Principal.this, ListaChatsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnPerfil).setOnClickListener(v ->
                startActivity(new Intent(Principal.this, perfil.class)));
    }

    private void escucharMensajesNuevos(FirebaseUser usuario) {
        View puntoNuevosChats = findViewById(R.id.puntoNuevosChats);

        db.collection("usuarios")
                .document(usuario.getUid())
                .collection("notificaciones")
                .whereEqualTo("tipo", "MENSAJE")
                .whereEqualTo("leida", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null && !value.isEmpty()) {
                        puntoNuevosChats.setVisibility(View.VISIBLE);
                    } else {
                        puntoNuevosChats.setVisibility(View.GONE);
                    }
                });
    }

    private void abrirBusqueda() {
        startActivity(new Intent(Principal.this, buscar.class));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mAuth == null) return;

        FirebaseUser usuario = mAuth.getCurrentUser();

        if (usuario == null) {
            irAlLogin();
        } else {
            usuario.reload().addOnCompleteListener(task -> {
                if (isFinishing() || isDestroyed()) return;

                if (mAuth.getCurrentUser() != null) {
                    cargarProductos();
                } else {
                    irAlLogin();
                }
            });
        }
    }



    private void cargarNombreUsuario(
            FirebaseUser usuario, TextView tvSaludo) {

        db.collection("usuarios")
                .document(usuario.getUid())
                .get()
                .addOnSuccessListener(documento -> {
                    if (!documento.exists()) return;

                    String nombres = documento.getString("nombres");

                    if (nombres != null && !nombres.trim().isEmpty()) {
                        String primerNombre =
                                nombres.trim().split("\\s+")[0];

                        tvSaludo.setText("¡Hola, " + primerNombre + "!");
                    }
                });
    }


    private void cargarProductos() {
        db.collection("productos")
                .get()
                .addOnSuccessListener(resultado -> {
                    productos.clear();

                    for (DocumentSnapshot documento : resultado.getDocuments()) {

                        // Los productos vendidos no se muestran en el catálogo
                        if (Boolean.TRUE.equals(documento.getBoolean("vendido"))) continue;

                        Producto p = documento.toObject(Producto.class);

                        if (p != null) {
                            p.setId(documento.getId());
                            productos.add(p);
                        }
                    }

                    Collections.sort(productos, (a, b) -> {
                        if (a.getFechaCreacion() == null
                                && b.getFechaCreacion() == null) {
                            return 0;
                        }

                        if (a.getFechaCreacion() == null) return 1;
                        if (b.getFechaCreacion() == null) return -1;

                        return b.getFechaCreacion()
                                .compareTo(a.getFechaCreacion());
                    });

                    tvContador.setText(productos.size() == 1
                            ? "1 producto disponible"
                            : productos.size() + " productos disponibles");

                    mostrarProductos(productos);
                })
                .addOnFailureListener(error ->
                        Toast.makeText(this,
                                "Error al cargar productos",
                                Toast.LENGTH_LONG).show());
    }

    private String seguro(String texto) {
        return texto == null ? "" : texto;
    }


    private void mostrarProductos(List<Producto> lista) {
        contenedorProductos.removeAllViews();

        tvSinResultados.setVisibility(
                lista.isEmpty() ? View.VISIBLE : View.GONE);

        for (int i = 0; i < lista.size(); i += 2) {
            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setWeightSum(2f);

            fila.addView(crearTarjeta(lista.get(i)));

            if (i + 1 < lista.size()) {
                fila.addView(crearTarjeta(lista.get(i + 1)));
            } else {

                fila.addView(new View(this),
                        new LinearLayout.LayoutParams(0, 1, 1f));
            }

            contenedorProductos.addView(fila,
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private View crearTarjeta(Producto p) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(forma(Color.WHITE, 20, BORDE));
        card.setClipToOutline(true);
        card.setElevation(dp(3));

        LinearLayout.LayoutParams lpCard =
                new LinearLayout.LayoutParams(
                        0, dp(ALTO_TARJETA_DP), 1f);

        lpCard.setMargins(dp(6), dp(6), dp(6), dp(10));
        card.setLayoutParams(lpCard);


        FrameLayout portada = new FrameLayout(this);
        portada.setBackgroundColor(CREMA);

        card.addView(portada, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(ALTO_PORTADA_DP)));

        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);

        portada.addView(img, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        boolean conFoto = ponerImagen(img, p);

        TextView tvTitulo = new TextView(this);
        tvTitulo.setText(seguro(p.getTitulo()).trim());
        tvTitulo.setTextSize(14);
        tvTitulo.setTypeface(null, Typeface.BOLD);
        tvTitulo.setMaxLines(2);
        tvTitulo.setEllipsize(TextUtils.TruncateAt.END);
        tvTitulo.setGravity(Gravity.CENTER_VERTICAL);
        tvTitulo.setPadding(dp(12), dp(8), dp(12), dp(8));

        if (conFoto) {
            tvTitulo.setTextColor(Color.WHITE);

            GradientDrawable degradado = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{
                            0x000B1F33,
                            0xE60B1F33
                    });

            tvTitulo.setBackground(degradado);
        } else {
            tvTitulo.setTextColor(AZUL);
            tvTitulo.setBackgroundColor(CREMA);
        }

        FrameLayout.LayoutParams lpTitulo =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM);

        portada.addView(tvTitulo, lpTitulo);

        TextView tagCategoria = crearEtiquetaVidrio(
                seguro(p.getCategoria()));

        FrameLayout.LayoutParams lpCat =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP | Gravity.START);

        lpCat.setMargins(dp(8), dp(8), dp(4), 0);
        portada.addView(tagCategoria, lpCat);

        String estado = seguro(p.getEstado());

        if (!estado.trim().isEmpty()) {
            TextView tagEstado = crearEtiqueta(
                    textoEstado(estado), colorEstado(estado));

            FrameLayout.LayoutParams lpEst =
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            Gravity.TOP | Gravity.END);

            lpEst.setMargins(dp(4), dp(8), dp(8), 0);
            portada.addView(tagEstado, lpEst);
        }


        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setGravity(Gravity.TOP);
        info.setPadding(dp(11), dp(9), dp(11), dp(10));


        card.addView(info, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f));

        String vendedor = seguro(p.getVendedor()).trim();

        if (vendedor.contains("@")) {
            vendedor = vendedor.substring(0, vendedor.indexOf('@'))
                    .toUpperCase(Locale.getDefault());
        }

        if (vendedor.isEmpty()) {
            vendedor = "Vendedor";
        }

        info.addView(filaConIcono(
                R.drawable.ic_person, vendedor, 12));

        String sede = seguro(p.getSede()).trim();

        if (!sede.isEmpty()) {
            View filaSede = filaConIcono(
                    R.drawable.ic_location, sede, 11);

            LinearLayout.LayoutParams lpSede =
                    (LinearLayout.LayoutParams)
                            filaSede.getLayoutParams();

            lpSede.topMargin = dp(4);
            filaSede.setLayoutParams(lpSede);

            info.addView(filaSede);
        }

        String descripcion = seguro(p.getDescripcion()).trim();

        if (!descripcion.isEmpty()) {
            TextView tvDesc = new TextView(this);
            tvDesc.setText(descripcion);
            tvDesc.setTextColor(GRIS);
            tvDesc.setTextSize(11);
            tvDesc.setMaxLines(2);
            tvDesc.setEllipsize(TextUtils.TruncateAt.END);

            LinearLayout.LayoutParams lpDesc =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);

            lpDesc.topMargin = dp(5);
            info.addView(tvDesc, lpDesc);
        }


        View espacioFlexible = new View(this);

        info.addView(espacioFlexible,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0, 1f));


        LinearLayout filaPrecio = new LinearLayout(this);
        filaPrecio.setOrientation(LinearLayout.HORIZONTAL);
        filaPrecio.setGravity(Gravity.CENTER_VERTICAL);

        info.addView(filaPrecio,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(38)));

        TextView tvPrecio = new TextView(this);
        tvPrecio.setText(String.format(
                Locale.US, "S/ %.2f", p.getPrecio()));
        tvPrecio.setTextColor(AZUL);
        tvPrecio.setTextSize(14);
        tvPrecio.setTypeface(null, Typeface.BOLD);
        tvPrecio.setGravity(Gravity.CENTER);
        tvPrecio.setSingleLine(true);
        tvPrecio.setPadding(dp(9), dp(5), dp(9), dp(5));
        tvPrecio.setBackground(forma(CREMA, 12, 0));

        filaPrecio.addView(tvPrecio,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dp(32)));

        filaPrecio.addView(new View(this),
                new LinearLayout.LayoutParams(0, 1, 1f));

        FrameLayout btnContactar = new FrameLayout(this);
        btnContactar.setBackgroundResource(R.drawable.bg_icon_circle);
        btnContactar.setBackgroundTintList(
                ColorStateList.valueOf(AMARILLO));

        ImageView icono = new ImageView(this);
        icono.setImageResource(R.drawable.ic_nav_chat);
        icono.setColorFilter(AZUL);

        btnContactar.addView(icono,
                new FrameLayout.LayoutParams(
                        dp(18), dp(18), Gravity.CENTER));

        filaPrecio.addView(btnContactar,
                new LinearLayout.LayoutParams(dp(36), dp(36)));

        btnContactar.setOnClickListener(v -> {
            FirebaseUser usuarioActual = mAuth.getCurrentUser();

            if (usuarioActual == null) {
                irAlLogin();
                return;
            }

            String vendedorId = p.getUsuarioId();

            if (vendedorId == null || vendedorId.trim().isEmpty()) {
                Toast.makeText(this,
                        "No se encontró la cuenta del vendedor",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (usuarioActual.getUid().equals(vendedorId)) {
                Toast.makeText(this,
                        "No puedes chatear contigo mismo",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(
                    Principal.this, ChatActivity.class);

            intent.putExtra("receptorId", vendedorId);
            intent.putExtra("productoId", p.getId());
            intent.putExtra("nombreVendedor", p.getVendedor());
            intent.putExtra("tituloProducto", p.getTitulo());

            startActivity(intent);
        });

        card.setOnClickListener(v -> mostrarDetalle(p));

        return card;
    }



    private TextView crearEtiqueta(String texto, int colorFondo) {
        TextView tag = new TextView(this);
        tag.setText(texto);
        tag.setTextColor(Color.WHITE);
        tag.setTextSize(10);
        tag.setTypeface(null, Typeface.BOLD);
        tag.setSingleLine(true);
        tag.setEllipsize(TextUtils.TruncateAt.END);
        tag.setPadding(dp(9), dp(4), dp(9), dp(4));
        tag.setBackgroundResource(R.drawable.bg_chip);
        tag.setBackgroundTintList(
                ColorStateList.valueOf(colorFondo));

        return tag;
    }

    private TextView crearEtiquetaVidrio(String texto) {
        TextView tag = new TextView(this);

        String categoria = texto == null || texto.trim().isEmpty()
                ? "Sin categoría"
                : texto.trim();

        tag.setText(categoria);
        tag.setTextColor(Color.WHITE);
        tag.setTextSize(10);
        tag.setTypeface(null, Typeface.BOLD);
        tag.setGravity(Gravity.CENTER);
        tag.setSingleLine(true);
        tag.setEllipsize(TextUtils.TruncateAt.END);
        tag.setMaxWidth(dp(125));
        tag.setPadding(dp(9), dp(4), dp(9), dp(4));

        tag.setBackgroundResource(R.drawable.bg_chip_glass);
        tag.setBackgroundTintList(
                ColorStateList.valueOf(AZUL));

        return tag;
    }

    private View filaConIcono(
            int iconoRes, String texto, int tamanoSp) {

        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(Gravity.CENTER_VERTICAL);

        fila.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(17)));

        ImageView icono = new ImageView(this);
        icono.setImageResource(iconoRes);
        icono.setColorFilter(MOSTAZA);

        fila.addView(icono,
                new LinearLayout.LayoutParams(dp(14), dp(14)));

        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextColor(GRIS);
        tv.setTextSize(tamanoSp);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);

        lp.setMarginStart(dp(5));
        fila.addView(tv, lp);

        return fila;
    }

    private String textoEstado(String estado) {
        return estado.equalsIgnoreCase("Estado regular")
                ? "Regular" : estado;
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



    private boolean ponerImagen(ImageView img, Producto p) {
        String base64 = p.getImagenBase64();

        if (base64 != null && !base64.trim().isEmpty()) {
            try {
                byte[] bytes = Base64.decode(
                        base64, Base64.DEFAULT);

                Bitmap bmp = BitmapFactory.decodeByteArray(
                        bytes, 0, bytes.length);

                if (bmp != null) {
                    img.setScaleType(
                            ImageView.ScaleType.CENTER_CROP);
                    img.setImageBitmap(bmp);
                    img.clearColorFilter();
                    img.setAlpha(1f);
                    return true;
                }
            } catch (IllegalArgumentException ignorado) {
            }
        }

        img.setScaleType(ImageView.ScaleType.CENTER);
        img.setImageResource(android.R.drawable.ic_menu_gallery);
        img.setColorFilter(MOSTAZA);
        img.setAlpha(0.65f);

        return false;
    }



    private void mostrarDetalle(Producto p) {
        Intent intent = new Intent(
                Principal.this, detalle_producto.class);
        intent.putExtra("productoId", p.getId());
        startActivity(intent);
    }


    private GradientDrawable forma(
            int relleno, int radioDp, int borde) {

        GradientDrawable g = new GradientDrawable();
        g.setColor(relleno);
        g.setCornerRadius(dp(radioDp));

        if (borde != 0) {
            g.setStroke(dp(1), borde);
        }

        return g;
    }

    private int dp(int valor) {
        return Math.round(
                valor * getResources().getDisplayMetrics().density);
    }


    private void pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

            permisoNotificaciones.launch(
                    Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void cargarIntereses(FirebaseUser usuario) {
        db.collection("usuarios")
                .document(usuario.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    intereses.clear();

                    Object guardados = doc.get("intereses");

                    if (guardados instanceof List<?>) {
                        for (Object elemento : (List<?>) guardados) {
                            if (elemento instanceof String) {
                                intereses.add((String) elemento);
                            }
                        }

                        NotificacionesPush.sincronizar(intereses);

                    } else {
                        String sugerida =
                                NotificacionesPush.categoriaPorCarrera(
                                        doc.getString("carrera"));

                        if (sugerida != null) {
                            intereses.add(sugerida);
                        }

                        guardarIntereses(false);
                    }
                });
    }

    private void guardarIntereses(boolean avisar) {
        FirebaseUser usuario = mAuth.getCurrentUser();

        if (usuario == null) return;

        Map<String, Object> datos = new HashMap<>();
        datos.put("intereses", new ArrayList<>(intereses));

        db.collection("usuarios")
                .document(usuario.getUid())
                .set(datos, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    NotificacionesPush.sincronizar(intereses);

                    if (avisar) {
                        Toast.makeText(this,
                                "Preferencias guardadas",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(error ->
                        Toast.makeText(this,
                                "No se pudieron guardar los intereses",
                                Toast.LENGTH_SHORT).show());
    }

    private void mostrarIntereses() {
        final String[] categorias = NotificacionesPush.CATEGORIAS;
        final boolean[] marcadas = new boolean[categorias.length];

        for (int i = 0; i < categorias.length; i++) {
            marcadas[i] = intereses.contains(categorias[i]);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Avisarme de anuncios nuevos en…")
                .setMultiChoiceItems(categorias, marcadas,
                        (dialog, which, marcado) ->
                                marcadas[which] = marcado)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    intereses.clear();

                    for (int i = 0; i < categorias.length; i++) {
                        if (marcadas[i]) {
                            intereses.add(categorias[i]);
                        }
                    }

                    guardarIntereses(true);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }



    private void mostrarPerfil() {
        FirebaseUser usuario = mAuth.getCurrentUser();

        String correo = usuario != null && usuario.getEmail() != null
                ? usuario.getEmail() : "";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Tu cuenta")
                .setMessage(correo)
                .setPositiveButton("Mis intereses",
                        (dialog, which) -> mostrarIntereses())
                .setNeutralButton("Cerrar sesión", (dialog, which) -> {
                    NotificacionesPush.cancelarTodas();
                    mAuth.signOut();
                    irAlLogin();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }


    private void irAlLogin() {
        Intent intent = new Intent(this, MainActivity.class);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }
}
