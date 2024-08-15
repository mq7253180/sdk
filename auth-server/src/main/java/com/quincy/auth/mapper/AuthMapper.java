package com.quincy.auth.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.quincy.auth.entity.Permission;
import com.quincy.auth.entity.Role;
import com.quincy.auth.o.Menu;

@Repository
public interface AuthMapper {
	public List<Role> findRolesByUserId(@Param("userId")Long userId);
	public List<Permission> findPermissionsByUserId(@Param("userId")Long userId);
	public List<Menu> findMenusByUserId(@Param("userId")Long userId);
	public List<Role> findRolesByUserIdAndEnterpriseId(@Param("userId")Long userId, @Param("enterpriseId")Long enterpriseId);
	public List<Permission> findPermissionsByUserIdAndEnterpriseId(@Param("userId")Long userId, @Param("enterpriseId")Long enterpriseId);
	public List<Menu> findMenusByUserIdAndEnterpriseId(@Param("userId")Long userId, @Param("enterpriseId")Long enterpriseId);
}