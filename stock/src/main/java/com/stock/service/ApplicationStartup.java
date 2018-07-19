package com.stock.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartup implements ApplicationListener<ContextRefreshedEvent> {
	private static final Logger log = Logger.getLogger(ApplicationStartup.class.getName());
	
	@Override
	public void onApplicationEvent(final ContextRefreshedEvent event) {
		//ApplicationContext ctx = event.getApplicationContext();
		log.info("啟動成功" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
	}

}
