package com.authservice.repository;

import com.authservice.model.ERole;
import com.authservice.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(ERole name);

    boolean existsByName(ERole name);
}