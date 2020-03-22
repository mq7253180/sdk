package com.quincy.auth.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
//	@JoinTable(name = "s_role_permission_rel", joinColumns = {@JoinColumn(name = "role_id")}, inverseJoinColumns = {@JoinColumn(name = "permission_id")})
//	private Set<Permission> permissions;
	@Column(name="name")
	private String name;
}