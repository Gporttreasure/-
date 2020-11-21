package com.education.business.service.education;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.business.mapper.education.QuestionInfoMapper;
import com.education.business.service.BaseService;
import com.education.common.model.PageInfo;
import com.education.common.utils.ObjectUtils;
import com.education.model.dto.QuestionInfoDto;
import com.education.model.entity.QuestionInfo;
import com.education.model.entity.QuestionLanguagePointsInfo;
import com.education.model.request.PageParam;
import com.education.model.request.QuestionInfoQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 试题管理
 * @author zengjintao
 * @version 1.0
 * @create_at 2020/11/19 11:02
 */
@Service
public class QuestionInfoService extends BaseService<QuestionInfoMapper, QuestionInfo> {

    @Autowired
    private QuestionLanguagePointsInfoService questionLanguagePointsInfoService;

    /**
     * 试题分页列表
     * @param pageParam
     * @param questionInfo
     * @return
     */
    public PageInfo<QuestionInfoDto> selectPageList(PageParam pageParam, QuestionInfoQuery questionInfoQuery) {
        Page<QuestionInfoDto> page = new Page<>(pageParam.getPageNumber(), pageParam.getPageSize());
        return selectPage(baseMapper.selectPageList(page, questionInfoQuery));
    }


    /**
     * 添加或修改试题
     * @param questionInfoDto
     * @return
     */
    @Transactional
    public boolean saveOrUpdate(QuestionInfoDto questionInfoDto) {
        List<Integer> languagePointsInfoIdList = questionInfoDto.getLanguagePointsInfoId();
        if (ObjectUtils.isNotEmpty(questionInfoDto.getId())) {
            // 删除试题知识点关联
            LambdaQueryWrapper queryWrapper = Wrappers.<QuestionLanguagePointsInfo>lambdaQuery()
                    .eq(QuestionLanguagePointsInfo::getQuestionInfoId, questionInfoDto.getId());
            questionLanguagePointsInfoService.remove(queryWrapper);
        }

        super.saveOrUpdate(questionInfoDto); // 保存试题记录

        // 保存试题知识点关联信息
        List<QuestionLanguagePointsInfo> questionLanguagePointsInfoList = new ArrayList<>();
        languagePointsInfoIdList.forEach(languagePointsInfoId -> {
            QuestionLanguagePointsInfo questionLanguagePointsInfo = new QuestionLanguagePointsInfo();
            questionLanguagePointsInfo.setLanguagePointsInfoId(languagePointsInfoId);
            questionLanguagePointsInfo.setQuestionInfoId(questionInfoDto.getId());
            questionLanguagePointsInfo.setCreateDate(new Date());
            questionLanguagePointsInfoList.add(questionLanguagePointsInfo);
        });
        return questionLanguagePointsInfoService.saveBatch(questionLanguagePointsInfoList);
    }

    public QuestionInfoDto selectById(Integer id) {
        return baseMapper.selectById(id);
    }
}
