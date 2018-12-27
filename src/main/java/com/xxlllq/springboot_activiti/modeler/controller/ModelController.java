package com.xxlllq.springboot_activiti.modeler.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xxlllq.springboot_activiti.modeler.service.ModelerService;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.*;
import org.activiti.engine.repository.*;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.explorer.util.XmlUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程模型Model操作相关
 * Created by chenhai on 2017/5/23.
 */
@RestController
@RequestMapping("/model")
public class ModelController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(ModelController.class);

    @Autowired
    private ProcessEngine processEngine;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Resource
    private ModelerService modelerService;

    /**
     * 创建流程模型
     *
     * @param name
     * @param description
     * @return
     */
    @PostMapping(value = "/create")
    public Object createModel(@RequestParam("name") String name, @RequestParam("key") String key,
                              @RequestParam("description") String description) {
        logger.info("创建空modeler：name:{},key:{},description:{}", name, name, description);
        try {
            //创建空模型
            String modelId = modelerService.crateModel(name, key, description);
            if (StringUtils.isBlank(modelId))
                logger.error("创建modeler失败modelId:" + modelId);

            return success();
        } catch (Exception e) {
            logger.error("创建模型失败", e);
        }
        return failed("");
    }

    /**
     * 模型列表
     *
     * @param modelAndView
     * @return
     */
    @RequestMapping("/list")
    @ResponseBody
    public Object modelList(ModelAndView modelAndView) {
        return modelerService.queryModelList();
    }

    /**
     * 删除模型
     *
     * @param id
     * @return
     */
    @DeleteMapping("{id}")
    public Object deleteModel(@PathVariable("id") String id) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        repositoryService.deleteModel(id);
        return success();
    }

    /**
     * 发布模型为流程定义
     *
     * @param id
     * @return
     * @throws Exception
     */
    @PostMapping("/deployment/{id}")
    public Object deploy(@PathVariable("id") String id) throws Exception {
        //获取模型
        Model modelData = repositoryService.getModel(id);
        byte[] bytes = repositoryService.getModelEditorSource(modelData.getId());
        if (bytes == null) {
            return failed("模型数据为空，请先设计流程并成功保存，再进行发布。");
        }

        JsonNode modelNode = new ObjectMapper().readTree(bytes);
        BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
        if (model.getProcesses().size() == 0) {
            return failed("数据模型不符要求，请至少设计一条主线流程。");
        }
        byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(model);

        //发布流程
        String processName = modelData.getName() + ".bpmn20.xml";
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment()
                .name(modelData.getName())
                .addString(processName, new String(bpmnBytes, "UTF-8"));

        Deployment deployment = deploymentBuilder.deploy();
        modelData.setDeploymentId(deployment.getId());
        repositoryService.saveModel(modelData);

        return success();
    }

    /**
     * 启动流程
     *
     * @param id
     * @return
     * @throws Exception
     */
    @PostMapping("/start/{id}")
    public Object start(@PathVariable("id") String id) throws Exception {
        //获取identityService
        IdentityService identityService = processEngine.getIdentityService();
        //设置节点的执行人
        identityService.setAuthenticatedUserId("admin");
        //得到runtimeService
        RuntimeService runtimeService = processEngine.getRuntimeService();
        //根据流程定义的key（标识）来启动一个实例，activiti找该key下版本最高的流程定义
        //一般情况下为了方便开发使用该方法启动一个流程实例
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(id);
        //根据流程定义的id来启动一个实例，这种方法一般不用
        //runtimeService.startProcessInstanceById(processDefinitionId);

        System.out.println("流程梳理所属流程定义id："
                + processInstance.getProcessDefinitionId());
        System.out.println("流程实例的id：" + processInstance.getProcessInstanceId());
        System.out.println("流程实例的执行id：" + processInstance.getId());
        System.out.println("流程当前的活动（结点）id：" + processInstance.getActivityId());
        System.out.println("业务标识：" + processInstance.getBusinessKey());
        return success();
    }

    @RequestMapping(value = "/uploadFile", method = RequestMethod.POST)
    public void deployUploadedFile(
            @RequestParam("uploadfile") MultipartFile uploadfile) {
        InputStreamReader in = null;
        try {
            try {
                boolean validFile = false;
                String fileName = uploadfile.getOriginalFilename();
                if (fileName.endsWith(".bpmn20.xml") || fileName.endsWith(".bpmn")) {
                    validFile = true;

                    XMLInputFactory xif = XmlUtil.createSafeXmlInputFactory();
                    in = new InputStreamReader(new ByteArrayInputStream(uploadfile.getBytes()), "UTF-8");
                    XMLStreamReader xtr = xif.createXMLStreamReader(in);
                    BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);

                    if (bpmnModel.getMainProcess() == null || bpmnModel.getMainProcess().getId() == null) {
//                        notificationManager.showErrorNotification(Messages.MODEL_IMPORT_FAILED,
//                                i18nManager.getMessage(Messages.MODEL_IMPORT_INVALID_BPMN_EXPLANATION));
                        System.out.println("err1");
                    } else {

                        if (bpmnModel.getLocationMap().isEmpty()) {
//                            notificationManager.showErrorNotification(Messages.MODEL_IMPORT_INVALID_BPMNDI,
//                                    i18nManager.getMessage(Messages.MODEL_IMPORT_INVALID_BPMNDI_EXPLANATION));
                            System.out.println("err2");
                        } else {

                            String processName = null;
                            if (StringUtils.isNotEmpty(bpmnModel.getMainProcess().getName())) {
                                processName = bpmnModel.getMainProcess().getName();
                            } else {
                                processName = bpmnModel.getMainProcess().getId();
                            }
                            Model modelData;
                            modelData = repositoryService.newModel();
                            ObjectNode modelObjectNode = new ObjectMapper().createObjectNode();
                            modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, processName);
                            modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
                            modelData.setMetaInfo(modelObjectNode.toString());
                            modelData.setName(processName);

                            repositoryService.saveModel(modelData);

                            BpmnJsonConverter jsonConverter = new BpmnJsonConverter();
                            ObjectNode editorNode = jsonConverter.convertToJson(bpmnModel);

                            repositoryService.addModelEditorSource(modelData.getId(), editorNode.toString().getBytes("utf-8"));
                        }
                    }
                } else {
//                    notificationManager.showErrorNotification(Messages.MODEL_IMPORT_INVALID_FILE,
//                            i18nManager.getMessage(Messages.MODEL_IMPORT_INVALID_FILE_EXPLANATION));
                    System.out.println("err3");
                }
            } catch (Exception e) {
                String errorMsg = e.getMessage().replace(System.getProperty("line.separator"), "<br/>");
//                notificationManager.showErrorNotification(Messages.MODEL_IMPORT_FAILED, errorMsg);
                System.out.println("err4");
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
//                    notificationManager.showErrorNotification("Server-side error", e.getMessage());
                    System.out.println("err5");
                }
            }
        }
    }


    /**
     * 查看流程图
     *
     * @param response
     * @param processInstanceId
     */
    @RequestMapping(value = "/image", method = RequestMethod.GET)
    public void image(HttpServletResponse response,
                      @RequestParam String processInstanceId) {
        try {
            InputStream is = modelerService.getDiagram(processInstanceId);
            if (is == null)
                return;

            response.setContentType("image/png");
            BufferedImage image = ImageIO.read(is);
            OutputStream out = response.getOutputStream();
            ImageIO.write(image, "png", out);

            is.close();
            out.close();
        } catch (Exception ex) {
            logger.error("查看流程图失败", ex);
        }
    }

    /**
     * 进入流程模型页面
     *
     * @return
     */
    @RequestMapping("/view")
    public ModelAndView view() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("model/index");
        return modelAndView;
    }
}
