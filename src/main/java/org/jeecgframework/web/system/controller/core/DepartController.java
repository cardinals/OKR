package org.jeecgframework.web.system.controller.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.jeecgframework.core.common.controller.BaseController;
import org.jeecgframework.core.common.hibernate.qbc.CriteriaQuery;
import org.jeecgframework.core.common.model.json.AjaxJson;
import org.jeecgframework.core.common.model.json.ComboTree;
import org.jeecgframework.core.common.model.json.DataGrid;
import org.jeecgframework.core.common.model.json.TreeGrid;
import org.jeecgframework.core.constant.Globals;
import org.jeecgframework.core.util.ExceptionUtil;
import org.jeecgframework.core.util.MutiLangUtil;
import org.jeecgframework.core.util.MyBeanUtils;
import org.jeecgframework.core.util.ResourceUtil;
import org.jeecgframework.core.util.StringUtil;
import org.jeecgframework.core.util.oConvertUtils;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.entity.vo.NormalExcelConstants;
import org.jeecgframework.tag.core.easyui.TagUtil;
import org.jeecgframework.tag.vo.datatable.SortDirection;
import org.jeecgframework.tag.vo.easyui.ComboTreeModel;
import org.jeecgframework.tag.vo.easyui.TreeGridModel;
import org.jeecgframework.web.system.pojo.base.TSDepart;
import org.jeecgframework.web.system.pojo.base.TSUser;
import org.jeecgframework.web.system.pojo.base.TSUserOrg;
import org.jeecgframework.web.system.service.SystemService;
import org.jeecgframework.web.system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;


/**
 * 部门信息处理类
 * 
 * @author 张代浩
 * 
 */
@Scope("prototype")
@Controller
@RequestMapping("/departController")
public class DepartController extends BaseController {
	
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(DepartController.class);
	private UserService userService;
	private SystemService systemService;
	
	
	private String message;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Autowired
	public void setSystemService(SystemService systemService) {
		this.systemService = systemService;
	}

	public UserService getUserService() {
		return userService;
	}

	@Autowired
	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	/**
	 * 部门列表页面跳转
	 * 
	 * @return
	 */
	@RequestMapping(params = "depart")
	public ModelAndView depart() {
		return new ModelAndView("system/depart/departList");
	}

	/**
	 * easyuiAJAX请求数据
	 * 
	 * @param request
	 * @param response
	 * @param dataGrid
	 */

	@RequestMapping(params = "datagrid")
	public void datagrid(HttpServletRequest request, HttpServletResponse response, DataGrid dataGrid) {
		CriteriaQuery cq = new CriteriaQuery(TSDepart.class, dataGrid);
		this.systemService.getDataGridReturn(cq, true);
		TagUtil.datagrid(response, dataGrid);
	}

    // update-start--Author:zhangguoming  Date:20140825 for：添加业务逻辑；添加类注释；
	/**
	 * 删除部门：
	 * <ul>
     *     组织机构下存在子机构时
     *     <li>不允许删除 组织机构</li>
	 * </ul>
	 * <ul>
     *     组织机构下存在用户时
     *     <li>不允许删除 组织机构</li>
	 * </ul>
	 * <ul>
     *     组织机构下 不存在子机构 且 不存在用户时
     *     <li>删除 组织机构-角色 信息</li>
     *     <li>删除 组织机构 信息</li>
	 * </ul>
	 * @return 删除的结果信息
	 */
	@RequestMapping(params = "del")
	@ResponseBody
	public AjaxJson del(TSDepart depart, HttpServletRequest request) {
		AjaxJson j = new AjaxJson();
		depart = systemService.getEntity(TSDepart.class, depart.getId());
        message = MutiLangUtil.paramDelSuccess("common.department");
        if (depart.getTSDeparts().size() == 0) {
            Long userCount = systemService.getCountForJdbc("select count(1) from t_s_user_org where org_id='" + depart.getId() + "'");
            if(userCount == 0) { // 组织机构下没有用户时，该组织机构才允许删除。
                systemService.executeSql("delete from t_s_role_org where org_id=?", depart.getId());
                
                //systemService.delete(depart);
                
                try {
					 systemService.addLog(message, Globals.Log_Type_DEL, Globals.Log_Leavel_INFO);
				} catch (Exception e) {
					e.printStackTrace();
					 message = MutiLangUtil.paramDelFail("common.department,common.error");
				}
               
            }
        } else {
            message = MutiLangUtil.paramDelFail("common.department,common.department.parentnode.del.error");
            j.setSuccess(false);
        }

        j.setMsg(message);
		return j;
	}
    // update-end--Author:zhangguoming  Date:20140825 for：添加业务逻辑；添加类注释；

