package com.quincy.auth.entity;

import java.util.Date;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@DynamicInsert
@DynamicUpdate
@EntityListeners({AuditingEntityListener.class})
@Entity(name = "b_user")
public class UserEntity {
	@Id
	@Column(name="id")
	private Long id;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
	@CreatedDate
	@Column(name="creation_time")
	private Date creationTime;
//	@ManyToMany(cascade = CascadeType.ALL)
//	@JoinTable(name = "s_role_user_rel", joinColumns = {@JoinColumn(name = "user_id")}, inverseJoinColumns = {@JoinColumn(name = "role_id")})
//	private Set<Role> roles;
	@Column(name="username")
	private String username;
	@Column(name="name")
	private String name;
	@Column(name="gender")
	private Byte gender;
	@Column(name="password")
	private String password;
	@Column(name="email")
	private String email;
	@Column(name="mobile_phone")
	private String mobilePhone;
	@Column(name="avatar")
	private String avatar;
	@Column(name="jsessionid_pc_browser")
	private String jsessionidPcBrowser;
	@Column(name="jsessionid_mobile_browser")
	private String jsessionidMobileBrowser;
	@Column(name="jsessionid_app")
	private String jsessionidApp;
}