<%@ page language="java" contentType="text/html; charset=UTF-8"  pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"  %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<!DOCTYPE html>
<html>
<head>
	<title>选课成绩管理 -  <spring:message code="app.title"></spring:message> </title>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
</head>
<body>
	<div class="easyui-panel" title='<spring:message code="common.search.condition"></spring:message>' >
		<form class="easyui-condition-form" id="listForm" >
			<table>
				<tr>
					<td>姓名</td>
					<td>
						<input name="studentName" class="easyui-textbox" type="text" />
					</td>
					<td>学号</td>
					<td>
						<input name="studentNo" class="easyui-textbox" type="text" />
					</td>
					<td>课程</td>
					<td>
						<select class="easyui-combobox" name="courseId" style="width: 220px;">
							<option value="">请选择</option>
							<c:if test="${courseList != null }">
								<c:forEach items="${courseList }" var="item">
								<option value="${item[0] }">${item[1] }</option>
								</c:forEach>
							</c:if>
						</select>
					</td>
				</tr>
			</table>
		</form>
		<div style="text-align:center;padding:5px">
			<!-- data-hotkey-event="click"  -->
	    	<a  href="javascript:void(0)" class="easyui-linkbutton easyui-hotkey" data-hotkey-keycode="13" data-options="iconCls:'icon-search'" style="width:80px" id="searchBtn" >查询</a>
	    	<a  href="javascript:void(0)" class="easyui-linkbutton" data-options="iconCls:'icon-undo'" style="width:80px" id="clearBtn">清空</a>
	   </div>
	</div>
	
	<table id="dataGrid" class="easyui-datagrid"></table>
	
	<div id="takeCourseDlg" class="easyui-dialog" style="width:620px;"
		data-options="iconCls:'icon-save', title:'在线选课',buttons:'#takeCourseDlg-buttons',modal:true,closed:true">
		<form class="easyui-condition-form" id="takeCourseForm" >
			<input name="id" type="hidden" />
			<table border="0" cellpadding="4" style="width: 100%;">
				<tr>
					<td>学生</td>
					<td>
						<select class="easyui-combobox" name="studentId" style="width: 220px;" data-options="required:true, missingMessage:'请输入必填项'"  >
							<c:if test="${studentList != null }">
								<c:forEach items="${studentList }" var="item">
								<option value="${item[0] }">${item[1] } - ${item[2] }</option>
								</c:forEach>
							</c:if>
						</select>
					</td>
					
					<td>课程</td>
					<td>
						<select class="easyui-combobox" name="courseId" style="width: 220px;" data-options="required:true, missingMessage:'请输入必填项'"  >
							<c:if test="${courseList != null }">
								<c:forEach items="${courseList }" var="item">
								<option value="${item[0] }">${item[1] }</option>
								</c:forEach>
							</c:if>
						</select>
					</td>
										
				</tr>
				 
			</table>
		</form>
	</div>
	<div id="takeCourseDlg-buttons">
		<a href="javascript:void(0);" class="easyui-linkbutton" data-options="iconCls:'icon-save'" id="takeCourseBtn">保存</a>
		<a href="javascript:void(0);" class="easyui-linkbutton" data-options="iconCls:'icon-cancel'" id="cancelBtn">取消</a>
	</div>
	
	
	<div id="saveGradeDlg" class="easyui-dialog" style="width:420px;"
		data-options="iconCls:'icon-save', title:'成绩录入',buttons:'#saveGradeDlg-buttons',modal:true,closed:true">
		<form class="easyui-condition-form" id="saveGradeForm" >
			<table border="0" cellpadding="4" style="width: 100%;">
				<tr>
					<td>考试成绩</td>
					<td>
						<input name="grade" class="easyui-textbox" type="text" data-options="required:true, missingMessage:'请输入必填项'"  />
					</td>
				</tr>
			</table>
		</form>
	</div>
	<div id="saveGradeDlg-buttons">
		<a href="javascript:void(0);" class="easyui-linkbutton" data-options="iconCls:'icon-save'" id="saveGradeBtn">保存</a>
		<a href="javascript:void(0);" class="easyui-linkbutton" data-options="iconCls:'icon-cancel'" id="saveGradeCancelBtn">取消</a>
	</div>
	
	<jsp:include page="/include_biz.jsp"></jsp:include>
	<script type="text/javascript" src="${ctx }/static/biz/sims/takecourse.js"></script>
</body>
</html>