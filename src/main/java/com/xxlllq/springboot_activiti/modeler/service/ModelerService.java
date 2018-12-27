package com.xxlllq.springboot_activiti.modeler.service;

import org.activiti.engine.repository.Model;

import java.io.InputStream;
import java.util.List;

/**
 * @author: he.feng
 * @date: 20:56 2017/11/30
 * @desc:
 **/
public interface ModelerService {

    /**
     * 创建空model
     *
     * @param name
     * @param key
     * @param description
     * @return
     */
    String crateModel(String name, String key, String description);


    /**
     * 获取模型列表
     *
     * @return
     */
    List<Model> queryModelList();


    /**
     * 查看流程图
     *
     * @param processInstanceId
     * @return
     */
    InputStream getDiagram(String processInstanceId);
}
