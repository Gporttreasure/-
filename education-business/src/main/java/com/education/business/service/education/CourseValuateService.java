package com.education.business.service.education;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.business.mapper.education.CourseValuateMapper;
import com.education.business.service.BaseService;
import com.education.business.task.CourseValuateMessageListener;
import com.education.business.task.TaskParam;
import com.education.common.constants.Constants;
import com.education.common.enums.ValuateTypeEnum;
import com.education.common.exception.BusinessException;
import com.education.common.model.PageInfo;
import com.education.model.dto.CourseValuateDto;
import com.education.model.entity.CourseValuate;
import com.education.model.request.PageParam;
import org.springframework.stereotype.Service;

/**
 * @author zengjintao
 * @create_at 2021/10/17 9:32
 * @since version 1.0.3
 */
@Service
public class CourseValuateService extends BaseService<CourseValuateMapper, CourseValuate> {

    public PageInfo<CourseValuateDto> listPage(PageParam pageParam, CourseValuate courseValuate) {
        Page<CourseValuateDto> page = new Page<>(pageParam.getPageNumber(), pageParam.getPageSize());
        return super.selectPage(baseMapper.listPage(page, courseValuate));
    }

    @Override
    public boolean saveOrUpdate(CourseValuate courseValuate) {
        Integer valuateMark = courseValuate.getValuateMark();
        if (valuateMark > 0 && valuateMark < 5) {
            courseValuate.setValuateType(ValuateTypeEnum.NEGATIVE.getValue());
        } else if (valuateMark >= 5 && valuateMark < 8) {
            courseValuate.setValuateType(ValuateTypeEnum.NEUTRAL.getValue());
        } else if (valuateMark >= 8 && valuateMark <= Constants.COURSE_VALUATE_MARK) {
            courseValuate.setValuateType(ValuateTypeEnum.GOOD.getValue());
        } else {
            throw new BusinessException("评价分数不能超过10分");
        }

        if (courseValuate.getId() == null) {
            TaskParam taskParam = new TaskParam(CourseValuateMessageListener.class);
            taskParam.put("courseId", courseValuate.getCourseId());
            taskParam.put("valuateMark", valuateMark);
            taskManager.pushTask(taskParam);
        }
        courseValuate.setStudentId(getStudentId());
        return super.saveOrUpdate(courseValuate);
    }
}
