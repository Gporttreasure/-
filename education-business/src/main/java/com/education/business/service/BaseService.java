package com.education.business.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.education.business.task.TaskManager;
import com.education.common.annotation.Unique;
import com.education.common.cache.CacheBean;
import com.education.common.constants.Constants;
import com.education.common.constants.EnumConstants;
import com.education.common.exception.BusinessException;
import com.education.common.model.PageInfo;
import com.education.common.utils.ObjectUtils;
import com.education.common.utils.ResultCode;
import com.education.model.dto.AdminUserSession;
import com.education.model.dto.StudentInfoSession;
import com.education.model.entity.BaseEntity;
import com.education.model.entity.StudentInfo;
import com.education.model.entity.SystemAdmin;
import com.education.model.request.PageParam;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * service 基类
 * @author zengjintao
 * @version 1.0
 * @create_at 2020/3/8 10:40
 */
public abstract class BaseService<M extends BaseMapper<T>, T> extends CrudService<M, T> {

    @Autowired
    protected TaskManager taskManager;
    @Autowired
    @Qualifier("redisCacheBean")
    protected CacheBean cacheBean;
    @Autowired
    protected HttpServletRequest request;
    @Resource
    protected CacheBean ehcacheBean;
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 判断试题是否客观题
     * 主观题由系统自动判分
     * @param questionType
     * @return
     */
    public boolean isObjectiveQuestion(int questionType) {
        if (questionType == EnumConstants.QuestionType.SINGLE_QUESTION.getValue()
                || questionType == EnumConstants.QuestionType.MULTIPLE_QUESTION.getValue()
                || questionType == EnumConstants.QuestionType.JUDGMENT_QUESTION.getValue()) {
            return true;
        }
        return false;
    }




    /**
     * 更新shiro 缓存中的用户信息，避免由于redis 缓存导致获取用户信息不一致问题
     * @param adminUserSession
     */
    public void updateShiroCacheUserInfo(AdminUserSession adminUserSession) {
        Subject subject = SecurityUtils.getSubject();
        PrincipalCollection principals = subject.getPrincipals();
        //realName认证信息的key，对应的value就是认证的user对象
        String realName = principals.getRealmNames().iterator().next();
        //创建一个PrincipalCollection对象
        PrincipalCollection newPrincipalCollection = new SimplePrincipalCollection(adminUserSession, realName);
        // 调用subject的runAs方法，把新的PrincipalCollection放到session里面
        subject.runAs(newPrincipalCollection);
    }

    public SystemAdmin getSystemAdmin() {
        AdminUserSession adminSession = getAdminUserSession();
        if (ObjectUtils.isNotEmpty(adminSession)) {
            return adminSession.getSystemAdmin();
        }
        return null;
    }

    public Integer getAdminUserId() {
        if (ObjectUtils.isNotEmpty(getSystemAdmin())) {
            return getSystemAdmin().getId();
        }
        return null;
    }

    /**
     * 获取管理员用户信息
     * @return
     */
    public AdminUserSession getAdminUserSession() {
        Subject subject = SecurityUtils.getSubject();
        AdminUserSession userSession = (AdminUserSession)subject.getPrincipal();
        if (ObjectUtils.isNotEmpty(userSession)) {
            return userSession;
        }
        return null;
    }

    @Override
    public boolean saveOrUpdate(T entity) {
        if (entity instanceof BaseEntity) {
            Date now = new Date();
            BaseEntity baseEntity = (BaseEntity) entity;
            Map uniqueFieldMap = this.getUniqueField(entity);
            BaseEntity result = null;
            if (uniqueFieldMap.size() > 0) {
                QueryWrapper queryWrapper = Wrappers.<T>query()
                        .select("id")
                        .allEq(uniqueFieldMap);
                result = (BaseEntity) super.getOne(queryWrapper);
            }
            if (ObjectUtils.isNotEmpty(result)) {
                ResultCode resultCode = new ResultCode(ResultCode.FAIL, "该数据已存在,请勿重复添加");
                if (baseEntity.getId() != null) {
                    // 修改的数据id 不一样，存在相同数据
                    if (baseEntity.getId().intValue() != result.getId().intValue()) {
                        throw new BusinessException(resultCode);
                    }
                } else {
                    throw new BusinessException(resultCode);
                }
            }

            if (baseEntity.getId() == null) {
                baseEntity.setCreateDate(now);
            } else {
                baseEntity.setUpdateDate(now);
            }
        }
        return super.saveOrUpdate(entity);
    }

    /**
     * 获取需要进行唯一约束校验的字段
     * @return
     */
    private Map<String, Object> getUniqueField(T entity) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entity.getClass());
        List<TableFieldInfo> fieldList = tableInfo.getFieldList();
        Map uniqueField = new HashMap<>();
        fieldList.forEach(tableFieldInfo -> {
            Field field = tableFieldInfo.getField();
            Unique unique = field.getAnnotation(Unique.class);
            if (ObjectUtils.isNotEmpty(unique)) {
                try {
                    field.setAccessible(true);
                    String column = ObjectUtils.isEmpty(unique.value()) ? field.getName() : unique.value();
                    uniqueField.put(column, field.get(entity));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
        return uniqueField;
    }

    public StudentInfoSession getStudentInfoSession() {
        String token = request.getHeader("token");
        return cacheBean.get(Constants.SESSION_NAME, token);
    }

    public StudentInfo getStudentInfo() {
        StudentInfoSession studentInfoSession = getStudentInfoSession();
       if (ObjectUtils.isNotEmpty(studentInfoSession)) {
           return studentInfoSession.getStudentInfo();
       }
       return null;
    }

    public Integer getStudentId() {
        if (this.getStudentInfo() != null) {
            return this.getStudentInfo().getId();
        }
        return null;
    }
}
