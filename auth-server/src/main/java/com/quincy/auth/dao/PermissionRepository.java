package com.quincy.auth.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.quincy.auth.entity.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long>, JpaSpecificationExecutor<Permission> {
	/*@Query("FROM com.hce.ducati.auth.entity.Permission p INNER JOIN PermissionRoleRel r1 ON p.id=r1.permissionId INNER JOIN RoleUserRel r2 ON r1.roleId=r2.roleId WHERE r2.userId=:userId")
	public List<Permission> findByUserId(@Param("userId")Long userId);*/
}
