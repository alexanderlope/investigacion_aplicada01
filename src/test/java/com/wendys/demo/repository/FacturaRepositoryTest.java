package com.wendys.demo.repository;

import com.wendys.demo.entity.Cliente;
import com.wendys.demo.entity.Factura;
import com.wendys.demo.entity.Pedido;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas SOLO para la entidad Factura y su relacion @OneToOne con
 * Pedido. Factura es el lado DUEÑO (tiene @JoinColumn pedido_id,
 * unique = true); Pedido es el lado NO dueño (mappedBy = "pedido").
 */
@DataJpaTest
class FacturaRepositoryTest {

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EntityManager entityManager;

    private Pedido crearPedidoBase() {
        Cliente cliente = clienteRepository.save(new Cliente("Cliente de prueba", "prueba@correo.com"));
        Pedido pedido = new Pedido(LocalDateTime.now(), "Para llevar");
        pedido.setCliente(cliente);
        return pedidoRepository.save(pedido);
    }

    @Test
    @DisplayName("Guardar una Factura persiste su relacion @OneToOne con Pedido")
    void guardarFactura_persisteRelacionConPedido() {
        Pedido pedido = crearPedidoBase();

        Factura factura = new Factura("FAC-0001", LocalDateTime.now(), new BigDecimal("18.45"), pedido);
        Factura guardada = facturaRepository.save(factura);

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getPedido().getId()).isEqualTo(pedido.getId());
        assertThat(guardada.getNumeroFactura()).isEqualTo("FAC-0001");
    }

    @Test
    @DisplayName("@PrePersist asigna fechaEmision automaticamente si no se especifica")
    void prePersist_asignaFechaEmisionSiEsNula() {
        Pedido pedido = crearPedidoBase();

        Factura factura = new Factura("FAC-0002", null, new BigDecimal("10.00"), pedido);
        Factura guardada = facturaRepository.save(factura);

        assertThat(guardada.getFechaEmision()).isNotNull();
    }

    @Test
    @DisplayName("FetchType.EAGER: al recargar la Factura, el Pedido viene cargado de inmediato")
    void fetchEager_pedidoDisponibleSinTransaccionAdicional() {
        Pedido pedido = crearPedidoBase();
        Factura factura = facturaRepository.save(
                new Factura("FAC-0003", LocalDateTime.now(), new BigDecimal("6.99"), pedido));
        entityManager.flush();
        entityManager.clear();

        Factura recargada = facturaRepository.findById(factura.getId()).orElseThrow();

        // EAGER: no deberia lanzar LazyInitializationException aunque
        // el EntityManager ya haya sido limpiado.
        assertThat(recargada.getPedido().getTipoEntrega()).isEqualTo("Para llevar");
    }

    @Test
    @DisplayName("Desde Pedido (lado no dueño, mappedBy) tambien se puede llegar a su Factura")
    void ladoNoDueño_pedidoAccedeASuFactura() {
        Pedido pedido = crearPedidoBase();
        facturaRepository.save(new Factura("FAC-0004", LocalDateTime.now(), new BigDecimal("12.50"), pedido));
        entityManager.flush();
        entityManager.clear();

        Pedido pedidoRecargado = pedidoRepository.findById(pedido.getId()).orElseThrow();

        assertThat(pedidoRecargado.getFactura()).isNotNull();
        assertThat(pedidoRecargado.getFactura().getNumeroFactura()).isEqualTo("FAC-0004");
    }

    @Test
    @DisplayName("No se puede crear una segunda Factura para el mismo Pedido (unique = true)")
    void noPermiteDosFacturasParaElMismoPedido() {
        Pedido pedido = crearPedidoBase();
        facturaRepository.save(new Factura("FAC-0005", LocalDateTime.now(), new BigDecimal("5.00"), pedido));
        entityManager.flush();

        Factura segundaFactura = new Factura("FAC-0006", LocalDateTime.now(), new BigDecimal("7.00"), pedido);

        assertThatThrownBy(() -> {
            facturaRepository.save(segundaFactura);
            facturaRepository.flush(); // aqui se dispara la violacion de la restriccion unique
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Eliminar una Factura no elimina el Pedido asociado (no hay cascade)")
    void eliminarFactura_noAfectaElPedido() {
        Pedido pedido = crearPedidoBase();
        Factura factura = facturaRepository.save(
                new Factura("FAC-0007", LocalDateTime.now(), new BigDecimal("9.99"), pedido));
        entityManager.flush();

        facturaRepository.delete(factura);
        entityManager.flush();

        assertThat(facturaRepository.count()).isEqualTo(0);
        assertThat(pedidoRepository.findById(pedido.getId())).isPresent();
    }
}
