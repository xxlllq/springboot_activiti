package com.xxlllq.springboot_activiti.modeler.controller;

import java.util.HashMap;
import java.util.Map;

/**
 * @类名称： BaseController
 * @类描述：
 * @创建人：xiangxl
 * @创建时间：2018/12/21 12:42
 * @version：
 */
public class BaseController {

    /**
     * 执行成功返回的结果
     *
     * @return
     */
    protected Map<String, Object> success() {
        Map<String, Object> map = new HashMap();
        map.put("status", true);
        map.put("reason", "操作成功");
        return map;
    }

    /**
     * 执行成功返回的结果
     *
     * @return
     */
    protected Map<String, Object> failed(String reason) {
        Map<String, Object> map = new HashMap();
        map.put("status", false);
        map.put("reason", "操作失败：" + reason);
        return map;
    }
}
