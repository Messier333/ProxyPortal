package com.messier333.proxyportal.user.repository;

import com.messier333.proxyportal.user.entity.User;
import com.messier333.proxyportal.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findAllByOrderByIdAsc();
    long countByRole(Role role);
}
