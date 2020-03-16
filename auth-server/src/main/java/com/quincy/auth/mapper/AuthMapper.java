package com.quincy.auth.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.quincy.auth.entity.Menu;
import com.quincy.auth.entity.Permission;
import com.quincy.auth.entity.Role;

@Repository
public interface AuthMapper {
	public List<Role> findRolesByUserId(@Param("userId")Long userId);
	public List<Permission> findPermissionsByUserId(@Param("userId")Long userId);
	public List<Menu> findMenusByUserId(@Param("userId")Long userId);
}
