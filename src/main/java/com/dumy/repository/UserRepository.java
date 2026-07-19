package com.dumy.repository;

import com.dumy.entity.EUserType;
import com.dumy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsernameAndUserType(String username, EUserType userType);

    boolean existsByUsername(String username);
}
