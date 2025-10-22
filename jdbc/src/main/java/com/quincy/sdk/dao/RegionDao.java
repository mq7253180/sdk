package com.quincy.sdk.dao;

import java.util.List;

import com.quincy.sdk.annotation.jdbc.ExecuteQuery;
import com.quincy.sdk.annotation.jdbc.JDBCDao;
import com.quincy.sdk.entity.Region;

@JDBCDao
public interface RegionDao {
	@ExecuteQuery(sql = "SELECT * FROM b_region", returnItemType = Region.class)
	public List<Region> find();
	@ExecuteQuery(sql = "SELECT * FROM b_region WHERE parent_id=?", returnItemType = Region.class)
	public List<Region> findByParentId(Long parentId);
}