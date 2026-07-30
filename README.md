# Wendy's JPA Demo — Tema 4: Relaciones entre entidades

Proyecto Spring Boot + JPA/Hibernate que demuestra `@OneToOne`, `@OneToMany`,
`@ManyToOne`, `@ManyToMany` (conceptualmente), `@JoinColumn`, `mappedBy`,
`CascadeType` y `FetchType` (LAZY vs EAGER), usando una tienda de comida
rápida tipo Wendy's como caso de uso.

## Modelo de entidades

```
Cliente (1) ──── (N) Pedido (1) ──── (N) DetallePedido (N) ──── (1) Producto
```

- **Cliente**: quien hace el pedido (ej. "Juan Perez").
- **Pedido**: una orden concreta (ej. "Para llevar", con fecha).
- **DetallePedido**: cada línea del pedido (ej. "2 x Baconator"), con
  cantidad y precio unitario. Es la entidad que resuelve la relación
  muchos-a-muchos entre Pedido y Producto de forma "enriquecida".
- **Producto**: ítems del menú (Baconator, Papas rizadas, Frosty...).

| Relación | Tipo | Dueño (`@JoinColumn`) | Lado inverso (`mappedBy`) |
|---|---|---|---|
| Cliente ↔ Pedido | 1:N | Pedido (`cliente_id`) | Cliente.pedidos |
| Pedido ↔ DetallePedido | 1:N | DetallePedido (`pedido_id`) | Pedido.detalles |
| DetallePedido ↔ Producto | N:1 | DetallePedido (`producto_id`) | Producto.detalles |

## Requisitos

- Java 17+
- Maven 3.8+ (o usa el `mvnw` si lo agregas con `mvn -N io.takari:maven:wrapper`)
- No necesitas instalar base de datos: usa **H2 en memoria**.

## Cómo ejecutarlo

```bash
mvn spring-boot:run
```

Al arrancar, `DemoRunner` ejecuta automáticamente toda la demostración y
verás en consola (con SQL incluido, gracias a `show-sql=true`):

1. **Cascada al guardar**: un solo `clienteRepository.save(cliente)` guarda
   también el Pedido y los 3 DetallePedido (`CascadeType.ALL`).
2. **FetchType.EAGER**: se lee un `Pedido` y su `Cliente` aparece de
   inmediato, sin necesitar sesión abierta.
3. **FetchType.LAZY correcto**: se accede a `cliente.getPedidos()`
   *dentro* de una transacción — funciona sin problema.
4. **FetchType.LAZY incorrecto**: se intenta acceder a `getPedidos()`
   *fuera* de la transacción → se captura una `LazyInitializationException`,
   mostrando por qué LAZY puede fallar si no se maneja bien.
5. **orphanRemoval**: se quita un `DetallePedido` de la lista del `Pedido`
   y se guarda → ese detalle desaparece de la base de datos aunque el
   pedido siga existiendo.
6. **CascadeType.REMOVE**: se borra el `Pedido` completo → sus
   `DetallePedido` se borran en cascada, pero los `Producto` (Baconator,
   Papas, Frosty) permanecen intactos, porque la cascada no llega hasta
   Producto.

## Consola H2 (opcional)

Con la app corriendo, entra a `http://localhost:8080/h2-console` con:
- JDBC URL: `jdbc:h2:mem:wendysdb`
- Usuario: `sa`
- Contraseña: (vacía)

## Estructura del proyecto

```
src/main/java/com/wendys/demo/
├── WendysDemoApplication.java
├── entity/
│   ├── Cliente.java
│   ├── Pedido.java
│   ├── DetallePedido.java
│   └── Producto.java
├── repository/
│   ├── ClienteRepository.java
│   ├── PedidoRepository.java
│   ├── DetallePedidoRepository.java
│   └── ProductoRepository.java
└── runner/
    ├── WendysDemoService.java   (lógica transaccional de la demo)
    └── DemoRunner.java          (orquesta y ejecuta al arrancar)
```
