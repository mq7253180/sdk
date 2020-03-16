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
@Entity(name = "s_permission")
public class Permission {
	@Id
	@Column(name="id")
	private Long id;
	@Column(name="name")
	private String name;
	@Column(name="des")
	private String des;
}
