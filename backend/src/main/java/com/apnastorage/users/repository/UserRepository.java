package com.apnastorage.users.repository;

import com.apnastorage.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    boolean existsByMob(String mob);

    Optional<User> findByUsername(String username);
}
