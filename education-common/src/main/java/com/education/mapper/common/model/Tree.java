package com.education.mapper.common.model;import lombok.Data;import java.util.List;/** * tree 树形结构实体类 * @author zengjintao * @create 2019/6/20 15:38 * @since 1.0 **/@Datapublic class Tree {    private Integer parentId;    private Integer id;    private List<Tree> children;    private String label;    private Integer value;    public Tree() {    }    public Tree(int id, String label, int parentId) {       this.label = label;       this.id = id;       this.parentId = parentId;    }}