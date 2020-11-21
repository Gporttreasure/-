package com.education.model.dto;

import com.education.model.entity.StudentInfo;

/**
 * @author zengjintao
 * @version 1.0
 * @create_at 2020/11/21 18:25
 */
public class StudentInfoDto extends StudentInfo {

    private String gradeName;
    private String confirmPassword;

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public void setGradeName(String gradeName) {
        this.gradeName = gradeName;
    }

    public String getGradeName() {
        return gradeName;
    }
}
