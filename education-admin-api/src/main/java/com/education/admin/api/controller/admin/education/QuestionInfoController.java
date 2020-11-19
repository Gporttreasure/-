package com.education.admin.api.controller.admin.education;

import com.education.business.service.education.QuestionInfoService;
import com.education.common.base.BaseController;
import com.education.common.utils.Result;
import com.education.model.dto.QuestionInfoDto;
import com.education.model.entity.QuestionInfo;
import com.education.model.request.PageParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 试题管理接口
 * @author zengjintao
 * @version 1.0
 * @create_at 2020/11/19 10:45
 */
@RestController
@RequestMapping("/system/question")
public class QuestionInfoController extends BaseController {

    @Autowired
    private QuestionInfoService questionInfoService;

    /**
     * 试题列表
     * @param pageParam
     * @param questionInfo
     * @return
     */
    @GetMapping
    public Result list(PageParam pageParam, QuestionInfo questionInfo) {
        return Result.success(questionInfoService.selectPageList(pageParam, questionInfo));
    }

    /**
     * 添加或修改试题
     * @param questionInfoDto
     * @return
     */
    @PostMapping("saveOrUpdate")
    public Result saveOrUpdate(@RequestBody QuestionInfoDto questionInfoDto) {
        questionInfoService.saveOrUpdate(questionInfoDto);
        return Result.success();
    }

    public Result selectById() {
        return null;
    }

/*    public Result selectById()*/
}
