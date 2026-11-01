package com.example.market_upn;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Producto {
    private String id;
    private String usuarioId;
    private String titulo;
    private String categoria;
    private String estado;
    private double precio;
    private String sede;
    private String descripcion;
    private String vendedor;
    private String imagenBase64;
    @ServerTimestamp
    private Date fechaCreacion;

    public Producto() {}

    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public String getUsuarioId() { return usuarioId != null ? usuarioId : ""; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getTitulo() { return titulo != null ? titulo : ""; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getCategoria() { return categoria != null ? categoria : ""; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getEstado() { return estado != null ? estado : ""; }
    public void setEstado(String estado) { this.estado = estado; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public String getSede() { return sede != null ? sede : ""; }
    public void setSede(String sede) { this.sede = sede; }

    public String getDescripcion() { return descripcion != null ? descripcion : ""; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getVendedor() { return vendedor != null ? vendedor : ""; }
    public void setVendedor(String vendedor) { this.vendedor = vendedor; }

    public String getImagenBase64() { return imagenBase64 != null ? imagenBase64 : ""; }
    public void setImagenBase64(String imagenBase64) { this.imagenBase64 = imagenBase64; }

    public Date getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Date fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}