package com.jsrush.bizlog.aop;

import com.jsrush.bizlog.annotation.*;
import com.jsrush.bizlog.constants.BizLogActionType;
import com.jsrush.bizlog.dto.BizActionLogDTO;
import com.jsrush.bizlog.entity.BizActionLog;
import com.jsrush.bizlog.event.BizActionLogAddEventPublisher;
import com.jsrush.bizlog.service.BizActionLogService;
import com.jsrush.common.config.SystemConfig;
import com.jsrush.common.service.SpringContextUtil;
import com.jsrush.util.LogUtil;
import com.jsrush.util.StringHelper;
import com.jsrush.util.SystemUtil;
import net.sf.json.JSONObject;
import ognl.Ognl;
import ognl.OgnlException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * 织入业务日志切面
 * @author sunburst
 *
 */
@Component
@Aspect
public class BizLoggerAspect {

	private static Logger log = LoggerFactory.getLogger(BizLoggerAspect.class);

    public static final String EDP = "@annotation(com.jsrush.bizlog.annotation.ActionLog)";

    @Autowired
    private BizActionLogService bizActionLogService;

    @Autowired
    private SystemConfig systemConfig;

    /**
     * around advice: 处理业务日志
     * @param joinPoint
     * @return
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("rawtypes")
	@Around(EDP)
    public Object logAround(ProceedingJoinPoint joinPoint) throws ClassNotFoundException {
    	ActionLog actionLogMethodCache = null;
    	WithTable withTableMethodCache = null;
    	WithTables withTablesMethodCache = null;
    	WithArgments withJsonsMethodCache = null;

    	Object[] arguments = joinPoint.getArgs();

        String targetName = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        Class targetClass = Class.forName(targetName);
        Method[] method = targetClass.getMethods();

        for (Method m : method) {
            if (m.getName().equals(methodName)) {
                Class[] tmpCs = m.getParameterTypes();
                if (tmpCs.length == arguments.length) {
                	actionLogMethodCache = m.getAnnotation(ActionLog.class);
                	withTableMethodCache = m.getAnnotation(WithTable.class);
                	withTablesMethodCache = m.getAnnotation(WithTables.class);
                	withJsonsMethodCache = m.getAnnotation(WithArgments.class);
                    break;
                }
            }
        }

        Object returnValue = null;
        try {
        	String formatBizContent = getBizContentWithArgs(arguments, withTableMethodCache);
        	if (withTablesMethodCache != null)
        		formatBizContent =getBizContentWithArgs(arguments, withTablesMethodCache);
        	returnValue = joinPoint.proceed(arguments);
        	parseActionLogAnnotion(arguments, actionLogMethodCache, withJsonsMethodCache, returnValue, formatBizContent);
        } catch (Throwable e) {
            e.printStackTrace();
            log.error(LogUtil.stackTraceToString(e));
        }
        return returnValue;
    }

    private String getBizContentWithArgs(Object[] arguments, WithTables withTablesMethodCache){
    	StringBuilder formatBizContent = new StringBuilder();
    	String splitor = withTablesMethodCache.splitor();
		WithTable[] withTables = withTablesMethodCache.withWithTables();
		for (WithTable withTableCache : withTables) {
			String bizContent =getBizContentWithArgs(arguments, withTableCache);
			if (StringHelper.isNotEmpty(bizContent)) {
				if (StringHelper.isNotEmpty(formatBizContent.toString()))
					formatBizContent.append(splitor);
				formatBizContent.append(bizContent);
			}
		}
		return formatBizContent.toString();
    }

    /**
     * 查询业务数据信息
     * @param arguments
     * @param actionLogMethodCache
     * @param withTableMethodCache
     * @param returnValue
     * @return
     */
    private String getBizContentWithArgs(Object[] arguments, WithTable withTableMethodCache){
    	if (withTableMethodCache == null)
    		return null;

        BizActionLogDTO bizActionLogDTO = new BizActionLogDTO();

    	String alias = withTableMethodCache.alias();
    	String splitor = withTableMethodCache.splitor();
    	WithColumn[] withColumns = withTableMethodCache.withColumns();
    	bizActionLogDTO.setWithColumns(withColumns);
    	bizActionLogDTO.setTableSplitor(splitor);

    	StringBuilder  hql = new StringBuilder();
    	hql.append(" select ").append(alias).append(" from ").append(withTableMethodCache.name()).append(" as ").append(alias).append(" where ");
    	ConditionColumn[] cdtColumns = withTableMethodCache.conditionColumns();
    	for (int i = 0; i < cdtColumns.length; i++){
    		ConditionColumn cdtColumn = cdtColumns[i];

    		String argValue = null;
    		String argExpress = cdtColumn.argExpress();
    		int argType = cdtColumn.argType();
    		Object argObj = getArgWithIndex(arguments,cdtColumn.argIndex());
    		if (StringHelper.isNotEmpty(argExpress)){
    			if (ConditionColumn.OBJECT == argType)
    				argValue = (String) getValueWithExpress(argExpress, argObj);
    			else if (ConditionColumn.JSONSTR == argType)
    				argValue = (String) getValueWithExpress(argExpress, SystemUtil.fromObject((String) argObj));
    			else
    				argValue = (String) argObj;
    		} else {
    			argValue = (String) argObj;
    		}

    		hql.append(alias).append(".").append(cdtColumn.name());
    	    if (ConditionColumn.EQUALS.equals(cdtColumn.operator()))
    			hql.append(" = '").append(argValue).append("' ");
    		if (ConditionColumn.INLIST.equals(cdtColumn.operator()))
    			hql.append(" in (").append(argValue).append(")");

    		if (i < cdtColumns.length - 1)
    			hql.append(" and ");

    	}
    	bizActionLogDTO.setFindBizInfoHql(hql);


        String formatBizContent = null;
        try {
        	formatBizContent = bizActionLogService.formatBizContent(bizActionLogDTO);
		} catch (OgnlException e) {
			e.printStackTrace();
			log.error(LogUtil.stackTraceToString(e));
		}

        return formatBizContent;
    }

