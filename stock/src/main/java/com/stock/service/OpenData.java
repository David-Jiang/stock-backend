package com.stock.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.stock.dao.MongoDBDao;
import com.stock.vo.FinancingVO;
import com.stock.vo.HistoryVO;
import com.stock.vo.SecuritiesVO;
import com.stock.vo.StockVO;

@Component
public class OpenData {
	
	@Autowired
	MongoDBDao mongoDBDao;
	
	private static DecimalFormat format_00 = new DecimalFormat("0.00");
	
	public List<StockVO> getStockInfo() throws Exception {
		List<StockVO> stockInfoList = null;
		try {
			stockInfoList = mongoDBDao.getAllStockInfo();
		} catch (Exception e) {
			throw e;
		}
		return stockInfoList;
	}
	
	public void insertStockInfo(String parameter) throws Exception {
		List<String> stockIdArr = new ArrayList<>();
		stockIdArr.add(parameter);
		
		String[] urlPathArr = {"https://stock.wearn.com/netbuy.asp?kind=", "https://stock.wearn.com/cdata.asp?kind=", "https://stock.wearn.com/acredit.asp?kind="};
		HttpURLConnection conn = null;
		BufferedReader buffer = null;
		
		List<StockVO> stockInfoList = new ArrayList<>();
		try {
			for (String stockId : stockIdArr) {
				StockVO stockVO = new StockVO();
				
				for (String urlPath: urlPathArr) {
					List<SecuritiesVO> securitiesTradeList = new ArrayList<>();
					List<HistoryVO> historyPriceList = new ArrayList<>();
					List<FinancingVO> financingTradeList = new ArrayList<>();
					
					URL url = new URL(urlPath + stockId);
					conn = (HttpURLConnection) url.openConnection();
				    conn.connect();
				    
				    String line = null;
				    StringBuffer str = new StringBuffer();
				    buffer = new BufferedReader(new InputStreamReader(conn.getInputStream(), "MS950"));
				    while((line = buffer.readLine()) != null) {
				    	str.append(line);
				    }
				    int count = 0;
				    String[] sourceArr = str.toString().split("<tr class=\"stockalllistbg");
				    for (String source : sourceArr) {
				    	if (count > 0) {
				    		String temp = source.trim().replaceAll("\"1\">", "");
				    		temp = temp.replaceAll("1\">", "");
					    	temp = temp.replaceAll("2\">", "");
					    	temp = temp.replaceAll("</tr>", "");
					    	temp = temp.replaceAll("</td>", "");
					    	temp = temp.replaceAll("&nbsp;", "");
					    	temp = temp.replaceAll("<td align=\"center\">", "");
					    	temp = temp.replaceAll("\\t", "");
					    	String[] tempArr = temp.split("<td align=\"right\">");
					    	if (count == sourceArr.length - 1) {
					    		if (urlPath.indexOf("netbuy") > -1) {
					    			tempArr[3] = tempArr[3].split("</table>")[0];
					    		} else {
					    			tempArr[5] = tempArr[5].split("</table>")[0];
					    		}
					    	}
					    	if (urlPath.indexOf("acredit") > -1) {
					    		tempArr[2] = tempArr[2].split("<span")[0];
				    			tempArr[4] = tempArr[4].split("<span")[0];
					    	}
					    	if (urlPath.indexOf("netbuy") > -1) {
					    		securitiesTradeList.add(changeToSecuritiesVO(tempArr, stockId));
					    	} else if (urlPath.indexOf("cdata") > -1) {
					    		historyPriceList.add(changeToHistoryVO(tempArr, stockId));
					    	} else {
					    		financingTradeList.add(changeToFinancingVO(tempArr, stockId));
					    	}
				    	} else {
				    		if (StringUtils.isBlank(stockVO.getStockId())) {
				    			String stockName = sourceArr[0].split("<font size=\"3\">")[1].split("</font>")[0].replaceAll(stockId, "").trim();
					    		stockVO.setStockId(stockId);
					    		stockVO.setStockName(stockName);
				    		}
				    	}
				    	count++;
				    }
				    if (urlPath.indexOf("netbuy") > -1) {
				    	stockVO.setSecuritiesTradeList(securitiesTradeList);
			    	} else if (urlPath.indexOf("cdata") > -1) {
			    		stockVO.setHistoryPriceList(historyPriceList);
			    	} else {
			    		stockVO.setFinancingTradeList(financingTradeList);
			    	}
				    buffer.close();
					conn.disconnect();
					Thread.sleep(5000);
				}
				
				stockInfoList.add(stockVO);
			}
			for (StockVO stockVO : stockInfoList) {
				List<HistoryVO> historyPriceList = stockVO.getHistoryPriceList();
				for (int i = 0; i < historyPriceList.size(); i++) {
					if ((i + 1) != historyPriceList.size()) {
						double endPriceNew = Double.valueOf(historyPriceList.get(i).getEndPrice().replaceAll(",", ""));
						double endPriceOld = Double.valueOf(historyPriceList.get(i + 1).getEndPrice().replaceAll(",", ""));
						double endPrice = endPriceNew - endPriceOld;
						historyPriceList.get(i).setWavePrice(format_00.format(endPrice));
					} else {
						historyPriceList.get(i).setWavePrice("0");
					}
				}
			}
			mongoDBDao.insertStockInfo(stockInfoList);
		} catch (Exception e) {
			buffer.close();
			conn.disconnect();
			throw e;
		}
	}
	
