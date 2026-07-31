package com.wendys.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, length = 30)
    private String tipoEntrega; // "En restaurante", "Para llevar", "Delivery"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado;

    // Lado DUEÑO de la relacion: aqui vive la clave foranea cliente_id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Lado NO dueño: se apoya en el campo "pedido" de DetallePedido
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetallePedido> detalles = new ArrayList<>();

    public Pedido() {
    }

    public Pedido(LocalDateTime fecha, String tipoEntrega) {
        this.fecha = fecha;
        this.tipoEntrega = tipoEntrega;
        this.estado = EstadoPedido.RECIBIDO;
    }

    /**
     * Valor total del pedido. No se persiste: se obtiene de sus detalles
     * para evitar almacenar un dato derivado que podria quedar desactualizado.
     */
    @Transient
    public BigDecimal getTotal() {
        return detalles.stream()
                .map(DetallePedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @PrePersist
    private void prepararPedidoNuevo() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoPedido.RECIBIDO;
        }
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

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
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

    @Override
    public String toString() {
        return "Pedido{id=" + id + ", fecha=" + fecha + ", tipoEntrega='" + tipoEntrega
                + "', estado=" + estado + ", total=" + getTotal() + "}";
    }

    public enum EstadoPedido {
        RECIBIDO,
        EN_PREPARACION,
        LISTO,
        ENTREGADO,
        CANCELADO
    }
}
