package com.jsrush.sims.dao;

import java.util.List;
import java.util.Map;

import com.jsrush.sims.entity.Course;

public interface CourseRepositoryCustom {

	/**
	 * 查询列表
	 * 
	 * @param condition
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	Map<String, Object> findPageList(Course condition, Integer pageNo, Integer pageSize);
	
	/**
	 * 查询课程列表
	 * @param ecId
	 * @return
	 */
	List<Map<Long, String>> findListByEcId(Long ecId);
	
}
