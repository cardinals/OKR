<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="/context/mytags.jsp"%>
<t:base type="jquery,easyui,tools,DatePicker"></t:base>


<%--update-start--Author:lijun  Date:20160301 for：组织机构查询条件修改为使用departSelect标签--%>
<%--update-start--Author:zhangguoming  Date:20140827 for：添加 组织机构查询条件--%>
<script>
    $(function() {
    	/* 去除选择框输入方式，by zbk  2016-11-03 */
    	/* //添加特殊的弹出框提示
        var datagrid = $("#userListtb");
        //datagrid.find("div[name='searchColums']").append($("#tempSearchColums div[name='searchColums']").html());
		datagrid.find("div[name='searchColums']").find("form#userListForm").append($("#realNameSearchColums div[name='searchColumsRealName']").html());
		$("#realNameSearchColums").html('');
        datagrid.find("div[name='searchColums']").find("form#userListForm").append($("#tempSearchColums div[name='searchColums']").html());
        $("#tempSearchColums").html(''); */
       // $("input[name='userOrgList.tsDepart.departname']").css("width","200px");
        
        var datagrid = $("#userListtb");
        datagrid.find("div[name='searchColums']").find("form#userListForm").append($("#tempSearchColums div[name='searchColums']").html());
        $("#tempSearchColums").html('');
	}); 
	
	
	
</script>
<div id="realNameSearchColums" style="display: none;">
	<div name="searchColumsRealName">
		<t:userSelect windowWidth="800px" windowHeight="500px" title="用户列表" minFlag="false" resize="false" ></t:userSelect>
	</div>
</div>
<div id="tempSearchColums" style="display: none;">
    <div name="searchColums">
       <t:departSelect minFlag="false" departNameInputWidth="300px" ></t:departSelect>
    </div>
</div>
<%--update-end--Author:zhangguoming  Date:20140827 for：添加 组织机构查询条件--%>
<%--update-end--Author:lijun  Date:20160302 for：组织机构查询条件修改为使用departSelect标签--%>

<div id="main_user_list" class="easyui-layout" fit="true">
    <div region="center" style="padding:0px;border:0px">
		<t:datagrid name="userList" title="common.operation" actionUrl="userController.do?datagrid" 
		    fit="true" fitColumns="true" idField="id" queryMode="group" sortName="createDate" sortOrder="desc" >
			<t:dgCol title="common.id" field="id" hidden="true"></t:dgCol>
			<t:dgCol title="common.username" sortable="false" field="userName" query="true"></t:dgCol>
		    <%--update-start--Author:zhangguoming  Date:20140827 for：通过用户对象的关联属性值获取组织机构名称（多对多关联）--%>
			<%--<t:dgCol title="common.department" field="TSDepart_id" query="true" replace="${departsReplace}"></t:dgCol>--%>
			<t:dgCol title="common.department" sortable="false" field="userOrgList.tsDepart.departname" query="false"></t:dgCol>
		    <%--update-end--Author:zhangguoming  Date:20140827 for：通过用户对象的关联属性值获取组织机构名称（多对多关联）--%>
			<t:dgCol title="common.real.name" field="realName" query="true"></t:dgCol>
			
			<t:dgCol title="common.role" field="userKey" ></t:dgCol>
			<t:dgCol title="common.createby" field="createBy" hidden="true"></t:dgCol>
			<t:dgCol title="common.createtime" field="createDate" formatter="yyyy-MM-dd" hidden="true"></t:dgCol>
			<t:dgCol title="common.updateby" field="updateBy" hidden="true"></t:dgCol>
			<t:dgCol title="common.updatetime" field="updateDate" formatter="yyyy-MM-dd" hidden="true"></t:dgCol>
			<t:dgCol title="common.status" sortable="true" field="status" replace="common.active_1,common.inactive_0,super.admin_-1" ></t:dgCol>
			<t:dgCol title="common.operation" field="opt" width="100"></t:dgCol>
			<t:dgDelOpt title="common.delete" url="userController.do?del&id={id}&userName={userName}"  exp="id#ne#1"/><%-- 1-系统管理员 --%>
			<t:dgToolBar title="common.add.param" langArg="common.user" icon="icon-add" url="userController.do?addorupdate" funname="add"></t:dgToolBar>
			<t:dgToolBar title="common.edit.param" langArg="common.user" icon="icon-edit" url="userController.do?addorupdate" funname="update"></t:dgToolBar>
			<t:dgToolBar title="common.password.reset" icon="icon-edit" funname="changepasswordforuser"></t:dgToolBar>
			<t:dgToolBar title="common.lock.user" icon="icon-edit" url="userController.do?lock&lockvalue=0" funname="lockObj"></t:dgToolBar>
			<t:dgToolBar title="common.unlock.user" icon="icon-edit" url="userController.do?lock&lockvalue=1" funname="unlockObj"></t:dgToolBar>
			<t:dgToolBar title="excelImport" icon="icon-put" funname="ImportXls"></t:dgToolBar>
			<t:dgToolBar title="excelOutput" icon="icon-putout" funname="ExportXls"></t:dgToolBar>
			<t:dgToolBar title="templateDownload" icon="icon-putout" funname="ExportXlsByT"></t:dgToolBar>
		</t:datagrid>
	</div>
</div>
<div data-options="region:'east',
	title:'<t:mutiLang langKey="member.list"/>',
	collapsed:true,
	split:true,
	onExpand : function(){
	
	},
	onCollapse : function() {
	
	}"
	style="width: 400px; overflow: hidden;" id="eastPanel">
    <div class="easyui-panel" style="padding:0px;border:0px" fit="true" border="false" id="userListpanel"></div>