	public void upEntity(TSDepart depart) {
		List<TSUser> users = systemService.findByProperty(TSUser.class, "TSDepart.id", depart.getId());
		if (users.size() > 0) {
			for (TSUser tsUser : users) {
				//tsUser.setTSDepart(null);
				//systemService.saveOrUpdate(tsUser);
				systemService.delete(tsUser);
			}
		}
	}

	/**
	 * 添加部门
	 * 
	 * @param depart
	 * @return
	 */
	@RequestMapping(params = "save")
	@ResponseBody
	public AjaxJson save(TSDepart depart, HttpServletRequest request) {
		// 设置上级部门
		String pid = request.getParameter("TSPDepart.id");
		if (pid.equals("")) {
			depart.setTSPDepart(null);
		}
		AjaxJson j = new AjaxJson();
		if (StringUtil.isNotEmpty(depart.getId())) {
            message = MutiLangUtil.paramUpdSuccess("common.department");
			userService.saveOrUpdate(depart);
			systemService.addLog(message, Globals.Log_Type_UPDATE, Globals.Log_Leavel_INFO);
		} else {
            message = MutiLangUtil.paramAddSuccess("common.department");
			userService.save(depart);
			systemService.addLog(message, Globals.Log_Type_INSERT, Globals.Log_Leavel_INFO);
		}

        j.setMsg(message);
		return j;
	}
	@RequestMapping(params = "add")
	public ModelAndView add(TSDepart depart, HttpServletRequest req) {
		List<TSDepart> departList = systemService.getList(TSDepart.class);
		req.setAttribute("departList", departList);
		
//      这个if代码段没有用吧，注释之
//		if (StringUtil.isNotEmpty(depart.getId())) {
//			TSDepart tspDepart = new TSDepart();
//			TSDepart tsDepart = new TSDepart();
//			depart = systemService.getEntity(TSDepart.class, depart.getId());
//			tspDepart.setId(depart.getId());
//			tspDepart.setDepartname(depart.getDepartname());
//			tsDepart.setTSPDepart(tspDepart);
//			req.setAttribute("depart", tsDepart);
//		}
		
        req.setAttribute("pid", depart.getId());
		return new ModelAndView("system/depart/depart");
	}
	/**
	 * 部门列表页面跳转
	 * 
	 * @return
	 */
	@RequestMapping(params = "update")
	public ModelAndView update(TSDepart depart, HttpServletRequest req) {
		List<TSDepart> departList = systemService.getList(TSDepart.class);
		req.setAttribute("departList", departList);
		if (StringUtil.isNotEmpty(depart.getId())) {
			depart = systemService.getEntity(TSDepart.class, depart.getId());
			req.setAttribute("depart", depart);
			
			
			//只有子叶节点且不为根节点，可以修改父类节点
			if(CollectionUtils.isEmpty(depart.getTSDeparts()) && depart.getTSPDepart() != null){
				req.setAttribute("isLeafNode","true");
			}else{
				req.setAttribute("isLeafNode","false");
			}
			
		}
		return new ModelAndView("system/depart/depart");
	}
	
