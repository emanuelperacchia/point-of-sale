package com.pos.system.repository;

import com.pos.system.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findByActiveTrueAndNameContainingIgnoreCase(String name);

    List<Client> findByActiveTrueAndDocumentNumberContainingIgnoreCase(String documentNumber);

    List<Client> findByActiveTrueAndEmailContainingIgnoreCase(String email);
}
