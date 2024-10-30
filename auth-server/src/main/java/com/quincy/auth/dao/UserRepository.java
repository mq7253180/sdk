package com.quincy.auth.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.quincy.auth.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {
//	public UserEntity findByUsernameOrEmailOrMobilePhone(String username, String email, String mobilePhone);
}
