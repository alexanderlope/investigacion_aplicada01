# Wendy's JPA Demo — Tema 4: Relaciones entre entidades

Proyecto Spring Boot + JPA/Hibernate que demuestra `@OneToOne`, `@OneToMany`,
`@ManyToOne`, `@ManyToMany` (conceptualmente), `@JoinColumn`, `mappedBy`,
`CascadeType` y `FetchType` (LAZY vs EAGER), usando una tienda de comida
rápida tipo Wendy's como caso de uso.

## Modelo de entidades

```
Cliente (1) ──── (N) Pedido (1) ──── (N) DetallePedido (N) ──── (1) Producto
                       │
                       │ (1:1)
                       ▼
                    Factura
```

- **Cliente**: quien hace el pedido (ej. "Juan Perez").
- **Pedido**: una orden concreta (ej. "Para llevar", con fecha).
- **DetallePedido**: cada línea del pedido (ej. "2 x Baconator"), con
  cantidad y precio unitario. Es la entidad que resuelve la relación
  muchos-a-muchos entre Pedido y Producto de forma "enriquecida".
- **Producto**: ítems del menú (Baconator, Papas rizadas, Frosty...).
- **Factura**: el comprobante de cobro de un Pedido (número, fecha de
  emisión, total). Cada Pedido tiene como máximo una Factura.

| Relación | Tipo | Dueño (`@JoinColumn`) | Lado inverso (`mappedBy`) |
|---|---|---|---|
| Cliente ↔ Pedido | 1:N | Pedido (`cliente_id`) | Cliente.pedidos |
| Pedido ↔ DetallePedido | 1:N | DetallePedido (`pedido_id`) | Pedido.detalles |
| DetallePedido ↔ Producto | N:1 | DetallePedido (`producto_id`) | Producto.detalles |
| Pedido ↔ Factura | 1:1 | Factura (`pedido_id`, unique) | Pedido.factura |

Nota sobre `Factura`: **no** tiene `cascade` desde `Pedido`, a propósito.
Emitir o borrar una factura es una acción de negocio independiente, no
algo que deba ocurrir automáticamente solo porque se guarda o se borra
un Pedido. Por eso, en la demo, la Factura se elimina explícitamente
*antes* de borrar el Pedido (si no, la base de datos rechazaría el
borrado por la restricción de clave foránea).

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
6. **@OneToOne**: se emite una `Factura` para el `Pedido` y se muestra la
   relación desde ambos lados — desde `Factura` (dueña, EAGER, ve el
   Pedido de inmediato) y desde `Pedido` (no dueño, LAZY, `mappedBy`).
7. **Sin cascade entre Pedido y Factura**: la Factura se borra
   explícitamente antes de borrar el Pedido (no hay `cascade` configurado
   entre ellos a propósito).
8. **CascadeType.REMOVE**: se borra el `Pedido` completo → sus
   `DetallePedido` se borran en cascada, pero los `Producto` (Baconator,
   Papas, Frosty) permanecen intactos, porque la cascada no llega hasta
   Producto.

## Consola H2 (opcional)

Con la app corriendo, entra a `http://localhost:8080/h2-console` con:
- JDBC URL: `jdbc:h2:mem:wendysdb`
- Usuario: `sa`
- Contraseña: (vacía)

## Cómo correr los tests

Se incluyó una clase de test **por cada entidad**, independiente entre sí,
para poder comprobar de forma aislada que cada una funciona:

```bash
mvn test
```

O una sola clase a la vez:

```bash
mvn test -Dtest=ProductoRepositoryTest
mvn test -Dtest=ClienteRepositoryTest
mvn test -Dtest=PedidoRepositoryTest
mvn test -Dtest=DetallePedidoRepositoryTest
mvn test -Dtest=FacturaRepositoryTest
```

Cada test usa `@DataJpaTest` (levanta solo la capa JPA + una base H2 en
memoria de prueba, mucho más rápido que levantar toda la aplicación) y
prueba, por entidad:

| Clase de test | Qué comprueba |
|---|---|
| `ProductoRepositoryTest` | CRUD básico: guardar, buscar por id, actualizar, borrar, listar todos |
| `ClienteRepositoryTest` | CRUD básico + cascada `CascadeType.ALL` hacia Pedido + `orphanRemoval` al quitar un Pedido de la lista + borrado en cascada |
| `PedidoRepositoryTest` | Relación `@ManyToOne` con Cliente + cascada hacia DetallePedido + `orphanRemoval` + que borrar un Pedido no borre sus Productos |
| `DetallePedidoRepositoryTest` | Sus dos relaciones `@ManyToOne` (Pedido y Producto), cálculo de `getSubtotal()`, `FetchType.EAGER`, y que un mismo Producto pueda estar en varios detalles |
| `FacturaRepositoryTest` | Relación `@OneToOne` con Pedido desde ambos lados, `@PrePersist`, `FetchType.EAGER`, la restricción `unique` (no permite 2 facturas para el mismo pedido), y que borrar la Factura no afecte al Pedido |

Cada archivo prueba **solo** lo que le corresponde a esa entidad (aunque
para crear un Pedido necesites un Cliente, por ejemplo, ese Cliente se
crea como dato de apoyo mínimo, no es lo que se está evaluando).



```
src/main/java/com/wendys/demo/
├── WendysDemoApplication.java
├── entity/
│   ├── Cliente.java
│   ├── Pedido.java
│   ├── DetallePedido.java
│   ├── Producto.java
│   └── Factura.java
├── repository/
│   ├── ClienteRepository.java
│   ├── PedidoRepository.java
│   ├── DetallePedidoRepository.java
│   ├── ProductoRepository.java
│   └── FacturaRepository.java
└── runner/
    ├── WendysDemoService.java   (lógica transaccional de la demo)
    └── DemoRunner.java          (orquesta y ejecuta al arrancar)

src/test/java/com/wendys/demo/repository/
├── ProductoRepositoryTest.java
├── ClienteRepositoryTest.java
├── PedidoRepositoryTest.java
├── DetallePedidoRepositoryTest.java
└── FacturaRepositoryTest.java
```