	public void updateStockInfo() throws Exception {
		String[] urlPathArr = {"https://stock.wearn.com/netbuy.asp?kind=", "https://stock.wearn.com/cdata.asp?kind=", "https://stock.wearn.com/acredit.asp?kind="};
		HttpURLConnection conn = null;
		BufferedReader buffer = null;
		try {
			List<StockVO> stockInfoList = mongoDBDao.getLatestStockInfo();
			String latestDate = stockInfoList.get(0).getHistoryPriceList().get(0).getTransactionDate();
			
			for (StockVO stockVO : stockInfoList) {
				String stockId = stockVO.getStockId();
				List<SecuritiesVO> securitiesTradeList = stockVO.getSecuritiesTradeList();
				List<HistoryVO> historyPriceList = stockVO.getHistoryPriceList();
				List<FinancingVO> financingTradeList = stockVO.getFinancingTradeList();
				
				for (String urlPath : urlPathArr) {
					URL url = new URL(urlPath + stockId);
					conn = (HttpURLConnection) url.openConnection();
				    conn.connect();
				    
				    String line = null;
				    StringBuffer str = new StringBuffer();
				    buffer = new BufferedReader(new InputStreamReader(conn.getInputStream(), "MS950"));
				    while((line = buffer.readLine()) != null) {
				    	str.append(line);
				    }
				    
				    int count = 0;
				    String[] sourceArr = str.toString().split("<tr class=\"stockalllistbg");
				    for (String source : sourceArr) {
				    	if (count > 0) {
				    		String temp = source.trim().replaceAll("\"1\">", "");
				    		temp = temp.replaceAll("1\">", "");
					    	temp = temp.replaceAll("2\">", "");
					    	temp = temp.replaceAll("</tr>", "");
					    	temp = temp.replaceAll("</td>", "");
					    	temp = temp.replaceAll("&nbsp;", "");
					    	temp = temp.replaceAll("<td align=\"center\">", "");
					    	temp = temp.replaceAll("\\t", "");
					    	String[] tempArr = temp.split("<td align=\"right\">");
					    	if (count == sourceArr.length - 1) {
					    		if (urlPath.indexOf("netbuy") > -1) {
					    			tempArr[3] = tempArr[3].split("</table>")[0];
					    		} else {
					    			tempArr[5] = tempArr[5].split("</table>")[0];
					    		}
					    	}
					    	if (urlPath.indexOf("acredit") > -1) {
					    		tempArr[2] = tempArr[2].split("<span")[0];
				    			tempArr[4] = tempArr[4].split("<span")[0];
					    	}
					    	String transactionDate = tempArr[0].trim().replaceAll("/", "");
					    	if (transactionDate.compareTo(latestDate) <= 0) {
					    		continue;
					    	}
					    	if (urlPath.indexOf("netbuy") > -1) {
					    		securitiesTradeList.add(changeToSecuritiesVO(tempArr, stockId));
					    	} else if (urlPath.indexOf("cdata") > -1) {
					    		historyPriceList.add(changeToHistoryVO(tempArr, stockId));
					    	} else {
					    		financingTradeList.add(changeToFinancingVO(tempArr, stockId));
					    	}
				    	}
				    	count++;
				    }
				}
				
			    Collections.sort(historyPriceList, Comparator.comparing(HistoryVO::getTransactionDate).reversed());
			    stockVO.setHistoryPriceList(historyPriceList);
			    for (int i = 0; i < historyPriceList.size(); i++) {
			    	if (StringUtils.isEmpty(historyPriceList.get(i).getWavePrice())) {
			    		if ((i + 1) != historyPriceList.size()) {
							double endPriceNew = Double.valueOf(historyPriceList.get(i).getEndPrice().replaceAll(",", ""));
							double endPriceOld = Double.valueOf(historyPriceList.get(i + 1).getEndPrice().replaceAll(",", ""));
							double endPrice = endPriceNew - endPriceOld;
							historyPriceList.get(i).setWavePrice(format_00.format(endPrice));
						} else {
							historyPriceList.get(i).setWavePrice("0");
						}
			    	}
				}
			    securitiesTradeList.removeIf(vo -> StringUtils.equals(vo.getTransactionDate(), latestDate));
			    historyPriceList.removeIf(vo -> StringUtils.equals(vo.getTransactionDate(), latestDate));
			    financingTradeList.removeIf(vo -> StringUtils.equals(vo.getTransactionDate(), latestDate));
			    
			    buffer.close();
				conn.disconnect();
				Thread.sleep(2000);
			}
			
			mongoDBDao.updateStockInfo(stockInfoList);
		} catch (Exception e) {
			buffer.close();
			conn.disconnect();
			throw e;
		}
	}
	
