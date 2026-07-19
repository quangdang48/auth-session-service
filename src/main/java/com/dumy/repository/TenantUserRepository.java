package com.dumy.repository;

import com.dumy.entity.TenantUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantUserRepository extends JpaRepository<TenantUser, String> {

    Optional<TenantUser> findByTenant_IdAndUser_Id(String tenantId, String userId);

    List<TenantUser> findByTenant_Id(String tenantId);

    List<TenantUser> findByUser_Id(String userId);
}
