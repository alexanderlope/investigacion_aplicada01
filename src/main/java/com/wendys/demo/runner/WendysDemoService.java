package com.wendys.demo.runner;

import com.wendys.demo.entity.*;
import com.wendys.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class WendysDemoService {

    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final DetallePedidoRepository detallePedidoRepository;

    public WendysDemoService(ClienteRepository clienteRepository,
                              PedidoRepository pedidoRepository,
                              ProductoRepository productoRepository,
                              DetallePedidoRepository detallePedidoRepository) {
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
    }

    public record IdsDemo(Long clienteId, Long pedidoId) {}

    @Transactional
    public IdsDemo crearDatosDemo() {
        // --- Menu de Wendy's ---
        Producto baconator = productoRepository.save(new Producto("Baconator", new BigDecimal("6.99")));
        Producto papas = productoRepository.save(new Producto("Papas rizadas", new BigDecimal("2.49")));
        Producto frosty = productoRepository.save(new Producto("Frosty de chocolate", new BigDecimal("1.99")));

        // --- Cliente ---
        Cliente cliente = new Cliente("Juan Perez", "juan.perez@gmail.com");

        // --- Pedido ---
        Pedido pedido = new Pedido(LocalDateTime.now(), "Para llevar");
        cliente.agregarPedido(pedido); // sincroniza ambos lados de la relacion

        // --- Detalles del pedido ---
        pedido.agregarDetalle(new DetallePedido(2, baconator.getPrecio(), baconator));
        pedido.agregarDetalle(new DetallePedido(1, papas.getPrecio(), papas));
        pedido.agregarDetalle(new DetallePedido(3, frosty.getPrecio(), frosty));

        // Un solo save(), gracias a CascadeType.ALL se guarda TODO el arbol:
        // Cliente -> Pedido -> DetallePedido
        clienteRepository.save(cliente);

        System.out.println(">>> Cliente, Pedido y " + pedido.getDetalles().size()
                + " DetallePedido guardados con UN SOLO save() gracias a CascadeType.ALL");
        System.out.println(">>> Pedido Wendy's #" + pedido.getId() + " | estado: "
                + pedido.getEstado() + " | total: $" + pedido.getTotal());

        return new IdsDemo(cliente.getId(), pedido.getId());
    }

    public void demostrarFetchEager(Long pedidoId) {
        System.out.println("\n--- DEMOSTRACION FetchType.EAGER (Pedido -> Cliente) ---");
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        // Ya estamos FUERA del metodo transaccional (este metodo no lo es)
        // y aun asi esto funciona sin error, porque es EAGER:
        System.out.println("Pedido #" + pedido.getId() + " pertenece a: " + pedido.getCliente().getNombre());
        System.out.println("Estado: " + pedido.getEstado());
        System.out.println("El total no se consulta aqui porque recorre 'detalles', una coleccion LAZY.");
    }

    @Transactional
    public Cliente obtenerClienteDetached(Long clienteId) {
        return clienteRepository.findById(clienteId).orElseThrow();
    }

    @Transactional
    public void demostrarFetchLazyCorrecto(Long clienteId) {
        System.out.println("\n--- DEMOSTRACION FetchType.LAZY (dentro de transaccion, SI funciona) ---");
        Cliente cliente = clienteRepository.findById(clienteId).orElseThrow();
        System.out.println(cliente.getNombre() + " tiene " + cliente.getPedidos().size() + " pedido(s)");
    }

    @Transactional
    public void demostrarCascadaRemove(Long pedidoId) {
        System.out.println("\n--- DEMOSTRACION CascadeType.ALL / REMOVE (borrar Pedido) ---");
        long detallesAntes = detallePedidoRepository.count();
        long productosAntes = productoRepository.count();

        pedidoRepository.deleteById(pedidoId);
        pedidoRepository.flush();

        long detallesDespues = detallePedidoRepository.count();
        long productosDespues = productoRepository.count();

        System.out.println("DetallePedido antes de borrar: " + detallesAntes + " -> despues: " + detallesDespues
                + " (se borraron en cascada)");
        System.out.println("Producto antes de borrar: " + productosAntes + " -> despues: " + productosDespues
                + " (NO cambia: la cascada no llega hasta Producto)");
    }

    @Transactional
    public void demostrarOrphanRemoval(Long pedidoId) {
        System.out.println("\n--- DEMOSTRACION orphanRemoval = true ---");
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        long detallesAntes = pedido.getDetalles().size();

        DetallePedido primero = pedido.getDetalles().get(0);
        System.out.println("Quitando de la lista: " + primero);
        pedido.quitarDetalle(primero);
        pedidoRepository.save(pedido);
        pedidoRepository.flush();

        long detallesDespues = pedido.getDetalles().size();
        System.out.println("Detalles en el pedido antes: " + detallesAntes + " -> despues: " + detallesDespues);
        System.out.println("El detalle huerfano fue eliminado automaticamente de la base de datos.");
    }
}