</div>

<script type="text/javascript">

// 下属管理
function querySubordinate(userid){
	
	var title = '<t:mutiLang langKey="member.list"/>';
    $('#main_user_list').layout('expand','east');
    $('#main_user_list').layout('panel','east').panel('setTitle', title);
    $('#main_user_list').layout('panel','east').panel('resize', {width: 350});
    $('#userListpanel').panel("refresh", "userController.do?subordinateList&userid=" + userid);
	
}


// 修改密码
function changepasswordforuser(){
	update("密码重置","userController.do?changepasswordforuser", "userList",550,200);
	
}

// 锁定用户
function lockObj(title,url, id) {
	gridname=id;
	var rowsData = $('#'+id).datagrid('getSelections');
	if (!rowsData || rowsData.length==0) {
		tip('<t:mutiLang langKey="common.please.select.edit.item"/>');
		return;
	}
		url += '&id='+rowsData[0].id;

	/* $.dialog.confirm('<t:mutiLang langKey="common.lock.user.tips"/>', function(){
		lockuploadify(url, '&id');
	}, function(){
		
	}); */
	
	top.layer.confirm('<t:mutiLang langKey="common.lock.user.tips"/>？', function(index){
		lockuploadify(url, '&id');
		top.layer.close(index);
	});
	
}

// 激活用户
function unlockObj(title,url, id) {

	gridname=id;
	var rowsData = $('#'+id).datagrid('getSelections');
	if (!rowsData || rowsData.length==0) {
		tip('<t:mutiLang langKey="common.please.select.edit.item"/>');
		return;
	}
		url += '&id='+rowsData[0].id;

	/* $.dialog.confirm('<t:mutiLang langKey="common.unlock.user.tips"/>', function(){
		lockuploadify(url, '&id');
	}, function(){
	}); */
	
	top.layer.confirm('<t:mutiLang langKey="common.unlock.user.tips"/>？', function(index){
		lockuploadify(url, '&id');
		top.layer.close(index);
	});
	
}


function lockuploadify(url, id) {
	$.ajax({
		async : false,
		cache : false,
		type : 'POST',
		url : url,// 请求的action路径
		error : function() {// 请求失败处理函数
		
		
		},
		success : function(data) {
			var d = $.parseJSON(data);
			if (d.success) {
			var msg = d.msg;
				top.layer.msg(msg);
				reloadTable();
			}
			
		}
	});
}
</script>

<%--update-start--Author:zhangguoming  Date:20140827 for：添加 组织机构查询条件：弹出 选择组织机构列表 相关操作--%>
<%--<a href="#" class="easyui-linkbutton" plain="true" icon="icon-search" onClick="choose_297e201048183a730148183ad85c0001()">选择</a>--%>
<%--<a href="#" class="easyui-linkbutton" plain="true" icon="icon-redo" onClick="clearAll_297e201048183a730148183ad85c0001();">清空</a>--%>
<script type="text/javascript">
//    var windowapi = frameElement.api, W = windowapi.opener;
    function choose_297e201048183a730148183ad85c0001() {
        if (typeof(windowapi) == 'undefined') {
            $.dialog({content: 'url:departController.do?departSelect', zIndex: 2100, title: '<t:mutiLang langKey="common.department.list"/>', lock: true, width: 400, height: 350, opacity: 0.4, button: [
                {name: '<t:mutiLang langKey="common.confirm"/>', callback: clickcallback_297e201048183a730148183ad85c0001, focus: true},
                {name: '<t:mutiLang langKey="common.cancel"/>', callback: function () {
                }}
            ]});
        } else {
            $.dialog({content: 'url:departController.do?departSelect', zIndex: 2100, title: '<t:mutiLang langKey="common.department.list"/>', lock: true, parent: windowapi, width: 400, height: 350, left: '85%', top: '65%', opacity: 0.4, button: [
                {name: '<t:mutiLang langKey="common.confirm"/>', callback: clickcallback_297e201048183a730148183ad85c0001, focus: true},
                {name: '<t:mutiLang langKey="common.cancel"/>', callback: function () {
                }}
            ]});
        }
    }
    function clearAll_297e201048183a730148183ad85c0001() {
        if ($('#departname').length >= 1) {
            $('#departname').val('');
            $('#departname').blur();
        }
        if ($("input[name='departname']").length >= 1) {
            $("input[name='departname']").val('');
            $("input[name='departname']").blur();
        }
        $('#orgIds').val("");
    }
    function clickcallback_297e201048183a730148183ad85c0001() {
        iframe = this.iframe.contentWindow;
        var departname = iframe.getdepartListSelections('text');
        if ($('#departname').length >= 1) {
            $('#departname').val(departname);
            $('#departname').blur();
        }
        if ($("input[name='departname']").length >= 1) {
            $("input[name='departname']").val(departname);
            $("input[name='departname']").blur();
        }
        var id = iframe.getdepartListSelections('id');
        if (id !== undefined && id != "") {
            $('#orgIds').val(id);
            $("input[name='orgIds']").val(id);
        }
    }

	//导入
	function ImportXls() {
		openuploadwin('Excel导入', 'userController.do?upload', "userList");
	}

	//导出
	function ExportXls() {
		JeecgExcelExport("userController.do?exportXls", "userList");
	}

	//模板下载
	function ExportXlsByT() {
		JeecgExcelExport("userController.do?exportXlsByT", "userList");
	}
</script>
<%--update-end--Author:zhangguoming  Date:20140827 for：添加 组织机构查询条件：弹出 选择组织机构列表 相关操作--%>
