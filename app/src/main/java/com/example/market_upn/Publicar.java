package com.example.market_upn;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Publicar extends AppCompatActivity {

    private static final int AZUL = Color.parseColor("#102A43");
    private static final int GRIS = Color.parseColor("#64748B");
    private static final int MOSTAZA = Color.parseColor("#D99B00");
    private static final int CREMA = Color.parseColor("#FFF8E6");
    private static final int BORDE = Color.parseColor("#E2E8F0");

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextInputLayout tilTitulo, tilDescripcion, tilPrecio;
    private EditText etTitulo, etDescripcion, etPrecio;

    private FrameLayout zonaFoto;
    private ImageView ivPreview;
    private LinearLayout llSubirContenido;
    private TextView tvSubirFoto, tvCambiar;

    private TextView[] opcionesCategoria;
    private TextView[] opcionesEstado;
    private String categoriaSel = null;
    private String estadoSel = null;

    private MaterialButton btnPublicarAnuncio;

    private Uri imagenSeleccionada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_publicar);
        final View header = findViewById(R.id.headerPublicar);
        final int headerPadTop = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, 0, sb.right, sb.bottom);
            header.setPadding(header.getPaddingLeft(), headerPadTop + sb.top,
                    header.getPaddingRight(), header.getPaddingBottom());
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser usuario = mAuth.getCurrentUser();
        if (usuario == null) {
            irAlLogin();
            return;
        }

        tilTitulo = findViewById(R.id.tilTitulo);
        tilDescripcion = findViewById(R.id.tilDescripcion);
        tilPrecio = findViewById(R.id.tilPrecio);
        etTitulo = findViewById(R.id.etTitulo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etPrecio = findViewById(R.id.etPrecio);

        zonaFoto = findViewById(R.id.zonaFoto);
        ivPreview = findViewById(R.id.ivPreview);
        llSubirContenido = findViewById(R.id.llSubirContenido);
        tvSubirFoto = findViewById(R.id.tvSubirFoto);
        tvCambiar = findViewById(R.id.tvCambiar);

        btnPublicarAnuncio = findViewById(R.id.btnPublicarAnuncio);

        zonaFoto.setBackground(zonaPunteada());
        zonaFoto.setClipToOutline(true);
        tvCambiar.setBackground(forma(AZUL, 16, 0));
        zonaFoto.setOnClickListener(v -> seleccionarFoto());

        opcionesCategoria = new TextView[]{
                findViewById(R.id.optCatIngenieria),
                findViewById(R.id.optCatCiencias),
                findViewById(R.id.optCatArquitectura),
                findViewById(R.id.optCatSalud)
        };
        opcionesEstado = new TextView[]{
                findViewById(R.id.optEstNuevo),
                findViewById(R.id.optEstComoNuevo),
                findViewById(R.id.optEstBuenEstado)

        };
        configurarOpciones(opcionesCategoria, false);
        configurarOpciones(opcionesEstado, true);

        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
        btnPublicarAnuncio.setOnClickListener(v -> publicarAnuncio());
    }

    private void configurarOpciones(TextView[] opciones, boolean esEstado) {
        for (TextView opcion : opciones) {
            opcion.setOnClickListener(v -> {
                String valor = v.getTag().toString();
                if (esEstado) estadoSel = valor;
                else categoriaSel = valor;
                pintarOpciones(opciones, esEstado);
            });
        }
        pintarOpciones(opciones, esEstado);
    }

    private void pintarOpciones(TextView[] opciones, boolean esEstado) {
        String seleccionado = esEstado ? estadoSel : categoriaSel;
        for (TextView t : opciones) {
            String valor = t.getTag().toString();
            boolean activo = valor.equals(seleccionado);
            int colorActivo = esEstado ? colorEstado(valor) : AZUL;

            t.setBackground(activo ? forma(colorActivo, 14, 0) : forma(Color.WHITE, 14, BORDE));
            t.setTextColor(activo ? Color.WHITE : GRIS);
            t.setTypeface(null, activo ? Typeface.BOLD : Typeface.NORMAL);
            t.setCompoundDrawablePadding(dp(6));
            t.setCompoundDrawablesRelative(activo ? iconoCheck() : null, null, null, null);
        }
    }

    private Drawable iconoCheck() {
        Drawable d = ContextCompat.getDrawable(this, R.drawable.ic_check_circle);
        if (d == null) return null;
        d = DrawableCompat.wrap(d.mutate());
        DrawableCompat.setTint(d, Color.WHITE);
        d.setBounds(0, 0, dp(18), dp(18));
        return d;
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

    private void seleccionarFoto() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;

            try {
                Bitmap miniatura = leerBitmapReducido(uri, 900);
                ivPreview.setImageBitmap(miniatura);
                ivPreview.setVisibility(View.VISIBLE);
                llSubirContenido.setVisibility(View.GONE);
                tvCambiar.setVisibility(View.VISIBLE);
                imagenSeleccionada = uri;
            } catch (Exception e) {
                Toast.makeText(this, "No se pudo cargar la imagen", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void publicarAnuncio() {

        FirebaseUser usuario = mAuth.getCurrentUser();
        if (usuario == null) {
            irAlLogin();
            return;
        }

        tilTitulo.setError(null);
        tilDescripcion.setError(null);
        tilPrecio.setError(null);

        String titulo = etTitulo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String precioTexto = etPrecio.getText().toString().trim();

        if (titulo.isEmpty()) {
            tilTitulo.setError("Ingresa el título del material");
            etTitulo.requestFocus();
            return;
        }

        if (descripcion.isEmpty()) {
            tilDescripcion.setError("Ingresa una descripción");
            etDescripcion.requestFocus();
            return;
        }

        if (categoriaSel == null) {
            Toast.makeText(this, "Selecciona una categoría", Toast.LENGTH_SHORT).show();
            return;
        }

        if (estadoSel == null) {
            Toast.makeText(this, "Selecciona el estado de conservación", Toast.LENGTH_SHORT).show();
            return;
        }

        if (precioTexto.isEmpty()) {
            tilPrecio.setError("Ingresa el precio");
            etPrecio.requestFocus();
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioTexto);
        } catch (NumberFormatException e) {
            tilPrecio.setError("Ingresa un precio válido");
            etPrecio.requestFocus();
            return;
        }

        if (precio < 0) {
            tilPrecio.setError("El precio no puede ser negativo");
            etPrecio.requestFocus();
            return;
        }

        btnPublicarAnuncio.setEnabled(false);
        btnPublicarAnuncio.setText("Publicando...");

        Map<String, Object> anuncio = new HashMap<>();

        anuncio.put("titulo", titulo);
        anuncio.put("descripcion", descripcion);
        anuncio.put("precio", precio);
        anuncio.put("categoria", categoriaSel);
        anuncio.put("estado", estadoSel);
        anuncio.put("usuarioId", usuario.getUid());
        anuncio.put("vendedor", usuario.getEmail());
        anuncio.put("fechaCreacion", FieldValue.serverTimestamp());

        if (imagenSeleccionada != null) {
            try {
                anuncio.put("imagenBase64", imagenABase64(imagenSeleccionada));
                anuncio.put("tieneFoto", true);
            } catch (Exception e) {
                Toast.makeText(this, "No se pudo procesar la imagen", Toast.LENGTH_LONG).show();
                restaurarBoton();
                return;
            }
        } else {
            anuncio.put("tieneFoto", false);
        }

        db.collection("productos")
                .add(anuncio)
                .addOnSuccessListener(documentReference -> {
                    generarAvisoGlobal(titulo, categoriaSel, documentReference.getId());
                    Toast.makeText(this, "¡Anuncio publicado correctamente!", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(Publicar.this, Principal.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(error -> {
                    restaurarBoton();
                    Toast.makeText(this,
                            "Error al publicar: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }


    private void generarAvisoGlobal(String tituloProducto, String categoria, String productoId) {
        final FirebaseUser autor = mAuth.getCurrentUser();

        String topico = Topicos.deCategoria(categoria);

        Notificacion noti = new Notificacion(
                "Nueva publicación en " + categoria,
                "Alguien publicó: " + tituloProducto,
                "PUBLICACION",
                productoId,
                false
        );

        db.collection("notificaciones_publicaciones")
                .document(topico)
                .collection("avisos")
                .add(noti)
                .addOnSuccessListener(ref -> {
                    if (autor != null) {
                        ref.update("autorId", autor.getUid());
                    }
                });


    }

    private void restaurarBoton() {
        btnPublicarAnuncio.setEnabled(true);
        btnPublicarAnuncio.setText("Publicar anuncio");
    }

    private String imagenABase64(Uri uri) throws Exception {
        Bitmap bmp = leerBitmapReducido(uri, 800);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 70, salida);
        return Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP);
    }

    private Bitmap leerBitmapReducido(Uri uri, int maxLado) throws Exception {

        BitmapFactory.Options medir = new BitmapFactory.Options();
        medir.inJustDecodeBounds = true;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, medir);
        }

        int sample = 1;
        while (medir.outWidth / sample > maxLado * 2 || medir.outHeight / sample > maxLado * 2) {
            sample *= 2;
        }
        BitmapFactory.Options leer = new BitmapFactory.Options();
        leer.inSampleSize = sample;

        Bitmap bmp;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            bmp = BitmapFactory.decodeStream(in, null, leer);
        }
        if (bmp == null) throw new Exception("Imagen inválida");

        Matrix matriz = new Matrix();
        float ratio = Math.min((float) maxLado / bmp.getWidth(), (float) maxLado / bmp.getHeight());
        if (ratio < 1f) matriz.postScale(ratio, ratio);
        int rotacion = leerRotacion(uri);
        if (rotacion != 0) matriz.postRotate(rotacion);

        if (!matriz.isIdentity()) {
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matriz, true);
        }
        return bmp;
    }

    private int leerRotacion(Uri uri) {
        if (Build.VERSION.SDK_INT < 24) return 0;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return 0;
            ExifInterface exif = new ExifInterface(in);
            int orientacion = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientacion) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private GradientDrawable zonaPunteada() {
        GradientDrawable g = new GradientDrawable();
        g.setColor(CREMA);
        g.setCornerRadius(dp(18));
        g.setStroke(dp(2), MOSTAZA, dp(8), dp(6));
        return g;
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

    private void irAlLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}