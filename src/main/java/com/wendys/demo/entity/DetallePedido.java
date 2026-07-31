package com.wendys.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * DetallePedido = una linea dentro de un pedido.
 * Ej: "2 x Baconator" o "1 x Frosty de chocolate".
 *
 * Esta entidad resuelve la relacion muchos-a-muchos entre Pedido y
 * Producto (un pedido tiene muchos productos, un producto aparece en
 * muchos pedidos), pero de forma "enriquecida": aqui guardamos ademas
 * la cantidad y el precio unitario en el momento de la compra.
 *
 * Es DUEÑA de DOS relaciones @ManyToOne:
 *  - hacia Pedido   (@JoinColumn pedido_id)
 *  - hacia Producto (@JoinColumn producto_id)
 */
@Entity
@Table(name = "detalle_pedido")
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private BigDecimal precioUnitario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    // EAGER aqui: cuando lees un detalle, casi siempre quieres saber
    // de inmediato QUE producto es (nombre, precio), sin otra consulta.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    public DetallePedido() {
    }

    public DetallePedido(Integer cantidad, BigDecimal precioUnitario, Producto producto) {
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.producto = producto;
    }

    public BigDecimal getSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    // --- Getters y setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    @Override
    public String toString() {
        return "DetallePedido{id=" + id + ", cantidad=" + cantidad
                + ", producto=" + (producto != null ? producto.getNombre() : "null")
                + ", subtotal=" + getSubtotal() + "}";
    }
}
