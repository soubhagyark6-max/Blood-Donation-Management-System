package com.lifelink.repository;

import com.lifelink.model.Role;
import com.lifelink.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRoleIn(java.util.Collection<Role> roles);

    long countByRole(Role role);
}