	/**
	 * 父级权限列表
	 * 
	 * @param request
	 * @param comboTree
	 * @return
	 */
	@RequestMapping(params = "setPFunction")
	@ResponseBody
	public List<ComboTree> setPFunction(HttpServletRequest request, ComboTree comboTree) {
		CriteriaQuery cq = new CriteriaQuery(TSDepart.class);
		if(null != request.getParameter("selfId")){
			cq.notEq("id", request.getParameter("selfId"));
		}
		if (comboTree.getId() != null) {
			cq.eq("TSPDepart.id", comboTree.getId());
		}
		if (comboTree.getId() == null) {
			cq.isNull("TSPDepart");
		}
		cq.add();
		List<TSDepart> departsList = systemService.getListByCriteriaQuery(cq, false);
		List<ComboTree> comboTrees = new ArrayList<ComboTree>();
		ComboTreeModel comboTreeModel = new ComboTreeModel("id", "departname", "TSDeparts");
		comboTrees = systemService.ComboTree(departsList, comboTreeModel, null, true);
		return comboTrees;

	}
	/**
	 * 部门列表，树形展示
	 * @param request
	 * @param response
	 * @param treegrid
	 * @return
	 */
	@RequestMapping(params = "departgrid")
	@ResponseBody
	public Object departgrid(TSDepart tSDepart,HttpServletRequest request, HttpServletResponse response, TreeGrid treegrid) {
		CriteriaQuery cq = new CriteriaQuery(TSDepart.class);
		if("yes".equals(request.getParameter("isSearch"))){
			treegrid.setId(null);
			tSDepart.setId(null);
		} 
		if(null != tSDepart.getDepartname()){
			org.jeecgframework.core.extend.hqlsearch.HqlGenerateUtil.installHql(cq, tSDepart);
		}
		if (treegrid.getId() != null) {
			cq.eq("TSPDepart.id", treegrid.getId());
		}
		if (treegrid.getId() == null) {
			cq.isNull("TSPDepart");
		}
		cq.add();
		List<TreeGrid> departList =null;
		departList=systemService.getListByCriteriaQuery(cq, false);
		if(departList.size()==0&&tSDepart.getDepartname()!=null){ 
			cq = new CriteriaQuery(TSDepart.class);
			TSDepart parDepart = new TSDepart();
			tSDepart.setTSPDepart(parDepart);
			org.jeecgframework.core.extend.hqlsearch.HqlGenerateUtil.installHql(cq, tSDepart);
		    departList =systemService.getListByCriteriaQuery(cq, false);
		}
		List<TreeGrid> treeGrids = new ArrayList<TreeGrid>();
		TreeGridModel treeGridModel = new TreeGridModel();
		treeGridModel.setTextField("departname");
		treeGridModel.setParentText("TSPDepart_departname");
		treeGridModel.setParentId("TSPDepart_id");
		treeGridModel.setSrc("description");
		treeGridModel.setIdField("id");
		treeGridModel.setChildList("TSDeparts");
        Map<String,Object> fieldMap = new HashMap<String, Object>();
        fieldMap.put("orgCode", "orgCode");
        fieldMap.put("orgType", "orgType");
		fieldMap.put("mobile", "mobile");
		fieldMap.put("fax", "fax");
		fieldMap.put("address", "address");
        treeGridModel.setFieldMap(fieldMap);
        treeGrids = systemService.treegrid(departList, treeGridModel);

        JSONArray jsonArray = new JSONArray();
        for (TreeGrid treeGrid : treeGrids) {
            jsonArray.add(JSON.parse(treeGrid.toJson()));
        }
        return jsonArray;
	}
	//----
	/**
	 * 方法描述:  查看成员列表
	 * 作    者： yiming.zhang
	 * 日    期： Dec 4, 2013-8:53:39 PM
	 * @param request
	 * @param departid
	 * @return 
	 * 返回类型： ModelAndView
	 */
	@RequestMapping(params = "userList")
	public ModelAndView userList(HttpServletRequest request, String departid) {
		request.setAttribute("departid", departid);
		return new ModelAndView("system/depart/departUserList");
	}
	
