package com.jsrush.bizlog.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.jsrush.bizlog.service.BizActionLogService;
import com.jsrush.util.LogUtil;

/**
 * 事件监听器:新增业务操作日志
 * @author sunburst
 *
 */
@Component
public class BizActionLogAddListener implements ApplicationListener<BizActionLogAddEvent>{

	private static Logger log = LoggerFactory.getLogger(BizActionLogAddListener.class);
			
	@Autowired
	private BizActionLogService bizActionLogService;
	
	@Async
	@Override
	public void onApplicationEvent(BizActionLogAddEvent event) {
		long start = System.currentTimeMillis();
		log.info("start call bizActionLogService.save() "+event.getBizActionLog().toString());
		try {
			bizActionLogService.save(event.getBizActionLog());
			
//			Thread.sleep(1000*10);//模拟耗时操作
			
		} catch (Exception e) {
			e.printStackTrace();
			log.error(LogUtil.stackTraceToString(e));
		}
		log.info("end call bizActionLogService.save() , total time:" + (System.currentTimeMillis() - start));
	}
	
}
