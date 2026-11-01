package com.example.market_upn;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Mensaje {
    private String id;
    private String remitenteId;
    private String receptorId;
    private String texto;
    private String productoId;
    @ServerTimestamp
    private Date fechaEnvio;

    public Mensaje() {
    }

    public Mensaje(String remitenteId, String receptorId, String texto, String productoId) {
        this.remitenteId = remitenteId;
        this.receptorId = receptorId;
        this.texto = texto;
        this.productoId = productoId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRemitenteId() { return remitenteId; }
    public void setRemitenteId(String remitenteId) { this.remitenteId = remitenteId; }

    public String getReceptorId() { return receptorId; }
    public void setReceptorId(String receptorId) { this.receptorId = receptorId; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public String getProductoId() { return productoId; }
    public void setProductoId(String productoId) { this.productoId = productoId; }

    public Date getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(Date fechaEnvio) { this.fechaEnvio = fechaEnvio; }
}