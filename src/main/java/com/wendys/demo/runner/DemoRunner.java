package com.wendys.demo.runner;

import com.wendys.demo.entity.Cliente;
import org.hibernate.LazyInitializationException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoRunner implements CommandLineRunner {

    private final WendysDemoService service;

    public DemoRunner(WendysDemoService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        System.out.println("\n==================================================");
        System.out.println(" DEMO: Relaciones JPA con tematica Wendy's");
        System.out.println("==================================================");

        // 1) Crear Cliente -> Pedido -> DetallePedido -> Producto de un solo save()
        WendysDemoService.IdsDemo ids = service.crearDatosDemo();
        Long clienteId = ids.clienteId();
        Long pedidoId = ids.pedidoId();

        // 2) FetchType.EAGER: funciona sin transaccion abierta
        service.demostrarFetchEager(pedidoId);

        // 3) FetchType.LAZY: version correcta (dentro de transaccion)
        service.demostrarFetchLazyCorrecto(clienteId);

        // 4) FetchType.LAZY: version incorrecta (fuera de transaccion) -> excepcion esperada
        System.out.println("\n--- DEMOSTRACION FetchType.LAZY (fuera de transaccion, FALLA) ---");
        try {
            Cliente clienteDetached = service.obtenerClienteDetached(clienteId);
            // Esta linea intenta inicializar la coleccion LAZY sin sesion activa:
            System.out.println("Pedidos: " + clienteDetached.getPedidos().size());
        } catch (LazyInitializationException e) {
            System.out.println("EXCEPCION ESPERADA -> LazyInitializationException:");
            System.out.println("   " + e.getMessage());
            System.out.println("Esto pasa porque 'pedidos' es LAZY y ya no hay sesion de Hibernate abierta.");
        }

        // 5) orphanRemoval: quitar un detalle de la lista sin borrar el pedido
        service.demostrarOrphanRemoval(pedidoId);

        // 6) @OneToOne: emitir una Factura para el Pedido y ver ambos lados de la relacion
        Long facturaId = service.emitirFactura(pedidoId, "FAC-0001");
        service.demostrarRelacionUnoAUno(facturaId, pedidoId);

        // 7) Borramos la Factura antes de borrar el Pedido (no hay cascade entre ellos)
        service.eliminarFactura(facturaId);

        // 8) CascadeType.REMOVE: borrar el pedido completo
        service.demostrarCascadaRemove(pedidoId);

        System.out.println("\n==================================================");
        System.out.println(" FIN DE LA DEMOSTRACION");
        System.out.println("==================================================\n");
    }
}