    /**
     * 解析业务日志注解信息，持久化日志信息
     * @param arguments
     * @param actionLogMethodCache
     * @param withJsonsMethodCache
     * @param returnValue
     * @param formatBizContent
     */
    private void parseActionLogAnnotion(Object[] arguments, ActionLog actionLogMethodCache,  WithArgments withJsonsMethodCache, Object returnValue, String formatBizContent){

    	String actionType = BizLogActionType.TEMPSTR;
        String bizType = "";
        String expectedExp = BizLogActionType.TEMPSTR;
        String bizInfoExp = BizLogActionType.TEMPSTR;

        StringBuilder bizContent = new StringBuilder();

    	if (actionLogMethodCache != null) {
        	actionType = actionLogMethodCache.actionType();
        	bizType = actionLogMethodCache.bizType();
        	expectedExp = actionLogMethodCache.expectedExp();
        	bizInfoExp = actionLogMethodCache.bizInfoExp();
        }

    	if (withJsonsMethodCache != null) {
    		String splitor = withJsonsMethodCache.splitor();
    		WithArgment[] withJsons = withJsonsMethodCache.withArgments();
    		for (int i = 0 ; i < withJsons.length; i++){
    			WithArgment wj = withJsons[i];

    			String argExpress = wj.argExpress();
    			int argType = wj.argType();
    			Object argObj = getArgWithIndex(arguments, wj.argIndex());
    			Object argValue = null;

    			if (StringHelper.isNotEmpty(argExpress)){
    				if (argExpress.equals(WithArgment.RETURNVALUE))
    					argValue = returnValue;
    				else if (WithArgment.OBJECT == argType)
        				argValue = getValueWithExpress(wj.argExpress(), argObj);
        			else if (WithArgment.JSONSTR == argType)
        				argValue = getValueWithExpress(argExpress, SystemUtil.fromObject(argObj));
        			else
        				argValue = argObj;
        		} else {
        				argValue = argObj;
        		}

    			if (WithArgment.ALINK.equals(wj.joinor())){
    				String fileUrl = File.separator + systemConfig.getWebContext() + argValue;
    				String alink = systemConfig.getAlinkTpl().replaceAll("text", wj.text()).replaceAll("url", fileUrl);
    				bizContent.append(alink);
    			} else
    				bizContent.append(wj.text()).append(wj.joinor()).append(argValue);

				if (i < withJsons.length -1)
					bizContent.append(splitor);
    		}

    		if (StringHelper.isNotEmpty(formatBizContent))
    			formatBizContent += bizContent.toString();
    		else
    			formatBizContent = bizContent.toString();

    	}

        if (excuteReturnValExp(expectedExp, returnValue) ){

        	BizActionLog entity =  new BizActionLog();

        	String bizInfo = getBizInfoWithArgs(arguments,bizInfoExp);
    		entity.setActionType(actionType);
			entity.setBizType(bizType);
			entity.setBizInfo(bizInfo);
    		if (StringHelper.isNotEmpty(formatBizContent))
    			entity.setBizContent(formatBizContent);
    		bizActionLogService.setEntityUserInfo(entity);
    		BizActionLogAddEventPublisher publisher = SpringContextUtil.getBean("bizActionLogAddEventPublisher");
			publisher.publishEvent(entity);
        }

    }

