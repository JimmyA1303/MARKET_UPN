package com.example.market_upn;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private TextInputEditText etCorreo;
    private TextInputEditText etPassword;
    private MaterialButton btnIniciarSesion;
    private TextView tvRegistrate;
    private TextView tvOlvidePassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etCorreo = findViewById(R.id.etCorreo);
        etPassword = findViewById(R.id.etPassword);
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion);
        tvRegistrate = findViewById(R.id.tvRegistrate);
        tvOlvidePassword = findViewById(R.id.tvOlvidePassword);

        btnIniciarSesion.setOnClickListener(v -> iniciarSesion());

        tvRegistrate.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Registro.class);
            startActivity(intent);
        });

        tvOlvidePassword.setOnClickListener(v -> recuperarPassword());

        animarEntrada();
    }


    private void animarEntrada() {
        View card = findViewById(R.id.layoutContenedorPrincipal);
        card.setAlpha(0f);
        card.setTranslationY(80f);
        card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        int[] ids = {R.id.chipCompra, R.id.chipVende, R.id.chipIntercambia};
        for (int i = 0; i < ids.length; i++) {
            View chip = findViewById(ids[i]);
            chip.setAlpha(0f);
            chip.setTranslationY(30f);
            chip.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(300 + i * 150L)
                    .setDuration(450)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void mostrarCarga(boolean cargando) {
        btnIniciarSesion.setEnabled(!cargando);
        btnIniciarSesion.setText(cargando ? "Ingresando..." : "Iniciar sesión");
    }

    private void iniciarSesion() {
        String correo = etCorreo.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString().trim();

        if (correo.isEmpty()) {
            etCorreo.setError("Ingresa tu correo institucional");
            etCorreo.requestFocus();
            return;
        }

        if (!correo.endsWith("@upn.pe")) {
            etCorreo.setError("Debes utilizar un correo institucional UPN");
            etCorreo.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Ingresa tu contraseña");
            etPassword.requestFocus();
            return;
        }

        mostrarCarga(true);

        mAuth.signInWithEmailAndPassword(correo, password)
                .addOnCompleteListener(this, task -> {
                    mostrarCarga(false);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {

                                db.collection("usuarios").document(user.getUid()).get()
                                        .addOnSuccessListener(documentSnapshot -> {
                                            if (documentSnapshot.exists()) {
                                                String carrera = documentSnapshot.getString("carrera");
                                                if (carrera != null && !carrera.isEmpty()) {
                                                    String topico = "cat_" + carrera.toLowerCase().replaceAll("[^a-z0-9]", "_");
                                                    FirebaseMessaging.getInstance().subscribeToTopic(topico);
                                                }
                                            }
                                        });

                                int numeroAleatorio = 100000 + new Random().nextInt(900000);
                                String codigoMFA = String.valueOf(numeroAleatorio);

                                codigo_verificacion_dialog.enviarCorreoCodigoGratis(correo, codigoMFA);

                                Intent intent = new Intent(MainActivity.this, codigo_verificacion_dialog.class);
                                intent.putExtra("CODIGO_MFA", codigoMFA);
                                startActivity(intent);

                            } else {
                                Toast.makeText(MainActivity.this, "Debes verificar tu correo institucional antes de iniciar sesión.", Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                            }
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Correo o contraseña incorrectos.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void recuperarPassword() {
        String correo = etCorreo.getText().toString().trim().toLowerCase();

        if (correo.isEmpty()) {
            etCorreo.setError("Ingresa primero tu correo institucional");
            etCorreo.requestFocus();
            return;
        }

        if (!correo.endsWith("@upn.pe")) {
            etCorreo.setError("Debes utilizar un correo institucional UPN");
            etCorreo.requestFocus();
            return;
        }

        mAuth.sendPasswordResetEmail(correo)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Se ha enviado un enlace para restablecer tu contraseña.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "No se pudo enviar el correo de recuperación.", Toast.LENGTH_LONG).show();
                    }
                });
    }
}