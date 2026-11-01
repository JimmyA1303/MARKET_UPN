package com.example.market_upn;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import com.google.firebase.messaging.FirebaseMessaging;

public class Registro extends AppCompatActivity {
    private static final String UNIVERSIDAD = "Universidad Privada del Norte";

    private TextInputEditText etNombres;
    private TextInputEditText etApellidos;
    private TextInputEditText etCodigo;
    private TextInputEditText etCorreoRegistro;
    private TextInputEditText etPasswordRegistro;
    private AutoCompleteTextView autoCarrera;
    private MaterialButton btnCrearCuenta;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etNombres = findViewById(R.id.etNombres);
        etApellidos = findViewById(R.id.etApellidos);
        etCodigo = findViewById(R.id.etCodigo);
        etCorreoRegistro = findViewById(R.id.etCorreoRegistro);
        etPasswordRegistro = findViewById(R.id.etPasswordRegistro);
        autoCarrera = findViewById(R.id.autoCarrera);
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta);

        String[] carreras = {
                "Ingeniería de Sistemas Computacionales",
                "Ingeniería de Software",
                "Ingeniería Industrial",
                "Administración",
                "Contabilidad",
                "Arquitectura",
                "Derecho",
                "Psicología",
                "Marketing",
                "Negocios Internacionales"
        };
        autoCarrera.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, carreras));

        btnCrearCuenta.setOnClickListener(v -> validarRegistro());

        TextView tvIniciarSesion = findViewById(R.id.tvIniciarSesion);
        tvIniciarSesion.setOnClickListener(v -> {
            startActivity(new Intent(Registro.this, MainActivity.class));
            finish();
        });
    }

    private void validarRegistro() {
        String nombres = etNombres.getText().toString().trim();
        String apellidos = etApellidos.getText().toString().trim();
        String codigo = etCodigo.getText().toString().trim().toUpperCase();
        String correo = etCorreoRegistro.getText().toString().trim().toLowerCase();
        String carrera = autoCarrera.getText().toString().trim();
        String password = etPasswordRegistro.getText().toString().trim();

        if (nombres.isEmpty()) {
            etNombres.setError("Ingresa tus nombres");
            etNombres.requestFocus();
            return;
        }
        if (apellidos.isEmpty()) {
            etApellidos.setError("Ingresa tus apellidos");
            etApellidos.requestFocus();
            return;
        }
        if (codigo.isEmpty()) {
            etCodigo.setError("Ingresa tu código de estudiante");
            etCodigo.requestFocus();
            return;
        }
        if (correo.isEmpty()) {
            etCorreoRegistro.setError("Ingresa tu correo institucional");
            etCorreoRegistro.requestFocus();
            return;
        }
        if (!correo.endsWith("@upn.pe")) {
            etCorreoRegistro.setError("Debes utilizar un correo institucional UPN");
            etCorreoRegistro.requestFocus();
            return;
        }
        if (carrera.isEmpty()) {
            autoCarrera.setError("Selecciona tu carrera profesional");
            autoCarrera.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPasswordRegistro.setError("Ingresa una contraseña");
            etPasswordRegistro.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPasswordRegistro.setError("La contraseña debe tener al menos 6 caracteres");
            etPasswordRegistro.requestFocus();
            return;
        }

        mostrarCarga(true);

        mAuth.createUserWithEmailAndPassword(correo, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            guardarPerfil(user, nombres, apellidos, codigo, correo, carrera);
                        } else {
                            mostrarCarga(false);
                        }
                    } else {
                        mostrarCarga(false);
                        Exception e = task.getException();
                        String mensaje;
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            mensaje = "Este correo ya está registrado.";
                        } else {
                            mensaje = "No se pudo crear la cuenta: "
                                    + (e != null && e.getMessage() != null ? e.getMessage() : "error desconocido");
                        }
                        Toast.makeText(Registro.this, mensaje, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void guardarPerfil(FirebaseUser user, String nombres, String apellidos,
                               String codigo, String correo, String carrera) {
        Map<String, Object> perfil = new HashMap<>();
        perfil.put("nombres", nombres);
        perfil.put("apellidos", apellidos);
        perfil.put("codigo", codigo);
        perfil.put("correo", correo);
        perfil.put("carrera", carrera);
        perfil.put("universidad", UNIVERSIDAD);
        perfil.put("creadoEn", FieldValue.serverTimestamp());

        db.collection("usuarios")
                .document(user.getUid())
                .set(perfil)
                .addOnSuccessListener(unused -> {
                    suscribirATopico(carrera);
                    enviarVerificacion(user);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Registro.this,
                            "Cuenta creada, pero no se pudieron guardar tus datos: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    enviarVerificacion(user);
                });
    }

    private void suscribirATopico(String carrera) {
        if (carrera != null && !carrera.isEmpty()) {
            String topico = "cat_" + carrera.toLowerCase().replaceAll("[^a-z0-9]", "_");
            FirebaseMessaging.getInstance().subscribeToTopic(topico)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                        }
                    });
        }
    }

    private void enviarVerificacion(FirebaseUser user) {
        user.sendEmailVerification().addOnCompleteListener(emailTask -> {
            mostrarCarga(false);

            if (emailTask.isSuccessful()) {
                Toast.makeText(Registro.this,
                        "Cuenta creada. Revisa tu correo institucional para verificar tu cuenta.",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(Registro.this,
                        "La cuenta fue creada, pero no se pudo enviar el correo de verificación.",
                        Toast.LENGTH_LONG).show();
            }

            mAuth.signOut();

            Intent intent = new Intent(Registro.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void mostrarCarga(boolean cargando) {
        btnCrearCuenta.setEnabled(!cargando);
        btnCrearCuenta.setText(cargando ? "Creando cuenta..." : "Crear cuenta");
    }
}