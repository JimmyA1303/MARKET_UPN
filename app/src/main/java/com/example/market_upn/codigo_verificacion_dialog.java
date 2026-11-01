package com.example.market_upn;

import android.content.Intent;
import android.os.Bundle;
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

import java.util.Properties;
import java.util.Random;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class codigo_verificacion_dialog extends AppCompatActivity {

    private EditText[] etOtps = new EditText[6];
    private TextView tvErrorOtp;
    private MaterialButton btnDialogVerificar;
    private MaterialButton btnDialogCancelar;
    private MaterialButton btnReenviar;

    private FirebaseAuth mAuth;

    private String codigoCorrecto;
    private int intentos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_codigo_verificacion_dialog);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets systemBars =
                            insets.getInsets(WindowInsetsCompat.Type.systemBars());

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        mAuth = FirebaseAuth.getInstance();

        codigoCorrecto = getIntent().getStringExtra("CODIGO_MFA");

        etOtps[0] = findViewById(R.id.etOtp1);
        etOtps[1] = findViewById(R.id.etOtp2);
        etOtps[2] = findViewById(R.id.etOtp3);
        etOtps[3] = findViewById(R.id.etOtp4);
        etOtps[4] = findViewById(R.id.etOtp5);
        etOtps[5] = findViewById(R.id.etOtp6);
        tvErrorOtp = findViewById(R.id.tvErrorOtp);

        btnDialogVerificar = findViewById(R.id.btnDialogVerificar);
        btnDialogCancelar = findViewById(R.id.btnDialogCancelar);
        btnReenviar = findViewById(R.id.btnReenviar);

        configurarInputsOtp();

        btnDialogVerificar.setOnClickListener(v -> {

            String codigoIngresado = obtenerCodigoIngresado();

            if (codigoIngresado.length() != 6) {
                mostrarError("Completa el código de 6 dígitos");
                return;
            }

            if (codigoIngresado.equals(codigoCorrecto)) {

                Toast.makeText(
                        codigo_verificacion_dialog.this,
                        "¡Inicio de sesión exitoso!",
                        Toast.LENGTH_SHORT
                ).show();

                Intent intent =
                        new Intent(
                                codigo_verificacion_dialog.this,
                                Principal.class
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                );

                startActivity(intent);

                finish();

            } else {

                intentos++;

                int restantes = 3 - intentos;

                if (restantes <= 0) {

                    Toast.makeText(
                            this,
                            "Demasiados intentos fallidos. Inicia sesión nuevamente.",
                            Toast.LENGTH_LONG
                    ).show();

                    mAuth.signOut();

                    Intent intent =
                            new Intent(
                                    codigo_verificacion_dialog.this,
                                    MainActivity.class
                            );

                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);

                    finish();

                } else {

                    limpiarInputs();
                    mostrarError("Código incorrecto. Intentos restantes: " + restantes);

                    Toast.makeText(
                            this,
                            "Código incorrecto. Intentos restantes: "
                                    + restantes,
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });


        btnDialogCancelar.setOnClickListener(v -> {

            mAuth.signOut();

            Intent intent =
                    new Intent(
                            codigo_verificacion_dialog.this,
                            MainActivity.class
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);

            finish();
        });


        btnReenviar.setOnClickListener(v -> reenviarCodigo());
    }

    private void configurarInputsOtp() {
        for (int i = 0; i < etOtps.length; i++) {
            final int currentIndex = i;
            etOtps[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    tvErrorOtp.setVisibility(View.GONE);
                    if (s.length() == 1 && currentIndex < etOtps.length - 1) {
                        etOtps[currentIndex + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            etOtps[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (etOtps[currentIndex].getText().toString().isEmpty() && currentIndex > 0) {
                        etOtps[currentIndex - 1].requestFocus();
                        etOtps[currentIndex - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private String obtenerCodigoIngresado() {
        StringBuilder sb = new StringBuilder();
        for (EditText et : etOtps) {
            sb.append(et.getText().toString().trim());
        }
        return sb.toString();
    }

    private void limpiarInputs() {
        for (EditText et : etOtps) {
            et.setText("");
        }
        etOtps[0].requestFocus();
    }

    private void mostrarError(String mensaje) {
        tvErrorOtp.setText(mensaje);
        tvErrorOtp.setVisibility(View.VISIBLE);
    }

    public static void enviarCorreoCodigoGratis(
            String correoDestino,
            String codigoSecreto) {

        new Thread(() -> {

            final String correoEmisor =
                    "monica19hernandez.q@gmail.com";

            final String contraseniaApp =
                    "iejlnkrdfomjucrp";

            Properties props = new Properties();

            props.put(
                    "mail.smtp.host",
                    "smtp.gmail.com"
            );

            props.put(
                    "mail.smtp.auth",
                    "true"
            );

            props.put(
                    "mail.smtp.starttls.enable",
                    "true"
            );

            props.put(
                    "mail.smtp.port",
                    "587"
            );

            props.put(
                    "mail.smtp.ssl.protocols",
                    "TLSv1.2"
            );

            Session session =
                    Session.getInstance(
                            props,
                            new Authenticator() {

                                @Override
                                protected PasswordAuthentication
                                getPasswordAuthentication() {

                                    return new PasswordAuthentication(
                                            correoEmisor,
                                            contraseniaApp
                                    );
                                }
                            }
                    );

            try {

                Message message =
                        new MimeMessage(session);

                message.setFrom(
                        new InternetAddress(correoEmisor)
                );

                message.setRecipients(
                        Message.RecipientType.TO,
                        InternetAddress.parse(correoDestino)
                );

                message.setSubject(
                        "Código de Verificación - MARKET UPN"
                );

                message.setText(
                        "Hola,\n\n" +
                                "Tu código de acceso para MARKET UPN es: "
                                + codigoSecreto +
                                "\n\n" +
                                "Por seguridad, no compartas este código con nadie."
                );

                Transport.send(message);

                System.out.println(
                        "Correo enviado correctamente."
                );

            } catch (MessagingException e) {

                e.printStackTrace();

                System.out.println(
                        "Error enviando correo: "
                                + e.getMessage()
                );
            }

        }).start();
    }

    private void reenviarCodigo() {

        intentos = 0;

        FirebaseUser user =
                mAuth.getCurrentUser();

        if (user == null ||
                user.getEmail() == null) {

            Toast.makeText(
                    this,
                    "Sesión no encontrada.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        codigoCorrecto =
                String.valueOf(
                        100000 +
                                new Random().nextInt(900000)
                );

        enviarCorreoCodigoGratis(
                user.getEmail(),
                codigoCorrecto
        );

        limpiarInputs();
        tvErrorOtp.setVisibility(View.GONE);

        Toast.makeText(
                this,
                "Se envió un nuevo código a tu correo.",
                Toast.LENGTH_SHORT
        ).show();
    }
}