package com.education.business.service.education;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.business.mapper.education.TestPaperInfoMapper;
import com.education.business.service.BaseService;
import com.education.common.exception.BusinessException;
import com.education.common.model.PageInfo;
import com.education.common.utils.ObjectUtils;
import com.education.common.utils.ResultCode;
import com.education.model.dto.TestPaperInfoDto;
import com.education.model.dto.TestPaperQuestionDto;
import com.education.model.entity.TestPaperInfo;
import com.education.model.entity.TestPaperQuestionInfo;
import com.education.model.request.PageParam;
import com.education.model.request.TestPaperQuestionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;

/**
 * @author zengjintao
 * @version 1.0
 * @create_at 2020/11/20 21:22
 */
@Service
public class TestPaperInfoService extends BaseService<TestPaperInfoMapper, TestPaperInfo> {

    @Autowired
    private TestPaperQuestionInfoService testPaperQuestionInfoService;

    /**
     * 试卷分页列表
     * @param pageParam
     * @param testPaperInfo
     * @return
     */
    public PageInfo<TestPaperInfoDto> selectPageList(PageParam pageParam, TestPaperInfo testPaperInfo) {
        Page<TestPaperInfoDto> page = new Page<>(pageParam.getPageNumber(), pageParam.getPageSize());
        return selectPage(baseMapper.selectPageList(page, testPaperInfo));
    }

    /**
     * 试卷试题列表
     * @param pageParam
     * @param testPaperQuestionRequest
     * @return
     */
    public PageInfo<TestPaperQuestionDto> selectPaperQuestionList(PageParam pageParam, TestPaperQuestionRequest testPaperQuestionRequest) {
        Page<TestPaperQuestionDto> page = new Page<>(pageParam.getPageNumber(), pageParam.getPageSize());
        return selectPage(baseMapper.selectPaperQuestionList(page, testPaperQuestionRequest));
    }

    @Transactional
    public void updatePaperQuestionMarkOrSort(TestPaperQuestionDto testPaperQuestionDto) {
        testPaperQuestionDto.setUpdateDate(new Date());
        LambdaUpdateWrapper updateWrapper = Wrappers.lambdaUpdate(TestPaperQuestionInfo.class)
                .eq(TestPaperQuestionInfo::getQuestionInfoId, testPaperQuestionDto.getQuestionInfoId())
                .eq(TestPaperQuestionInfo::getTestPaperInfoId, testPaperQuestionDto.getTestPaperInfoId())
                .set(TestPaperQuestionInfo::getMark, testPaperQuestionDto.getMark())
                .set(TestPaperQuestionInfo::getSort, testPaperQuestionDto.getSort());
        testPaperQuestionInfoService.update(updateWrapper);

        // 更新试卷总分
        if (ObjectUtils.isNotEmpty(testPaperQuestionDto.getUpdateType()) &&
                testPaperQuestionDto.getUpdateType().intValue() == ResultCode.SUCCESS) {
            TestPaperInfo testPaperInfo = this.getById(testPaperQuestionDto.getTestPaperInfoId());
            testPaperInfo.setMark(testPaperQuestionDto.getMark() + testPaperInfo.getMark());
            this.updateById(testPaperInfo);
        }
    }

    @Override
    public boolean saveOrUpdate(TestPaperInfo testPaperInfo) {
        if (testPaperInfo.getId() != null && testPaperInfo.getExamNumber() > 0) {
            throw new BusinessException(new ResultCode(ResultCode.FAIL, "试卷已被使用, 无法修改"));
        }
        return super.saveOrUpdate(testPaperInfo);
    }

    @Transactional
    public ResultCode deleteById(Integer id) {
        TestPaperInfo testPaperInfo = super.getById(id);
        if (testPaperInfo.getExamNumber() == 0) {
            super.removeById(id);
            // 删除试卷试题关联信息
            testPaperQuestionInfoService.deleteByTestPaperInfoId(id);
            return new ResultCode(ResultCode.SUCCESS, "删除成功");
        }
        return new ResultCode(ResultCode.FAIL, "试卷已被使用, 无法删除");
    }

    public ResultCode publishTestPaperInfo(Integer testPaperInfoId) {
        TestPaperInfo testPaperInfo = super.getById(testPaperInfoId);
        if (testPaperInfo.getPublishFlag()) {
            return new ResultCode(ResultCode.FAIL, "试卷已发布,请勿重复操作");
        }

        boolean flag = testPaperQuestionInfoService.hasTestPaperInfoQuestion(testPaperInfoId);
        if (!flag) {
            return new ResultCode(ResultCode.FAIL, "改试卷暂未关联试题,请关联试题之后在发布");
        }

        testPaperInfo.setPublishFlag(true);
        testPaperInfo.setPublishTime(new Date());
        super.updateById(testPaperInfo);
        return new ResultCode(ResultCode.SUCCESS, "发布成功");
    }

    public Object cancelTestPaperInfo(Integer testPaperInfoId) {
        TestPaperInfo testPaperInfo = super.getById(testPaperInfoId);
        if (testPaperInfo.getExamNumber() > 0) {
            return new ResultCode(ResultCode.FAIL, "试卷已有学员作答, 无法撤回");
        }
        testPaperInfo.setPublishFlag(false);
        super.updateById(testPaperInfo);
        return new ResultCode(ResultCode.SUCCESS, "撤销成功");
    }
}