	/**
	 * 方法描述:  成员列表dataGrid
	 * 作    者： yiming.zhang
	 * 日    期： Dec 4, 2013-10:40:17 PM
	 * @param user
	 * @param request
	 * @param response
	 * @param dataGrid 
	 * 返回类型： void
	 */
	@RequestMapping(params = "userDatagrid")
	public void userDatagrid(TSUser user,HttpServletRequest request, HttpServletResponse response, DataGrid dataGrid) {
		CriteriaQuery cq = new CriteriaQuery(TSUser.class, dataGrid);
		//查询条件组装器
		user.setDepartid(null);
		org.jeecgframework.core.extend.hqlsearch.HqlGenerateUtil.installHql(cq, user);
		String departid = oConvertUtils.getString(request.getParameter("departid"));
		if (!StringUtil.isEmpty(departid)) {
//            update-start--Author:zhangguoming  Date:20140825 for：用户表字段变更后的查询字段修改
			DetachedCriteria dc = cq.getDetachedCriteria();
			DetachedCriteria dcDepart = dc.createCriteria("userOrgList");// 内关联获取orgList属性
			dcDepart.add(Restrictions.eq("tsDepart.id", departid));
            // 这种方式也是可以的
//            DetachedCriteria dcDepart = dc.createAlias("userOrgList", "userOrg");
//            dcDepart.add(Restrictions.eq("userOrg.tsDepart.id", departid));
//            update-end--Author:zhangguoming  Date:20140825 for：用户表字段变更后的查询字段修改
		}
		Short[] userstate = new Short[] { Globals.User_Normal, Globals.User_ADMIN };
		cq.in("status", userstate);
		cq.add();
		this.systemService.getDataGridReturn(cq, true);// 第二个参数是否分页
		TagUtil.datagrid(response, dataGrid);
	}
	//----

//    update-start--Author:zhangguoming  Date:20140826 for：获取机构树；
    /**
     * 获取机构树-combotree
     * @param request
     * @return
     */
    @RequestMapping(params = "getOrgTree")
    @ResponseBody
    public List<ComboTree> getOrgTree(HttpServletRequest request) {
//        findHql不能处理is null条件
//        List<TSDepart> departsList = systemService.findHql("from TSPDepart where TSPDepart.id is null");
        List<TSDepart> departsList = systemService.findByQueryString("from TSDepart where TSPDepart.id is null");
        List<ComboTree> comboTrees = new ArrayList<ComboTree>();
        ComboTreeModel comboTreeModel = new ComboTreeModel("id", "departname", "TSDeparts");
        comboTrees = systemService.ComboTree(departsList, comboTreeModel, null, true);
        return comboTrees;
    }
//    update-end--Author:zhangguoming  Date:20140826 for：获取机构树；


//    update-start--Author:zhangguoming  Date:20140826 for：添加已有用户到组织机构；
    /**
     * 添加 用户到组织机构 的页面  跳转
     * @param req request
     * @return 处理结果信息
     */
    @RequestMapping(params = "goAddUserToOrg")
    public ModelAndView goAddUserToOrg(HttpServletRequest req) {
        req.setAttribute("orgId", req.getParameter("orgId"));
        return new ModelAndView("system/depart/noCurDepartUserList");
    }
    /**
     * 获取 除当前 组织之外的用户信息列表
     * @param request request
     * @return 处理结果信息
     */
    @RequestMapping(params = "addUserToOrgList")
    public void addUserToOrgList(TSUser user, HttpServletRequest request, HttpServletResponse response, DataGrid dataGrid) {
        String orgId = request.getParameter("orgId");

        CriteriaQuery cq = new CriteriaQuery(TSUser.class, dataGrid);
        org.jeecgframework.core.extend.hqlsearch.HqlGenerateUtil.installHql(cq, user);

        // 获取 当前组织机构的用户信息
        CriteriaQuery subCq = new CriteriaQuery(TSUserOrg.class);
        subCq.setProjection(Property.forName("tsUser.id"));
        subCq.eq("tsDepart.id", orgId);
        subCq.add();

        cq.add(Property.forName("id").notIn(subCq.getDetachedCriteria()));
        cq.add();

        this.systemService.getDataGridReturn(cq, true);
        TagUtil.datagrid(response, dataGrid);
    }
    /**
     * 添加 用户到组织机构
     * @param req request
     * @return 处理结果信息
     */
    @RequestMapping(params = "doAddUserToOrg")
    @ResponseBody
    public AjaxJson doAddUserToOrg(HttpServletRequest req) {
        AjaxJson j = new AjaxJson();
        TSDepart depart = systemService.getEntity(TSDepart.class, req.getParameter("orgId"));
        saveOrgUserList(req, depart);
        message =  MutiLangUtil.paramAddSuccess("common.user");
//      systemService.addLog(message, Globals.Log_Type_UPDATE, Globals.Log_Leavel_INFO);
        j.setMsg(message);

        return j;
    }
    /**
     * 保存 组织机构-用户 关系信息
     * @param request request
     * @param depart depart
     */
    private void saveOrgUserList(HttpServletRequest request, TSDepart depart) {
        String orgIds = oConvertUtils.getString(request.getParameter("userIds"));

        List<TSUserOrg> userOrgList = new ArrayList<TSUserOrg>();
        List<String> userIdList = extractIdListByComma(orgIds);
        for (String userId : userIdList) {
            TSUser user = new TSUser();
            user.setId(userId);

            TSUserOrg userOrg = new TSUserOrg();
            userOrg.setTsUser(user);
            userOrg.setTsDepart(depart);

            userOrgList.add(userOrg);
        }
        if (!userOrgList.isEmpty()) {
            systemService.batchSave(userOrgList);
        }
    }
//    update-end--Author:zhangguoming  Date:20140826 for：添加已有用户到组织机构

//    update-start--Author:zhangguoming  Date:20140827 for：用户列表页面 组织机构查询条件：选择组织机构列表 相关操作
    /**
     * 用户选择机构列表跳转页面
     *
     * @return
     */
    @RequestMapping(params = "departSelect")
    public String departSelect() {
        return "system/depart/departSelect";
    }
    /**
     * 角色显示列表
     *
     * @param response response
     * @param dataGrid dataGrid
     */
    @RequestMapping(params = "departSelectDataGrid")
    public void datagridRole(HttpServletResponse response, DataGrid dataGrid) {
        CriteriaQuery cq = new CriteriaQuery(TSDepart.class, dataGrid);
        this.systemService.getDataGridReturn(cq, true);
        TagUtil.datagrid(response, dataGrid);
    }

//    update-end--Author:zhangguoming  Date:20140827 for：用户列表页面 组织机构查询条件：选择组织机构列表 相关操作
	/**
	 * 导入功能跳转
	 *
	 * @return
	 */
	@RequestMapping(params = "upload")
	public ModelAndView upload(HttpServletRequest req) {
		req.setAttribute("controller_name","departController");
		return new ModelAndView("common/upload/pub_excel_upload");
	}

