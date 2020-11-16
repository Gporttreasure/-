package com.education.admin.api.controller.system;

import com.education.common.base.BaseController;
import com.education.common.utils.Result;
import com.education.common.utils.ResultCode;
import com.education.model.dto.MenuTree;
import com.education.model.entity.SystemMenu;
import com.education.service.system.SystemMenuService;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;


/**
 * 菜单管理
 * @author zengjintao
 * @version 1.0
 * @create_at 2019/3/22 21:44
 */
@RestController
@RequestMapping("/system/menu")
public class SystemMenuController extends BaseController {

    @Autowired
    private SystemMenuService systemMenuService;

    /**
     * 菜单列表
     * @return
     */
    @GetMapping("menuTreeList")
    @RequiresPermissions("system:menu:list")
    public Result<List<MenuTree>> menuTreeList() {
        return Result.success(systemMenuService.selectMenuTreeList());
    }

    /**
     * 菜单详情
     * @param id
     * @return
     */
    @GetMapping("selectById")
    public Result<MenuTree> selectById(Integer id) {
        return Result.success(systemMenuService.selectById(id));
    }

    /**
     * 保存或修改菜单
     * @param systemMenu
     * @return
     */
    @PostMapping
    @RequiresPermissions(value = {"system:menu:save", "system:menu:update"}, logical = Logical.OR)
    public Result saveOrUpdate(@RequestBody SystemMenu systemMenu) {
        systemMenuService.saveOrUpdate(systemMenu);
        return Result.success();
    }

    /**
     * 根据id 删除菜单
     * @param id
     * @return
     */
    @DeleteMapping("{id}")
    @RequiresPermissions("system:menu:deleteById")
    public Result deleteById(@PathVariable Integer id) {
        SystemMenu systemMenu = systemMenuService.getById(id);
        if (systemMenu.getCreateType() == ResultCode.SUCCESS) {
            return Result.success(ResultCode.FAIL, "您不能删除系统内置菜单");
        }
        systemMenuService.removeById(id);
        return Result.success("删除成功");
    }

    /**
     * 获取角色 选中的 tree 菜单
     * @param roleId
     * @return
     */
    @GetMapping("selectMenuListByRoleId")
    public Result<List<Integer>> selectMenuListByRoleId(Integer roleId) {
        return Result.success(systemMenuService.selectTreeCheckedMenuId(roleId));
    }
}
