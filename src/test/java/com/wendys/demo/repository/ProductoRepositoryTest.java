package com.wendys.demo.repository;

import com.wendys.demo.entity.Producto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas SOLO para la entidad Producto.
 * No involucra a Cliente, Pedido ni DetallePedido: aqui se comprueba
 * que Producto por si sola funciona bien (mapeo basico, CRUD).
 */
@DataJpaTest
class ProductoRepositoryTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Test
    @DisplayName("Guardar un Producto genera un id y persiste sus campos")
    void guardarProducto_generaIdYPersisteCampos() {
        Producto baconator = new Producto("Baconator", new BigDecimal("6.99"));

        Producto guardado = productoRepository.save(baconator);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getNombre()).isEqualTo("Baconator");
        assertThat(guardado.getPrecio()).isEqualByComparingTo("6.99");
    }

    @Test
    @DisplayName("findById encuentra un Producto previamente guardado")
    void findById_encuentraProductoGuardado() {
        Producto papas = productoRepository.save(new Producto("Papas rizadas", new BigDecimal("2.49")));

        Optional<Producto> encontrado = productoRepository.findById(papas.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNombre()).isEqualTo("Papas rizadas");
    }

    @Test
    @DisplayName("Un Producto nuevo empieza con la lista de detalles vacia")
    void productoNuevo_sinDetallesAsociados() {
        Producto frosty = productoRepository.save(new Producto("Frosty de chocolate", new BigDecimal("1.99")));

        assertThat(frosty.getDetalles()).isEmpty();
    }

    @Test
    @DisplayName("Actualizar el precio de un Producto se refleja al recargarlo")
    void actualizarProducto_reflejaCambios() {
        Producto producto = productoRepository.save(new Producto("Nuggets (6pz)", new BigDecimal("4.50")));

        producto.setPrecio(new BigDecimal("4.99"));
        productoRepository.save(producto);
        productoRepository.flush();

        Producto recargado = productoRepository.findById(producto.getId()).orElseThrow();
        assertThat(recargado.getPrecio()).isEqualByComparingTo("4.99");
    }

    @Test
    @DisplayName("Eliminar un Producto sin relaciones funciona sin error")
    void eliminarProducto_sinRelaciones_funciona() {
        Producto producto = productoRepository.save(new Producto("Refresco", new BigDecimal("1.50")));
        Long id = producto.getId();

        productoRepository.deleteById(id);
        productoRepository.flush();

        assertThat(productoRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("findAll devuelve todos los productos guardados")
    void findAll_devuelveTodosLosProductos() {
        productoRepository.save(new Producto("Baconator", new BigDecimal("6.99")));
        productoRepository.save(new Producto("Papas rizadas", new BigDecimal("2.49")));

        List<Producto> productos = productoRepository.findAll();

        assertThat(productos).hasSize(2);
    }
}
