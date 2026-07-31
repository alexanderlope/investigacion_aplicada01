package com.wendys.demo.repository;

import com.wendys.demo.entity.Cliente;
import com.wendys.demo.entity.DetallePedido;
import com.wendys.demo.entity.Pedido;
import com.wendys.demo.entity.Producto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas SOLO para la entidad Pedido: su relacion @ManyToOne hacia
 * Cliente (lado dueño, @JoinColumn cliente_id) y su relacion
 * @OneToMany hacia DetallePedido (mappedBy, CascadeType.ALL,
 * orphanRemoval). No se prueba aqui el comportamiento de Producto
 * ni de Cliente por separado, solo lo que involucra a Pedido.
 */
@DataJpaTest
class PedidoRepositoryTest {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private DetallePedidoRepository detallePedidoRepository;

    @Autowired
    private EntityManager entityManager;

    private Cliente crearClienteBase() {
        return clienteRepository.save(new Cliente("Cliente de prueba", "prueba@correo.com"));
    }

    @Test
    @DisplayName("Guardar un Pedido requiere y persiste su relacion @ManyToOne con Cliente")
    void guardarPedido_persisteRelacionConCliente() {
        Cliente cliente = crearClienteBase();
        Pedido pedido = new Pedido(LocalDateTime.now(), "Para llevar");
        pedido.setCliente(cliente);

        Pedido guardado = pedidoRepository.save(pedido);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getCliente().getId()).isEqualTo(cliente.getId());
    }

    @Test
    @DisplayName("Un Pedido nuevo empieza con la lista de detalles vacia")
    void pedidoNuevo_sinDetalles() {
        Cliente cliente = crearClienteBase();
        Pedido pedido = new Pedido(LocalDateTime.now(), "Delivery");
        pedido.setCliente(cliente);
        pedidoRepository.save(pedido);

        assertThat(pedido.getDetalles()).isEmpty();
    }

    @Test
    @DisplayName("agregarDetalle + CascadeType.ALL guarda los DetallePedido con un solo save() del Pedido")
    void agregarDetalle_cascadaGuardaDetallesAutomaticamente() {
        Cliente cliente = crearClienteBase();
        Producto baconator = productoRepository.save(new Producto("Baconator", new BigDecimal("6.99")));
        Producto frosty = productoRepository.save(new Producto("Frosty", new BigDecimal("1.99")));

        Pedido pedido = new Pedido(LocalDateTime.now(), "En restaurante");
        pedido.setCliente(cliente);
        pedido.agregarDetalle(new DetallePedido(2, baconator.getPrecio(), baconator));
        pedido.agregarDetalle(new DetallePedido(1, frosty.getPrecio(), frosty));

        pedidoRepository.save(pedido); // un solo save
        entityManager.flush();
        entityManager.clear();

        Pedido recargado = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertThat(recargado.getDetalles()).hasSize(2);
        assertThat(detallePedidoRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("orphanRemoval: quitar un DetallePedido de la lista del Pedido lo borra de la base de datos")
    void quitarDetalleDeLaLista_loEliminaPorOrphanRemoval() {
        Cliente cliente = crearClienteBase();
        Producto papas = productoRepository.save(new Producto("Papas rizadas", new BigDecimal("2.49")));

        Pedido pedido = new Pedido(LocalDateTime.now(), "Para llevar");
        pedido.setCliente(cliente);
        DetallePedido detalle = new DetallePedido(3, papas.getPrecio(), papas);
        pedido.agregarDetalle(detalle);
        pedidoRepository.save(pedido);
        entityManager.flush();

        assertThat(detallePedidoRepository.count()).isEqualTo(1);

        pedido.quitarDetalle(detalle);
        pedidoRepository.save(pedido);
        entityManager.flush();

        assertThat(detallePedidoRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Borrar un Pedido borra en cascada sus DetallePedido, pero NO los Producto")
    void borrarPedido_borraDetallesPeroNoProductos() {
        Cliente cliente = crearClienteBase();
        Producto baconator = productoRepository.save(new Producto("Baconator", new BigDecimal("6.99")));

        Pedido pedido = new Pedido(LocalDateTime.now(), "Delivery");
        pedido.setCliente(cliente);
        pedido.agregarDetalle(new DetallePedido(1, baconator.getPrecio(), baconator));
        pedidoRepository.save(pedido);
        entityManager.flush();

        assertThat(detallePedidoRepository.count()).isEqualTo(1);
        assertThat(productoRepository.count()).isEqualTo(1);

        pedidoRepository.delete(pedido);
        entityManager.flush();

        assertThat(detallePedidoRepository.count()).isEqualTo(0);
        assertThat(productoRepository.count()).isEqualTo(1); // el producto sigue existiendo
    }
}
