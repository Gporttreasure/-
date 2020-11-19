package com.education.model.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 科目信息表
 */
@TableName("subject_info")
public class SubjectInfo extends BaseEntity<SubjectInfo> {

	private String name;
	@TableField("school_type")
	private Integer schoolType;
	@TableField("grade_info_id")
	private Integer gradeInfoId;


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getSchoolType() {
		return schoolType;
	}

	public void setSchoolType(Integer schoolType) {
		this.schoolType = schoolType;
	}

	public Integer getGradeInfoId() {
		return gradeInfoId;
	}

	public void setGradeInfoId(Integer gradeInfoId) {
		this.gradeInfoId = gradeInfoId;
	}
}