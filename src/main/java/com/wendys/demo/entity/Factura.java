package com.wendys.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Factura emitida para un Pedido.
 *
 * Relacion 1 a 1: Factura es el lado DUEÑO (aqui vive la columna
 * pedido_id, con unique = true para garantizar que un Pedido no
 * pueda tener mas de una Factura).
 */
@Entity
@Table(name = "factura")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String numeroFactura;

    @Column(nullable = false)
    private LocalDateTime fechaEmision;

    @Column(nullable = false)
    private BigDecimal totalCobrado;

    // Relación 1 a 1: La factura es dueña de la relación (aquí se guarda el pedido_id)
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    public Factura() {
    }

    public Factura(String numeroFactura, LocalDateTime fechaEmision, BigDecimal totalCobrado, Pedido pedido) {
        this.numeroFactura = numeroFactura;
        this.fechaEmision = fechaEmision;
        this.totalCobrado = totalCobrado;
        this.pedido = pedido;
    }

    @PrePersist
    private void prepararFactura() {
        if (fechaEmision == null) {
            fechaEmision = LocalDateTime.now();
        }
    }

    // --- Getters y setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }

    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }

    public BigDecimal getTotalCobrado() { return totalCobrado; }
    public void setTotalCobrado(BigDecimal totalCobrado) { this.totalCobrado = totalCobrado; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    @Override
    public String toString() {
        return "Factura{id=" + id + ", numero='" + numeroFactura + "', total=$" + totalCobrado + "}";
    }
}
