package com.stock.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
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
					Criteria.where("transactionDate").gte(DateUtil.before10day())
					));
			List<SecuritiesVO> securitiesVOList = mongoTemplate.find(query, SecuritiesVO.class, "SecuritiesTrade");
			Collections.sort(securitiesVOList, Comparator.comparing(SecuritiesVO::getTransactionDate).reversed());
			stockVO.setSecuritiesTradeList(securitiesVOList);
			
			List<FinancingVO> financingVOList = mongoTemplate.find(query, FinancingVO.class, "FinancingTrade");
			Collections.sort(financingVOList, Comparator.comparing(FinancingVO::getTransactionDate).reversed());
			stockVO.setFinancingTradeList(financingVOList);
			
			List<HistoryVO> historyVOList = mongoTemplate.find(query, HistoryVO.class, "HistoryPrice");
			Collections.sort(historyVOList, Comparator.comparing(HistoryVO::getTransactionDate).reversed());
			stockVO.setHistoryPriceList(historyVOList);
			
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
	
	public void updateStockInfo(List<StockVO> stockInfoList) {
		for (StockVO stockVO : stockInfoList) {
			mongoTemplate.insert(stockVO.getSecuritiesTradeList(), "SecuritiesTrade");
			mongoTemplate.insert(stockVO.getFinancingTradeList(), "FinancingTrade");
			mongoTemplate.insert(stockVO.getHistoryPriceList(), "HistoryPrice");
		}
	}
	
	public List<StockVO> getLatestStockInfo() {
		List<StockVO> list = mongoTemplate.findAll(StockVO.class, "StockMain");
		String firstStockId = list.get(0).getStockId();
		Aggregation findLatestDateAgg = Aggregation.newAggregation(
				Aggregation.match(Criteria.where("stockId").is(firstStockId)),
				Aggregation.group("stockId").max("transactionDate").as("latestDate")
				);
		AggregationResults<HistoryVO> groupResults = mongoTemplate.aggregate(findLatestDateAgg, "HistoryPrice", HistoryVO.class);
		String latestDate = groupResults.getMappedResults().get(0).getLatestDate();
		
		List<SecuritiesVO> securitiesList = mongoTemplate.find(new Query(Criteria.where("transactionDate").is(latestDate)), SecuritiesVO.class, "SecuritiesTrade");
		List<HistoryVO> historyList = mongoTemplate.find(new Query(Criteria.where("transactionDate").is(latestDate)), HistoryVO.class, "HistoryPrice");
		List<FinancingVO> financingList = mongoTemplate.find(new Query(Criteria.where("transactionDate").is(latestDate)), FinancingVO.class, "FinancingTrade");
		
		list.forEach(stockVO -> {
			String stockId = stockVO.getStockId();
			stockVO.setSecuritiesTradeList(new ArrayList<>());
			stockVO.setHistoryPriceList(new ArrayList<>());
			stockVO.setFinancingTradeList(new ArrayList<>());
			
			for (SecuritiesVO securitiesVO : securitiesList) {
				if (StringUtils.equals(stockId, securitiesVO.getStockId())) {
					stockVO.getSecuritiesTradeList().add(securitiesVO);
					break;
				}
			}
			for (HistoryVO historyVO : historyList) {
				if (StringUtils.equals(stockId, historyVO.getStockId())) {
					stockVO.getHistoryPriceList().add(historyVO);
					break;
				}
			}
			for (FinancingVO financingVO : financingList) {
				if (StringUtils.equals(stockId, financingVO.getStockId())) {
					stockVO.getFinancingTradeList().add(financingVO);
					break;
				}
			}
		});
		return list;
	}
}
