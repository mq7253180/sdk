package com.quincy.auth.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.quincy.auth.entity.ClientSystem;

@Repository
public interface ClientSystemRepository extends JpaRepository<ClientSystem, Long>, JpaSpecificationExecutor<ClientSystem> {
	public ClientSystem findByClientId(String clientId);
}