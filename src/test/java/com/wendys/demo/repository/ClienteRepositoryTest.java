package com.wendys.demo.repository;

import com.wendys.demo.entity.Cliente;
import com.wendys.demo.entity.Pedido;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas SOLO para la entidad Cliente y su relacion 1:N con Pedido
 * (@OneToMany / mappedBy / CascadeType.ALL / orphanRemoval).
 * No involucra DetallePedido ni Producto.
 */
@DataJpaTest
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Guardar un Cliente genera un id y persiste sus campos")
    void guardarCliente_generaIdYPersisteCampos() {
        Cliente cliente = new Cliente("Juan Perez", "juan.perez@correo.com");

        Cliente guardado = clienteRepository.save(cliente);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getNombre()).isEqualTo("Juan Perez");
        assertThat(guardado.getCorreo()).isEqualTo("juan.perez@correo.com");
    }

    @Test
    @DisplayName("Un Cliente nuevo empieza con la lista de pedidos vacia")
    void clienteNuevo_sinPedidos() {
        Cliente cliente = clienteRepository.save(new Cliente("Ana Lopez", "ana@correo.com"));

        assertThat(cliente.getPedidos()).isEmpty();
    }

    @Test
    @DisplayName("agregarPedido sincroniza ambos lados y CascadeType.ALL guarda el Pedido solo")
    void agregarPedido_cascadaGuardaElPedidoAutomaticamente() {
        Cliente cliente = new Cliente("Mario Diaz", "mario@correo.com");
        Pedido pedido = new Pedido(LocalDateTime.now(), "En restaurante");

        cliente.agregarPedido(pedido); // sincroniza cliente <-> pedido

        clienteRepository.save(cliente); // UN solo save, gracias a CascadeType.ALL
        entityManager.flush();
        entityManager.clear(); // forzamos releer de la base de datos, no de memoria

        Cliente recargado = clienteRepository.findById(cliente.getId()).orElseThrow();
        assertThat(recargado.getPedidos()).hasSize(1);
        assertThat(recargado.getPedidos().get(0).getTipoEntrega()).isEqualTo("En restaurante");

        // Confirmamos tambien que el Pedido quedo realmente guardado en su propia tabla
        assertThat(pedidoRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("orphanRemoval: quitar un Pedido de la lista del Cliente lo borra de la base de datos")
    void quitarPedidoDeLaLista_loEliminaPorOrphanRemoval() {
        Cliente cliente = new Cliente("Sofia Ruiz", "sofia@correo.com");
        Pedido pedido = new Pedido(LocalDateTime.now(), "Delivery");
        cliente.agregarPedido(pedido);
        clienteRepository.save(cliente);
        entityManager.flush();

        assertThat(pedidoRepository.count()).isEqualTo(1);

        cliente.getPedidos().remove(pedido);
        clienteRepository.save(cliente);
        entityManager.flush();

        assertThat(pedidoRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Borrar un Cliente borra en cascada sus Pedidos (CascadeType.ALL / REMOVE)")
    void borrarCliente_borraSusPedidosEnCascada() {
        Cliente cliente = new Cliente("Pedro Gomez", "pedro@correo.com");
        cliente.agregarPedido(new Pedido(LocalDateTime.now(), "Para llevar"));
        cliente.agregarPedido(new Pedido(LocalDateTime.now(), "En restaurante"));
        clienteRepository.save(cliente);
        entityManager.flush();

        assertThat(pedidoRepository.count()).isEqualTo(2);

        clienteRepository.delete(cliente);
        entityManager.flush();

        assertThat(pedidoRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("findById no encuentra un Cliente que no existe")
    void findById_clienteInexistente_devuelveVacio() {
        Optional<Cliente> resultado = clienteRepository.findById(999L);

        assertThat(resultado).isEmpty();
    }
}
