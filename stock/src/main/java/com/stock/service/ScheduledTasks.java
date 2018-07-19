package com.stock.service;


import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.stock.dao.MongoDBDao;

@Component
public class ScheduledTasks {
	private static final Logger log = Logger.getLogger(ScheduledTasks.class.getName());
	
	@Autowired
	private ApplicationContext context;
	
	@Autowired
	MongoDBDao mongoDBDao;
	
	@Scheduled(cron = "0 30 17 ? * MON-FRI")
    public void stockInfoTask() {
		OpenData openData = context.getBean(OpenData.class);
		try {
			openData.updateStockInfo();
		} catch (Exception e) {
			log.info("排程發生錯誤，原因為：" + e.getMessage());
		}
    }
	
}
