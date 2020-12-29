package com.education.core.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.education.business.service.system.SystemDictService;
import com.education.common.base.BaseController;
import com.education.common.constants.CacheKey;
import com.education.common.model.PageInfo;
import com.education.common.utils.Result;
import com.education.model.entity.SystemDict;
import com.education.model.request.PageParam;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;


/**
 * 字典管理接口
 * @author zengjintao
 * @version 1.0
 * @create_at 2020/3/9 21:15
 */
@RequestMapping("/api/dict")
@RestController
@Api(tags = "字典管理接口")
public class DictController extends BaseController {

    @Autowired
    private SystemDictService systemDictService;


    /**
     * 字典类型列表
     * @param pageParam
     * @param systemDict
     * @return
     */
    @GetMapping
    @Cacheable(cacheNames = CacheKey.SYSTEM_DICT, keyGenerator = CacheKey.KEY_GENERATOR_BEAN_NAME)
    public Result<PageInfo<SystemDict>> list(PageParam pageParam, SystemDict systemDict) {
        LambdaQueryWrapper queryWrapper = Wrappers.lambdaQuery(systemDict)
                .orderByDesc(SystemDict::getSort);
        return Result.success(systemDictService.selectPage(pageParam, queryWrapper));
    }

    /**
     * 添加或修改字典类型
     * @param systemDict
     * allEntries 设置为true 清空所有key
     * @return
     */
    @PostMapping
    @CacheEvict(cacheNames = CacheKey.SYSTEM_DICT, keyGenerator = CacheKey.KEY_GENERATOR_BEAN_NAME, allEntries = true)
    public Result saveOrUpdate(@RequestBody SystemDict systemDict) {
        systemDictService.saveOrUpdate(systemDict);
        return Result.success();
    }

    /**
     * 根据id 删除字典类型
     * @param id
     * @return
     */
    @DeleteMapping("{id}")
    @CacheEvict(
        cacheNames = {CacheKey.SYSTEM_DICT, CacheKey.SYSTEM_DICT_VALUE},
        keyGenerator = CacheKey.KEY_GENERATOR_BEAN_NAME,
        allEntries = true
    )
    public Result deleteById(@PathVariable Integer id) {
        systemDictService.deleteById(id);
        return Result.success();
    }
}
