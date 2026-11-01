package com.example.market_upn;

import java.util.Date;

public class ChatResumen {
    private String otroUsuarioId;
    private String productoId;
    private String otroUsuarioNombre;
    private String tituloProducto;
    private String ultimoMensaje;
    private Date fechaUltimoMensaje;

    public ChatResumen() {}

    public ChatResumen(String otroUsuarioId, String productoId) {
        this.otroUsuarioId = otroUsuarioId;
        this.productoId = productoId;
        this.otroUsuarioNombre = "Cargando...";
        this.tituloProducto = "Cargando producto...";
        this.ultimoMensaje = "";
    }

    public String getOtroUsuarioId() { return otroUsuarioId; }
    public void setOtroUsuarioId(String otroUsuarioId) { this.otroUsuarioId = otroUsuarioId; }

    public String getProductoId() { return productoId; }
    public void setProductoId(String productoId) { this.productoId = productoId; }

    public String getOtroUsuarioNombre() { return otroUsuarioNombre; }
    public void setOtroUsuarioNombre(String otroUsuarioNombre) { this.otroUsuarioNombre = otroUsuarioNombre; }

    public String getTituloProducto() { return tituloProducto; }
    public void setTituloProducto(String tituloProducto) { this.tituloProducto = tituloProducto; }

    public String getUltimoMensaje() { return ultimoMensaje; }
    public void setUltimoMensaje(String ultimoMensaje) { this.ultimoMensaje = ultimoMensaje; }

    public Date getFechaUltimoMensaje() { return fechaUltimoMensaje; }
    public void setFechaUltimoMensaje(Date fechaUltimoMensaje) { this.fechaUltimoMensaje = fechaUltimoMensaje; }
}
