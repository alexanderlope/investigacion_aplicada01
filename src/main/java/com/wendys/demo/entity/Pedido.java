package com.wendys.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido hecho por un Cliente (ej: "Pedido #1 de Juan").
 *
 * Relaciones:
 * 1) Pedido -> Cliente  : @ManyToOne (lado DUEÑO, tiene @JoinColumn)
 * 2) Pedido -> Detalles : @OneToMany (lado NO dueño, mappedBy)
 * 3) Pedido -> Factura  : @OneToOne  (lado NO dueño, mappedBy).
 *    El dueño real es Factura, que tiene la columna pedido_id.
 *
 * FetchType.EAGER en "cliente": tiene sentido porque casi siempre que
 * muestras un pedido, quieres saber de quien es, sin una consulta extra.
 *
 * CascadeType.ALL + orphanRemoval = true en "detalles": si borras un
 * Pedido, se borran sus DetallePedido. Si quitas un detalle de la lista
 * y guardas, ese detalle huerfano se elimina de la base de datos.
 */
@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    private String tipoEntrega; // "En restaurante", "Para llevar", "Delivery"

    // Lado DUEÑO de la relacion: aqui vive la clave foranea cliente_id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Lado NO dueño: se apoya en el campo "pedido" de DetallePedido
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetallePedido> detalles = new ArrayList<>();

    // Lado NO dueño de la relacion 1:1. El dueño es Factura (tiene @JoinColumn pedido_id).
    // No se pone cascade aqui a proposito: emitir/borrar una Factura es una accion
    // independiente, no algo que deba pasar automaticamente al tocar el Pedido.
    @OneToOne(mappedBy = "pedido", fetch = FetchType.LAZY)
    private Factura factura;

    public Pedido() {
    }

    public Pedido(LocalDateTime fecha, String tipoEntrega) {
        this.fecha = fecha;
        this.tipoEntrega = tipoEntrega;
    }

    public void agregarDetalle(DetallePedido detalle) {
        detalles.add(detalle);
        detalle.setPedido(this);
    }

    public void quitarDetalle(DetallePedido detalle) {
        detalles.remove(detalle);
        detalle.setPedido(null); // dispara orphanRemoval al guardar
    }

    // --- Getters y setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getTipoEntrega() {
        return tipoEntrega;
    }

    public void setTipoEntrega(String tipoEntrega) {
        this.tipoEntrega = tipoEntrega;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public List<DetallePedido> getDetalles() {
        return detalles;
    }

    public Factura getFactura() {
        return factura;
    }

    public void setFactura(Factura factura) {
        this.factura = factura;
    }

    @Override
    public String toString() {
        return "Pedido{id=" + id + ", fecha=" + fecha + ", tipoEntrega='" + tipoEntrega + "'}";
    }
}
