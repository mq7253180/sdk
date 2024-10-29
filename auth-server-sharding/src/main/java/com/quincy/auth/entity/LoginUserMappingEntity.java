package com.quincy.auth.entity;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@DynamicInsert
@DynamicUpdate
@EntityListeners({AuditingEntityListener.class})
@Entity(name = "b_login_user_mapping")
public class LoginUserMappingEntity {
	@Id
	@Column(name="id")
	private Long id;
	@Column(name="login_name")
	private String loginName;
	@Id
	@Column(name="user_id")
	private Long userId;
}