	/**
	 * 导出excel
	 *
	 * @param request
	 * @param response
	 */
	@RequestMapping(params = "exportXls")
	public String exportXls(TSDepart tsDepart, HttpServletRequest request, HttpServletResponse response
			, DataGrid dataGrid, ModelMap modelMap) {
		CriteriaQuery cq = new CriteriaQuery(TSDepart.class, dataGrid);
		org.jeecgframework.core.extend.hqlsearch.HqlGenerateUtil.installHql(cq, tsDepart, request.getParameterMap());
		cq.addOrder("orgCode", SortDirection.asc);
		List<TSDepart> tsDeparts = this.systemService.getListByCriteriaQuery(cq,false);
		/*List<TSDepart> finalTsDeparts = new ArrayList<TSDepart>();
		List<TSDepart> tsDeparts = systemService.getSession().createSQLQuery("select * from t_s_depart where length(org_code) = 3 order by org_code asc").addEntity(TSDepart.class).list();
		for(TSDepart tsDepart1:tsDeparts){
			finalTsDeparts.add(tsDepart1);
			String orgcode1 = tsDepart1.getOrgCode();
			List<TSDepart> tsDeparts1 = systemService.getSession().createSQLQuery("select * from t_s_depart where org_code like :orgcode and length(org_code)=6 order by org_code asc").addEntity(TSDepart.class).setString("orgcode",orgcode1+"%").list();
			for(TSDepart tsDepart2:tsDeparts1){
				finalTsDeparts.add(tsDepart2);
				String orgcode2 = tsDepart2.getOrgCode();
				List<TSDepart> tsDeparts2 = systemService.getSession().createSQLQuery("select * from t_s_depart where org_code like :orgcode and length(org_code)=9 order by org_code asc").addEntity(TSDepart.class).setString("orgcode",orgcode2+"%").list();
				for(TSDepart tsDepart3:tsDeparts2){
					finalTsDeparts.add(tsDepart3);
				}
			}
		}*/
		modelMap.put(NormalExcelConstants.FILE_NAME,"组织机构表");
		modelMap.put(NormalExcelConstants.CLASS,TSDepart.class);
		modelMap.put(NormalExcelConstants.PARAMS,new ExportParams("组织机构表列表", "导出人:"+ ResourceUtil.getSessionUserName().getRealName(),
				"导出信息"));
		modelMap.put(NormalExcelConstants.DATA_LIST,tsDeparts);
		return NormalExcelConstants.JEECG_EXCEL_VIEW;
	}
	/**
	 * 导出excel 使模板
	 *
	 * @param request
	 * @param response
	 */
	@RequestMapping(params = "exportXlsByT")
	public String exportXlsByT(TSDepart tsDepart,HttpServletRequest request,HttpServletResponse response
			, DataGrid dataGrid,ModelMap modelMap) {
		modelMap.put(NormalExcelConstants.FILE_NAME,"组织机构表");
		modelMap.put(NormalExcelConstants.CLASS,TSDepart.class);
		modelMap.put(NormalExcelConstants.PARAMS,new ExportParams("组织机构表列表", "导出人:"+ResourceUtil.getSessionUserName().getRealName(),
				"导出信息"));
		modelMap.put(NormalExcelConstants.DATA_LIST,new ArrayList());
		return NormalExcelConstants.JEECG_EXCEL_VIEW;
	}

