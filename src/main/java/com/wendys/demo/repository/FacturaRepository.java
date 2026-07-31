package com.wendys.demo.repository;

import com.wendys.demo.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {
    // Aquí puedes agregar consultas personalizadas luego, por ejemplo:
    // Optional<Factura> findByNumeroFactura(String numeroFactura);
}
