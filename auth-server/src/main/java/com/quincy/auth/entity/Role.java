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
@Entity(name = "s_role")
public class Role {
	@Id
	@Column(name="id")
	private Long id;
//	@ManyToMany(cascade = CascadeType.ALL)
//	@JoinTable(name = "s_permission_role_rel", joinColumns = {@JoinColumn(name = "role_id")}, inverseJoinColumns = {@JoinColumn(name = "permission_id")})
//	private Set<Permission> permissions;
	@Column(name="name")
	private String name;
}