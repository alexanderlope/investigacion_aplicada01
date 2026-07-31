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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas SOLO para la entidad DetallePedido: sus dos relaciones
 * @ManyToOne (hacia Pedido y hacia Producto, ambas dueñas con
 * @JoinColumn) y el calculo de subtotal. No se prueba aqui cascada
 * de Cliente ni comportamiento propio de Producto.
 */
@DataJpaTest
class DetallePedidoRepositoryTest {

    @Autowired
    private DetallePedidoRepository detallePedidoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private EntityManager entityManager;

    // Contador para generar un correo distinto en cada llamada a crearPedidoBase().
    // Cliente.correo es unique, y algunos tests necesitan mas de un Cliente/Pedido
    // (ej. productoPuedeAparecerEnVariosDetalles), asi que un correo fijo chocaria.
    private final AtomicInteger contadorCliente = new AtomicInteger();

    private Pedido crearPedidoBase() {
        String correo = "prueba" + contadorCliente.incrementAndGet() + "@correo.com";
        Cliente cliente = clienteRepository.save(new Cliente("Cliente de prueba", correo));
        Pedido pedido = new Pedido(LocalDateTime.now(), "Para llevar");
        pedido.setCliente(cliente);
        return pedidoRepository.save(pedido);
    }

    @Test
    @DisplayName("Guardar un DetallePedido persiste sus relaciones con Pedido y Producto")
    void guardarDetalle_persisteRelacionesConPedidoYProducto() {
        Pedido pedido = crearPedidoBase();
        Producto baconator = productoRepository.save(new Producto("Baconator", new BigDecimal("6.99")));

        DetallePedido detalle = new DetallePedido(2, baconator.getPrecio(), baconator);
        detalle.setPedido(pedido);

        DetallePedido guardado = detallePedidoRepository.save(detalle);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getPedido().getId()).isEqualTo(pedido.getId());
        assertThat(guardado.getProducto().getId()).isEqualTo(baconator.getId());
    }

    @Test
    @DisplayName("getSubtotal() calcula cantidad x precioUnitario correctamente")
    void getSubtotal_calculaCorrectamente() {
        Pedido pedido = crearPedidoBase();
        Producto frosty = productoRepository.save(new Producto("Frosty de chocolate", new BigDecimal("1.99")));

        DetallePedido detalle = new DetallePedido(3, new BigDecimal("1.99"), frosty);
        detalle.setPedido(pedido);
        detallePedidoRepository.save(detalle);

        assertThat(detalle.getSubtotal()).isEqualByComparingTo(new BigDecimal("5.97")); // 3 x 1.99
    }

    @Test
    @DisplayName("FetchType.EAGER: al recargar el DetallePedido, Producto y Pedido vienen cargados de inmediato")
    void fetchEager_productoYPedidoDisponiblesSinTransaccionAdicional() {
        Pedido pedido = crearPedidoBase();
        Producto papas = productoRepository.save(new Producto("Papas rizadas", new BigDecimal("2.49")));

        DetallePedido detalle = new DetallePedido(1, papas.getPrecio(), papas);
        detalle.setPedido(pedido);
        detallePedidoRepository.save(detalle);
        entityManager.flush();
        entityManager.clear(); // fuerza a Hibernate a recargar desde la base de datos

        DetallePedido recargado = detallePedidoRepository.findById(detalle.getId()).orElseThrow();

        // Como ambas relaciones son EAGER, esto NO deberia lanzar
        // LazyInitializationException aunque estemos fuera del "clear".
        assertThat(recargado.getProducto().getNombre()).isEqualTo("Papas rizadas");
        assertThat(recargado.getPedido().getTipoEntrega()).isEqualTo("Para llevar");
    }

    @Test
    @DisplayName("Un mismo Producto puede aparecer en varios DetallePedido distintos")
    void productoPuedeAparecerEnVariosDetalles() {
        Pedido pedido1 = crearPedidoBase();
        Pedido pedido2 = crearPedidoBase();
        Producto baconator = productoRepository.save(new Producto("Baconator", new BigDecimal("6.99")));

        DetallePedido detalle1 = new DetallePedido(1, baconator.getPrecio(), baconator);
        detalle1.setPedido(pedido1);
        detallePedidoRepository.save(detalle1);

        DetallePedido detalle2 = new DetallePedido(2, baconator.getPrecio(), baconator);
        detalle2.setPedido(pedido2);
        detallePedidoRepository.save(detalle2);

        entityManager.flush();

        assertThat(detallePedidoRepository.count()).isEqualTo(2);
        assertThat(detalle1.getProducto().getId()).isEqualTo(detalle2.getProducto().getId());
    }

    @Test
    @DisplayName("Eliminar un DetallePedido no afecta al Pedido ni al Producto relacionados")
    void eliminarDetalle_noAfectaPedidoNiProducto() {
        Pedido pedido = crearPedidoBase();
        Producto frosty = productoRepository.save(new Producto("Frosty", new BigDecimal("1.99")));

        DetallePedido detalle = new DetallePedido(1, frosty.getPrecio(), frosty);
        detalle.setPedido(pedido);
        detallePedidoRepository.save(detalle);
        entityManager.flush();

        detallePedidoRepository.delete(detalle);
        entityManager.flush();

        assertThat(detallePedidoRepository.count()).isEqualTo(0);
        assertThat(pedidoRepository.findById(pedido.getId())).isPresent();
        assertThat(productoRepository.findById(frosty.getId())).isPresent();
    }
}
