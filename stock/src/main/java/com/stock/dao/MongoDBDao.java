package com.stock.dao;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.stock.util.DateUtil;
import com.stock.vo.FinancingVO;
import com.stock.vo.HistoryVO;
import com.stock.vo.SecuritiesVO;
import com.stock.vo.StockVO;

@Component
public class MongoDBDao {
	
	@Resource
    private MongoTemplate mongoTemplate;
	
	public List<StockVO> getAllStockInfo() {
		List<StockVO> list = mongoTemplate.findAll(StockVO.class, "StockMain");
		list.forEach(stockVO -> {
			Query query = new Query();
			query.addCriteria(new Criteria().andOperator(
					Criteria.where("stockId").is(stockVO.getStockId()),
					Criteria.where("transactionDate").gte(DateUtil.before30day())
					));
			mongoTemplate.find(query, SecuritiesVO.class, "SecuritiesTrade");
			stockVO.setSecuritiesTradeList(mongoTemplate.find(query, SecuritiesVO.class, "SecuritiesTrade"));
			
			mongoTemplate.find(query, FinancingVO.class, "FinancingTrade");
			stockVO.setFinancingTradeList(mongoTemplate.find(query, FinancingVO.class, "FinancingTrade"));
			
			mongoTemplate.find(query, HistoryVO.class, "HistoryPrice");
			stockVO.setHistoryPriceList(mongoTemplate.find(query, HistoryVO.class, "HistoryPrice"));
		});
		return list;
	}
	
	public void insertStockInfo(List<StockVO> stockInfoList) {
		for (StockVO stockVO : stockInfoList) {
			if (!mongoTemplate.exists(new Query(Criteria.where("stockId").is(stockVO.getStockId())), StockVO.class, "StockMain")) {
				mongoTemplate.insert(stockVO.getSecuritiesTradeList(), "SecuritiesTrade");
				mongoTemplate.insert(stockVO.getFinancingTradeList(), "FinancingTrade");
				mongoTemplate.insert(stockVO.getHistoryPriceList(), "HistoryPrice");
			}
		}
	}
	
	public void updateAllStockInfo(List<StockVO> stockInfoList) {
		for (StockVO stockVO : stockInfoList) {
			mongoTemplate.insert(stockVO.getSecuritiesTradeList().get(0), "SecuritiesTrade");
			mongoTemplate.insert(stockVO.getFinancingTradeList().get(0), "FinancingTrade");
			mongoTemplate.insert(stockVO.getHistoryPriceList().get(0), "HistoryPrice");
		}
	}
}
