package com.quincy.auth.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.quincy.auth.entity.LoginUserMappingEntity;

@Repository
public interface LoginUserMappingRepository extends JpaRepository<LoginUserMappingEntity, Long>, JpaSpecificationExecutor<LoginUserMappingEntity> {
	public LoginUserMappingEntity findByLoginName(String loginName);
	public int deleteByLoginName(String loginName);
}