	@RequestMapping(params = "importExcel", method = RequestMethod.POST)
//	@RequestMapping(value="/importExcel",method=RequestMethod.POST)  
	@ResponseBody
	public AjaxJson importExcel(HttpServletRequest request, HttpServletResponse response) {
		AjaxJson j = new AjaxJson();
		
		MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
		Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
		for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
			MultipartFile file = entity.getValue();// 获取上传文件对象
			ImportParams params = new ImportParams();
			params.setTitleRows(2);
			params.setHeadRows(1);
			params.setNeedSave(true);
			try {
				List<TSDepart> tsDeparts = ExcelImportUtil.importExcel(file.getInputStream(),TSDepart.class,params);
				for (TSDepart tsDepart : tsDeparts) {
					String orgCode = tsDepart.getOrgCode();
					List<TSDepart> departs = systemService.findByProperty(TSDepart.class,"orgCode",orgCode);
					if(departs.size()!=0){
						TSDepart depart = departs.get(0);
						MyBeanUtils.copyBeanNotNull2Bean(tsDepart,depart);
						systemService.saveOrUpdate(depart);
					}else {
						tsDepart.setOrgType(tsDepart.getOrgType().substring(0,1));
						//TSTypegroup ts = systemService.findByProperty(TSTypegroup.class,"typegroupcode","orgtype").get(0);
						//List<TSType> types = systemService.findByProperty(TSType.class,"id",ts.getId());
						//int len = 3;//每级组织机构得长度
						/*for(int i=0;i<types.size();i++){
							String typecode = types.get(i).getTypecode();

						}*/
						String orgcode = tsDepart.getOrgCode();
						String parentOrgCode = orgcode.substring(0,orgcode.length()-3);
						TSDepart parentDept = (TSDepart) systemService.getSession().createSQLQuery("select * from t_s_depart where ORG_CODE = :parentOrgCode")
								.addEntity(TSDepart.class)
								.setString("parentOrgCode",parentOrgCode)
								.list().get(0);
						tsDepart.setTSPDepart(parentDept);
						systemService.save(tsDepart);
					}
				}
				j.setMsg("文件导入成功！");
			} catch (Exception e) {
				j.setMsg("文件导入失败！");
				logger.error(ExceptionUtil.getExceptionMessage(e));
			}finally{
				try {
					file.getInputStream().close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return j;
	}
	
	
	
	
	
	
	//update--start--by:jg_renjie--at:20160318 for:#942 【组件封装】组织机构弹出模式，目前是列表，得改造成树方式
	@RequestMapping(params = "getDepartNameByCode")
	@ResponseBody
	public AjaxJson getDepartNameByCode(HttpServletRequest request, HttpServletResponse response){
		
		AjaxJson j = new AjaxJson();
		try {
			String orgCode = request.getParameter("orgCode");
			TSDepart depart = systemService.findUniqueByProperty(TSDepart.class, "orgCode", orgCode);
			j.setMsg(" ");
			if(null!=depart){
				j.setMsg(depart.getDepartname());
			}
		
			j.setSuccess(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return j;
		
	}
	
	
	
	   //获取departName_____工时项目修改
    @RequestMapping(params = "getDepartNamesByIds")
    @ResponseBody
    public AjaxJson getDepartNamesByIds(HttpServletRequest request, HttpServletResponse response){
        
        AjaxJson j = new AjaxJson();
        try {
            String orgIds = request.getParameter("orgIds");
            String result = "";
            if(StringUtils.isNotEmpty(orgIds)){
                String [] arr = orgIds.split("[,]");
                for(String id:arr){
                    TSDepart depart = systemService.findUniqueByProperty(TSDepart.class, "id", id);
                    
                    result += depart.getDepartname()+",";
                }
            }
            
            if(StringUtils.isNotEmpty(result)){
                result= result.substring(0, result.length()-1);
            }
       
            j.setMsg(result);
        
        } catch (Exception e) {
            e.printStackTrace();
        }
        return j;
    }
	
	
	
	
	
	//update--start--by:jg_renjie--at:20160318 for:#942 【组件封装】组织机构弹出模式，目前是列表，得改造成树方式
	@RequestMapping(params = "getDepartInfo")
	@ResponseBody
	public AjaxJson getDepartInfo(HttpServletRequest request, HttpServletResponse response){
		
		AjaxJson j = new AjaxJson();
		
		//String level = request.getParameter("level");
		String parentid = request.getParameter("parentid");
		
		List<TSDepart> tSDeparts = new ArrayList<TSDepart>();
		
		StringBuffer hql = new StringBuffer(" from TSDepart t where 1=1 ");
		if(StringUtils.isNotBlank(parentid)){
			
			TSDepart dePart = this.systemService.getEntity(TSDepart.class, parentid);
			
			hql.append(" and TSPDepart = ?");
			tSDeparts = this.systemService.findHql(hql.toString(), dePart);
		} else {
			hql.append(" and t.orgType = ?");
			tSDeparts = this.systemService.findHql(hql.toString(), "1");
		}
		List<Map<String,Object>> dateList = new ArrayList<Map<String,Object>>();
		if(tSDeparts.size()>0){
			Map<String,Object> map = null;
			String sql = null;
			 Object[] params = null;
			for(TSDepart depart:tSDeparts){
				map = new HashMap<String,Object>();
				map.put("id", depart.getId());
				map.put("name", depart.getDepartname());
				if(StringUtils.isNotBlank(parentid)){
					map.put("pId", parentid);
				} else{
					map.put("pId", "1");
				}
				//根据id判断是否有子节点
				sql = "select count(1) from t_s_depart t where t.parentdepartid = ?";
				params = new Object[]{depart.getId()};
				long count = this.systemService.getCountForJdbcParam(sql, params);
				if(count>0){
					map.put("isParent",true);
				}
				dateList.add(map);
			}
		}
		net.sf.json.JSONArray jsonArray = net.sf.json.JSONArray.fromObject(dateList);
		j.setMsg(jsonArray.toString());
		return j;
	}
	//update--end--by:jg_renjie--at:20160318 for:#942 【组件封装】组织机构弹出模式，目前是列表，得改造成树方式
	
	
	
	@RequestMapping(params = "getAllDepartInfo")
	@ResponseBody
	public AjaxJson getAllDepartInfo(HttpServletRequest request, HttpServletResponse response){
		
		AjaxJson j = new AjaxJson();
		
		//String level = request.getParameter("level");
		
	    List<TSDepart> tSDeparts = this.systemService.findHql("from TSDepart", null);
		
		List<Map<String,Object>> dateList = new ArrayList<Map<String,Object>>();
		if(tSDeparts.size()>0){
			Map<String,Object> map = null;
			String sql = null;
			 Object[] params = null;
			for(TSDepart depart:tSDeparts){
				map = new HashMap<String,Object>();
				map.put("id", depart.getId());
				map.put("name", depart.getDepartname());
				TSDepart tspDepart = depart.getTSPDepart();
				
				
				if(tspDepart != null && StringUtils.isNotBlank(tspDepart.getId())){
					map.put("pId", tspDepart.getId());
				} else{
					map.put("pId", "1");
				}
				//根据id判断是否有子节点
				sql = "select count(1) from t_s_depart t where t.parentdepartid = ?";
				params = new Object[]{depart.getId()};
				long count = this.systemService.getCountForJdbcParam(sql, params);
				if(count>0){
					map.put("isParent",true);
				}
				dateList.add(map);
			}
		}
		net.sf.json.JSONArray jsonArray = net.sf.json.JSONArray.fromObject(dateList);
		j.setMsg(jsonArray.toString());
		return j;
	}
	
	
	
	/**
	 * 根据父类节点异步加载
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(params = "getDepartInfoByAsync")
	@ResponseBody
	public List<Map<String,Object>> getDepartInfoByAsync(HttpServletRequest request, HttpServletResponse response){
		
		String parentid = request.getParameter("parentid");
		
		List<TSDepart> tSDeparts = new ArrayList<TSDepart>();
		
		StringBuffer hql = new StringBuffer(" from TSDepart t where 1=1 ");
		if(StringUtils.isNotBlank(parentid)){
			
			TSDepart dePart = this.systemService.getEntity(TSDepart.class, parentid);
			
			hql.append(" and TSPDepart = ?");
			tSDeparts = this.systemService.findHql(hql.toString(), dePart);
		} else {
			hql.append(" and t.orgType = ?");
			tSDeparts = this.systemService.findHql(hql.toString(), "1");
		}
		List<Map<String,Object>> dateList = new ArrayList<Map<String,Object>>();
		if(tSDeparts.size()>0){
			Map<String,Object> map = null;
			String sql = null;
			 Object[] params = null;
			for(TSDepart depart:tSDeparts){
				map = new HashMap<String,Object>();
				map.put("id", depart.getId());
				map.put("name", depart.getDepartname());
				if(StringUtils.isNotBlank(parentid)){
					map.put("pId", parentid);
				} else{
					map.put("pId", "1");
				}
				//根据id判断是否有子节点
				sql = "select count(1) from t_s_depart t where t.parentdepartid = ?";
				params = new Object[]{depart.getId()};
				long count = this.systemService.getCountForJdbcParam(sql, params);
				if(count>0){
					map.put("isParent",true);
				}
				dateList.add(map);
			}
		}
		return dateList;
	}
}