    /**
     * 根据位置获取参数信息
     * @param arguments
     * @param index
     * @return
     */
    private Object getArgWithIndex(Object[] arguments,int index){
    	return arguments[index];
    }

    /**
     * 获取业务标识信息
     * @param arguments
     * @param bizInfoExp
     * @return
     */
    private String getBizInfoWithArgs(Object[] arguments, String bizInfoExp){
    	String bizInfo = null;
    	if (StringHelper.isEmpty(bizInfoExp))
    		bizInfo = arguments[0].toString();
    	else {
    		StringBuilder argVal = new StringBuilder();
    		String[] argsIndex = bizInfoExp.split(ActionLog.BIZINFOEXP_SPLITOR);
    		for (int i = 0 ; i < argsIndex.length; i++) {
				Object argWithIndex = getArgWithIndex(arguments, Integer.valueOf(argsIndex[i].toString()));
				if (argWithIndex != null)
					argVal.append((String) argWithIndex);
				if (i < argsIndex.length - 1)
					argVal.append(ActionLog.BIZINFOEXP_SPLITOR);
			}
    		bizInfo = argVal.toString();
    	}
    	return bizInfo;
    }

    /**
     * 执行表达式：对象类型参数
     * @param expression
     * @param root
     * @return
     */
    public Object getValueWithExpress(String expression, Object root){
    	try {
			return Ognl.getValue(expression, root);
		} catch (OgnlException e) {
			e.printStackTrace();
			log.error(LogUtil.stackTraceToString(e));
		}
    	return null;
    }

    /**
     * 获取jsonObject中的数据
     * @param expression
     * @param jsonObject
     * @return
     */
    public Object getValueWithExpress(String expression, JSONObject jsonObject){
   		return jsonObject.getString(expression);
    }

    /**
     * 初始化ognl参数context
     * @param returnValue
     * @return
     */
    private Object initContext(Object returnValue) {
    	HashMap<String, Object> context = new HashMap<String, Object>();
    	context.put(ActionLog.RETURNVALUE, returnValue);
    	return context;
    }

    /**
     * 执行表达式：判定方法返回值是否符合条件
     * @param expression	eg. "returnValue == 1" or "returnValue > 2 && returnValue < 4"
     * @param returnValue	eg. 1 or 6
     * @return
     */
    public Boolean excuteReturnValExp(String expression, Object returnValue){
    	if (StringHelper.isEmpty(expression))
    		return true;
    	Object context = initContext(returnValue);
    	return (Boolean) getValueWithExpress(expression, context);
    }

}
