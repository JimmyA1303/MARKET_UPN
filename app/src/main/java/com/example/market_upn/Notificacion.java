package com.example.market_upn;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Notificacion {
    private String id;
    private String titulo;
    private String mensaje;
    private String tipo;
    private String referenciaId;
    private boolean leida;
    
    @ServerTimestamp
    private Date fechaCreacion;

    public Notificacion() {}

    public Notificacion(String titulo, String mensaje, String tipo, String referenciaId, boolean leida) {
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.referenciaId = referenciaId;
        this.leida = leida;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getReferenciaId() { return referenciaId; }
    public void setReferenciaId(String referenciaId) { this.referenciaId = referenciaId; }

    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }

    public Date getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Date fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}