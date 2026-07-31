package com.wendys.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Producto del menu de Wendy's (Baconator, Frosty, Papas, etc.)
 *
 * Lado "NO dueño" de la relacion con DetallePedido.
 * Un mismo Producto puede aparecer en muchos DetallePedido (uno por cada
 * vez que alguien lo pide), por eso @OneToMany(mappedBy = "producto").
 *
 * FetchType.LAZY aqui es clave: si cargas un Producto, NO quieres traerte
 * automaticamente todo el historial de pedidos donde aparecio.
 */
@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre; // Ej: "Baconator", "Frosty de chocolate", "Papas rizadas"

    @Column(nullable = false)
    private BigDecimal precio;

    // mappedBy = "producto" -> el dueño de la relacion es el campo "producto"
    // dentro de la clase DetallePedido. Aqui NO se crea ninguna columna nueva.
    @OneToMany(mappedBy = "producto", fetch = FetchType.LAZY)
    private List<DetallePedido> detalles = new ArrayList<>();

    public Producto() {
    }

    public Producto(String nombre, BigDecimal precio) {
        this.nombre = nombre;
        this.precio = precio;
    }

    // --- Getters y setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public List<DetallePedido> getDetalles() {
        return detalles;
    }

    @Override
    public String toString() {
        return "Producto{id=" + id + ", nombre='" + nombre + "', precio=" + precio + "}";
    }
}
