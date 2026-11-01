package com.example.market_upn;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class buscar extends AppCompatActivity {

    private static final int AZUL = Color.parseColor("#102A43");
    private static final int GRIS = Color.parseColor("#64748B");
    private static final int AMARILLO = Color.parseColor("#FFB914");
    private static final int MOSTAZA = Color.parseColor("#D99B00");
    private static final int CREMA = Color.parseColor("#FFF2D4");
    private static final int BORDE = Color.parseColor("#E2E8F0");

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText etFiltroBusqueda;
    private TextView tvResultadosCount;
    private TextView tvSinResultadosBusqueda;
    private LinearLayout contenedorResultadosBusqueda;

    private TextView[] chipsCategoria;
    private TextView[] chipsEstado;

    private final List<Producto> productos = new ArrayList<>();
    private String categoriaActual = "Todos";
    private String estadoActual = "Cualquiera";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_buscar);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {
                    Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(sb.left, sb.top, sb.right, 0);
                    return insets;
                });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFiltroBusqueda = findViewById(R.id.etFiltroBusqueda);
        tvResultadosCount = findViewById(R.id.tvResultadosCount);
        tvSinResultadosBusqueda = findViewById(R.id.tvSinResultadosBusqueda);
        contenedorResultadosBusqueda = findViewById(R.id.contenedorResultadosBusqueda);

        configurarBuscador();
        configurarCategorias();
        configurarEstados();
        configurarNavegacionInferior();

        cargarProductos();
    }



    private void configurarBuscador() {
        etFiltroBusqueda.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                aplicarFiltros();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void configurarCategorias() {
        chipsCategoria = new TextView[]{
                findViewById(R.id.filtroCatTodos),
                findViewById(R.id.filtroCatIngenieria),
                findViewById(R.id.filtroCatCiencias),
                findViewById(R.id.filtroCatArquitectura),
                findViewById(R.id.filtroCatSalud)
        };

        for (TextView chip : chipsCategoria) {
            chip.setOnClickListener(v -> {
                categoriaActual = ((TextView) v).getText().toString();
                pintarCategorias();
                aplicarFiltros();
            });
        }
        pintarCategorias();
    }


    private void pintarCategorias() {
        for (TextView chip : chipsCategoria) {
            boolean activo = chip.getText().toString()
                    .equalsIgnoreCase(categoriaActual);

            chip.setBackground(
                    activo
                            ? forma(AMARILLO, 20, 0)
                            : forma(Color.WHITE, 20, BORDE)
            );


            chip.setTextColor(AZUL);

            chip.setTypeface(
                    null,
                    activo ? Typeface.BOLD : Typeface.NORMAL
            );
        }
    }


    private void configurarEstados() {
        chipsEstado = new TextView[]{
                findViewById(R.id.filtroEstCualquiera),
                findViewById(R.id.filtroEstComoNuevo),
                findViewById(R.id.filtroEstNuevo),
                findViewById(R.id.filtroEstBuenEstado)
        };

        for (TextView chip : chipsEstado) {
            chip.setOnClickListener(v -> {
                estadoActual = ((TextView) v).getText().toString();
                pintarEstados();
                aplicarFiltros();
            });
        }
        pintarEstados();
    }

    private void pintarEstados() {
        for (TextView chip : chipsEstado) {
            boolean activo = chip.getText().toString().equalsIgnoreCase(estadoActual);
            chip.setBackground(activo ? forma(AMARILLO, 17, 0) : forma(Color.WHITE, 17, BORDE));
            chip.setTextColor(activo ? AZUL : GRIS);
            chip.setTypeface(null, activo ? Typeface.BOLD : Typeface.NORMAL);
        }
    }



    private void cargarProductos() {
        db.collection("productos")
                .get()
                .addOnSuccessListener(resultado -> {
                    productos.clear();
                    for (DocumentSnapshot doc : resultado.getDocuments()) {

                        // Los productos vendidos no aparecen en la búsqueda
                        if (Boolean.TRUE.equals(doc.getBoolean("vendido"))) continue;

                        Producto p = doc.toObject(Producto.class);
                        if (p != null) {
                            p.setId(doc.getId());
                            productos.add(p);
                        }
                    }
                    Collections.sort(productos, (a, b) -> {
                        if (a.getFechaCreacion() == null && b.getFechaCreacion() == null) return 0;
                        if (a.getFechaCreacion() == null) return 1;
                        if (b.getFechaCreacion() == null) return -1;
                        return b.getFechaCreacion().compareTo(a.getFechaCreacion());
                    });
                    aplicarFiltros();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al cargar productos", Toast.LENGTH_SHORT).show());
    }

    private void aplicarFiltros() {
        String texto = etFiltroBusqueda.getText().toString().trim().toLowerCase(Locale.getDefault());
        List<Producto> filtrados = new ArrayList<>();

        for (Producto p : productos) {
            String cat = p.getCategoria() == null ? "" : p.getCategoria();
            String est = p.getEstado() == null ? "" : p.getEstado();
            String tit = p.getTitulo() == null ? "" : p.getTitulo();
            String desc = p.getDescripcion() == null ? "" : p.getDescripcion();

            boolean catOk = categoriaActual.equalsIgnoreCase("Todos") || cat.equalsIgnoreCase(categoriaActual);
            boolean estOk = estadoActual.equalsIgnoreCase("Cualquiera") || est.equalsIgnoreCase(estadoActual);
            boolean textoOk = texto.isEmpty() || tit.toLowerCase(Locale.getDefault()).contains(texto)
                    || desc.toLowerCase(Locale.getDefault()).contains(texto);

            if (catOk && estOk && textoOk) {
                filtrados.add(p);
            }
        }

        tvResultadosCount.setText("Resultados (" + filtrados.size() + ")");
        mostrarResultados(filtrados);
    }



    private void mostrarResultados(List<Producto> lista) {
        contenedorResultadosBusqueda.removeAllViews();
        tvSinResultadosBusqueda.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);

        for (Producto p : lista) {
            contenedorResultadosBusqueda.addView(crearTarjetaHorizontal(p));
        }
    }


    private View crearTarjetaHorizontal(Producto p) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(forma(Color.WHITE, 20, BORDE));
        card.setElevation(dp(2));
        card.setClipToOutline(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = dp(12);
        card.setLayoutParams(lp);


        FrameLayout contenedorImagen = new FrameLayout(this);
        contenedorImagen.setBackground(forma(CREMA, 15, 0));
        contenedorImagen.setClipToOutline(true);

        LinearLayout.LayoutParams lpImagen = new LinearLayout.LayoutParams(
                dp(100), dp(112)
        );
        contenedorImagen.setLayoutParams(lpImagen);

        ImageView img = new ImageView(this);
        img.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ponerImagen(img, p);
        contenedorImagen.addView(img);
        card.addView(contenedorImagen);


        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(13), dp(2), 0, dp(2));

        LinearLayout.LayoutParams lpInfo = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        info.setLayoutParams(lpInfo);


        TextView categoria = new TextView(this);
        categoria.setText(p.getCategoria() == null
                ? "MATERIAL UNIVERSITARIO"
                : p.getCategoria().toUpperCase(Locale.getDefault()));
        categoria.setTextColor(MOSTAZA);
        categoria.setTextSize(9);
        categoria.setLetterSpacing(0.04f);
        categoria.setTypeface(null, Typeface.BOLD);
        categoria.setMaxLines(1);
        categoria.setEllipsize(TextUtils.TruncateAt.END);

        info.addView(categoria);


        TextView titulo = new TextView(this);
        titulo.setText(p.getTitulo() == null ? "Producto" : p.getTitulo());
        titulo.setTextColor(AZUL);
        titulo.setTextSize(15);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setMaxLines(2);
        titulo.setEllipsize(TextUtils.TruncateAt.END);

        LinearLayout.LayoutParams lpTitulo = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lpTitulo.topMargin = dp(5);
        info.addView(titulo, lpTitulo);


        TextView vendedor = new TextView(this);
        vendedor.setText("Por: " +
                (p.getVendedor() == null || p.getVendedor().isEmpty()
                        ? "Estudiante"
                        : p.getVendedor()));
        vendedor.setTextColor(GRIS);
        vendedor.setTextSize(11);
        vendedor.setMaxLines(1);
        vendedor.setEllipsize(TextUtils.TruncateAt.END);

        LinearLayout.LayoutParams lpVendedor = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lpVendedor.topMargin = dp(4);
        info.addView(vendedor, lpVendedor);


        TextView precio = new TextView(this);
        precio.setText(String.format(
                Locale.getDefault(), "S/ %.2f", p.getPrecio()
        ));
        precio.setTextColor(AZUL);
        precio.setTextSize(18);
        precio.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams lpPrecio = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lpPrecio.topMargin = dp(7);
        precio.setPadding(dp(10), dp(5), dp(10), dp(5));
        precio.setBackground(forma(CREMA, 10, 0));

        info.addView(precio, lpPrecio);

        card.addView(info);


        card.setClickable(true);
        card.setFocusable(true);
        card.setForeground(
                getDrawable(android.R.drawable.list_selector_background)
        );
        card.setOnClickListener(v -> mostrarDetalle(p));

        return card;
    }


    private void ponerImagen(ImageView img, Producto p) {
        String base64 = p.getImagenBase64();
        if (base64 != null && !base64.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp != null) {
                    img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    img.setImageBitmap(bmp);
                    return;
                }
            } catch (Exception ignored) {}
        }
        img.setScaleType(ImageView.ScaleType.CENTER);
        img.setImageResource(android.R.drawable.ic_menu_gallery);
        img.setColorFilter(MOSTAZA);
    }

    private void mostrarDetalle(Producto p) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(p.getTitulo())
                .setMessage("Categoría: " + p.getCategoria() + "\nEstado: " + p.getEstado()
                        + "\nPrecio: S/ " + p.getPrecio() + "\n\n" + (p.getDescripcion() == null ? "" : p.getDescripcion()))
                .setPositiveButton("Cerrar", null)
                .show();
    }



    private void configurarNavegacionInferior() {
        findViewById(R.id.btnInicio).setOnClickListener(v -> {
            finish();
        });

        findViewById(R.id.btnPublicar).setOnClickListener(v -> {
            startActivity(new Intent(this, Publicar.class));
            finish();
        });

        findViewById(R.id.btnChats).setOnClickListener(v -> {
            Intent intent = new Intent(buscar.this, ListaChatsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnPerfil).setOnClickListener(v -> finish());
    }



    private GradientDrawable forma(int relleno, int radioDp, int borde) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(relleno);
        g.setCornerRadius(dp(radioDp));
        if (borde != 0) g.setStroke(dp(1), borde);
        return g;
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}