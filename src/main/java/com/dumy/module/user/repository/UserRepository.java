package com.dumy.module.user.repository;

import com.dumy.module.user.entity.EUserType;
import com.dumy.module.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsernameAndUserType(String username, EUserType userType);

    boolean existsByUsername(String username);
}
