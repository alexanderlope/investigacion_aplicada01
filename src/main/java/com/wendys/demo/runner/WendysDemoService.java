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
    private final FacturaRepository facturaRepository;

    public WendysDemoService(ClienteRepository clienteRepository,
                              PedidoRepository pedidoRepository,
                              ProductoRepository productoRepository,
                              DetallePedidoRepository detallePedidoRepository,
                              FacturaRepository facturaRepository) {
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.facturaRepository = facturaRepository;
    }

    /**
     * PASO 1: crea el menu (Producto) y un Cliente con un Pedido que
     * ya trae sus DetallePedido armados.
     *
     * Gracias a CascadeType.ALL en Cliente->Pedido y en Pedido->Detalle,
     * basta con guardar el Cliente UNA sola vez: Hibernate se encarga
     * de propagar el INSERT hacia Pedido y hacia DetallePedido.
     */
    /**
     * Pequeño contenedor para devolver ambos ids generados en la demo.
     */
    public record IdsDemo(Long clienteId, Long pedidoId) {}

    @Transactional
    public IdsDemo crearDatosDemo() {
        // --- Menu de Wendy's ---
        Producto baconator = productoRepository.save(new Producto("Baconator", new BigDecimal("6.99")));
        Producto papas = productoRepository.save(new Producto("Papas rizadas", new BigDecimal("2.49")));
        Producto frosty = productoRepository.save(new Producto("Frosty de chocolate", new BigDecimal("1.99")));

        // --- Cliente ---
        Cliente cliente = new Cliente("Juan Perez", "juan.perez@correo.com");

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

        return new IdsDemo(cliente.getId(), pedido.getId());
    }

    /**
     * PASO 2: demuestra FetchType.EAGER.
     * Pedido.cliente es EAGER -> al leer el Pedido, el Cliente viene
     * cargado de inmediato, en la MISMA consulta (o en una automatica),
     * sin necesitar una transaccion abierta despues.
     */
    public void demostrarFetchEager(Long pedidoId) {
        System.out.println("\n--- DEMOSTRACION FetchType.EAGER (Pedido -> Cliente) ---");
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        // Ya estamos FUERA del metodo transaccional (este metodo no lo es)
        // y aun asi esto funciona sin error, porque es EAGER:
        System.out.println("Pedido #" + pedido.getId() + " pertenece a: " + pedido.getCliente().getNombre());
    }

    /**
     * PASO 3: demuestra FetchType.LAZY.
     * Devolvemos el Cliente ya "tocado" dentro de una transaccion que
     * se cierra al terminar el metodo (@Transactional). El objeto queda
     * DETACHED. Si luego, fuera de esta transaccion, intentamos acceder
     * a la coleccion "pedidos" (LAZY), Hibernate lanza
     * LazyInitializationException porque no hay sesion activa.
     */
    @Transactional
    public Cliente obtenerClienteDetached(Long clienteId) {
        return clienteRepository.findById(clienteId).orElseThrow();
    }

    /**
     * Version correcta: accede a la coleccion LAZY DENTRO de la
     * transaccion, asi que funciona sin problema.
     */
    @Transactional
    public void demostrarFetchLazyCorrecto(Long clienteId) {
        System.out.println("\n--- DEMOSTRACION FetchType.LAZY (dentro de transaccion, SI funciona) ---");
        Cliente cliente = clienteRepository.findById(clienteId).orElseThrow();
        System.out.println(cliente.getNombre() + " tiene " + cliente.getPedidos().size() + " pedido(s)");
    }

    /**
     * PASO 4 (nuevo): demuestra @OneToOne.
     * Factura es el lado DUEÑO (tiene @JoinColumn pedido_id, unique = true).
     * Pedido es el lado NO dueño (mappedBy = "pedido").
     *
     * A proposito NO usamos cascade aqui: emitir una factura es una accion
     * de negocio independiente, no algo que deba dispararse solo porque
     * se guarda o se borra un Pedido.
     */
    @Transactional
    public Long emitirFactura(Long pedidoId, String numeroFactura) {
        System.out.println("\n--- DEMOSTRACION @OneToOne (Pedido <-> Factura) ---");
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();

        BigDecimal total = pedido.getDetalles().stream()
                .map(DetallePedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Factura factura = new Factura(numeroFactura, LocalDateTime.now(), total, pedido);
        facturaRepository.save(factura);

        System.out.println(">>> Factura " + numeroFactura + " emitida para el Pedido #" + pedido.getId()
                + " por un total de $" + total);

        return factura.getId();
    }

    /**
     * Muestra ambos lados de la relacion 1:1:
     *  - Desde Factura (dueña, EAGER): factura.getPedido() ya viene cargado.
     *  - Desde Pedido (no dueño, LAZY): pedido.getFactura() se carga bajo demanda,
     *    por eso este metodo debe ejecutarse DENTRO de una transaccion.
     */
    @Transactional
    public void demostrarRelacionUnoAUno(Long facturaId, Long pedidoId) {
        Factura factura = facturaRepository.findById(facturaId).orElseThrow();
        System.out.println("Desde Factura (EAGER) -> Pedido: #" + factura.getPedido().getId()
                + " (" + factura.getPedido().getTipoEntrega() + ")");

        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        System.out.println("Desde Pedido (LAZY) -> Factura: " + pedido.getFactura());
    }

    /**
     * Borra la Factura antes de borrar el Pedido en la demo de cascada.
     * Como Factura NO tiene cascade desde Pedido y su columna pedido_id
     * es NOT NULL, si intentaramos borrar el Pedido con la Factura todavia
     * apuntando a el, la base de datos rechazaria el borrado por la
     * restriccion de clave foranea. Por eso aqui se borra explicitamente.
     */
    @Transactional
    public void eliminarFactura(Long facturaId) {
        facturaRepository.deleteById(facturaId);
        facturaRepository.flush();
        System.out.println(">>> Factura eliminada explicitamente (no hay cascade Pedido -> Factura)");
    }

    /**
     * PASO 5: demuestra CascadeType.REMOVE + orphanRemoval.
     * Al borrar el Pedido, sus DetallePedido se borran en cascada,
     * pero los Producto (Baconator, Papas, Frosty) NO se tocan,
     * porque la cascada solo va Pedido -> DetallePedido, nunca hacia Producto.
     */
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

    /**
     * PASO 5 (opcional): demuestra orphanRemoval quitando UN detalle
     * de la lista de un pedido, sin borrar el pedido completo.
     */
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