	private SecuritiesVO changeToSecuritiesVO(String[] source, String stockId) {
		SecuritiesVO securitiesVO = new SecuritiesVO();
		securitiesVO.setStockId(stockId);
    	securitiesVO.setTransactionDate(source[0].trim().replaceAll("/", ""));
    	securitiesVO.setInvestAmount(Integer.parseInt(source[1].trim().replaceAll(",", "")));
    	securitiesVO.setNativeAmount(Integer.parseInt(source[2].trim().replaceAll(",", "")));
    	securitiesVO.setForeignAmount(Integer.parseInt(source[3].trim().replaceAll(",", "")));
    	securitiesVO.setTotalAmount(securitiesVO.getInvestAmount() + securitiesVO.getNativeAmount() + securitiesVO.getForeignAmount());
    	return securitiesVO;
	}
	
	private HistoryVO changeToHistoryVO(String[] source, String stockId) {
		HistoryVO historyVO = new HistoryVO();
		historyVO.setStockId(stockId);
		historyVO.setTransactionDate(source[0].trim().replaceAll("/", ""));
		historyVO.setStartPrice(source[1].trim());
		historyVO.setHighPrice(source[2].trim());
		historyVO.setLowPrice(source[3].trim());
		historyVO.setEndPrice(source[4].trim());
		historyVO.setTransactionAmount(source[5].trim());
    	return historyVO;
	}
	
	private FinancingVO changeToFinancingVO(String[] source, String stockId) {
		FinancingVO financingVO = new FinancingVO();
		financingVO.setStockId(stockId);
		financingVO.setTransactionDate(source[0].trim().replaceAll("/", ""));
		financingVO.setMarginAmount(source[1].trim());
		financingVO.setMarginBalance(source[2].trim());
		financingVO.setShortAmount(source[3].trim());
		financingVO.setShortBalance(source[4].trim());
		return financingVO;
	}
}
