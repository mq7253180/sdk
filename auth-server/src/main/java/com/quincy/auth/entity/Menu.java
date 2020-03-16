package com.quincy.auth.entity;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Data;

@Data
@DynamicInsert
@DynamicUpdate
@EntityListeners({AuditingEntityListener.class})
@Entity(name = "s_menu")
public class Menu implements Serializable {
	private static final long serialVersionUID = 6829594433533198471L;
	@Id
	@Column(name="id")
	private Long id;
	@Column(name="p_id")
	private Long pId;
	@Column(name="name")
	private String name;
	@Column(name="uri")
	private String uri;
	@Column(name="icon")
	private String icon;
	@Transient
	private List<Menu> children;
}
