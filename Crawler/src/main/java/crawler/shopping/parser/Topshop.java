package crawler.shopping.parser;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.HasParentFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import crawler.shopping.data.XmlData;
import crawler.shopping.db.MySqlConnector;


public class Topshop implements ParserImpl {

	private Connection sqlConnection;
	private Logger log = Logger.getLogger(this.getClass());
	private NodeList rootNodeList;
	private String originalPrice, discountPrice, color;
	
	private final int REPEAT_COUNT = 5;
	
	public Topshop() {
		
	}
	
	@Override
	public String getProductName(String html){
		NodeList nodeList = this.rootNodeList;
		NodeFilter divFilter = new TagNameFilter("div");
		NodeFilter h1Filter = new TagNameFilter("h1");
		HasAttributeFilter attributeFilter = new HasAttributeFilter("id", "product_tab_1");
		
		HasParentFilter parentFilter = new HasParentFilter(new AndFilter(divFilter, attributeFilter), true);
		nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(h1Filter, parentFilter), true);
		if(nodeList.size() < 1)
			return null;
		
		return nodeList.elementAt(0).getFirstChild().getText();
	}
	
	private void getPriceAndColor(){
		NodeList nodeList = this.rootNodeList;
		NodeFilter nodeFilter = new TagNameFilter("li");
		NodeFilter spanFilter = new TagNameFilter("span");
		HasAttributeFilter attributeFilter = new HasAttributeFilter("class");
		HasParentFilter parentFilter = new HasParentFilter(new AndFilter(nodeFilter, attributeFilter), true);
		nodeList = nodeList.extractAllNodesThatMatch(parentFilter, true);
		
		if(nodeList.size() > 0){
			originalPrice = nodeList.elementAt(0).getFirstChild().getText();
			
			attributeFilter = new HasAttributeFilter("class", "now_price product_price");
			parentFilter = new HasParentFilter(new AndFilter(nodeFilter, attributeFilter), true);
			nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, parentFilter), true);
			discountPrice = nodeList.elementAt(0).getFirstChild().getText();
		} else {
			attributeFilter = new HasAttributeFilter("class", "product_price");
			parentFilter = new HasParentFilter(new AndFilter(nodeFilter, attributeFilter), true);
			nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, parentFilter), true);
			discountPrice = originalPrice = nodeList.elementAt(0).getFirstChild().getText();
		}
		
		attributeFilter = new HasAttributeFilter("class", "product_colour");
		parentFilter = new HasParentFilter(new AndFilter(nodeFilter, attributeFilter), true);
		nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, parentFilter), true);
		color = nodeList.elementAt(0).getFirstChild().getText();;
		
		
		if(discountPrice != null)
			discountPrice = discountPrice.trim();
		if(originalPrice != null)
			originalPrice = originalPrice.trim();
	}
	
	public boolean isAvailable(String html){
		if(html.contains("This item is not available.") == true){
			return false;
		}
		return true;
	}
	
	public String getProductName(){
		NodeList nodeList = this.rootNodeList;
		NodeFilter spanFilter = new TagNameFilter("input");
		HasAttributeFilter attributeFilter = new HasAttributeFilter("type", "hidden");
		HasAttributeFilter attributeFilter2 = new HasAttributeFilter("name", "/nm/formhandler/ProdHandler.productId");
		nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, new AndFilter(attributeFilter, attributeFilter2)), true);
		if(nodeList.size() < 1)
			return null;
		return ((Tag)nodeList.elementAt(0)).getAttribute("value");
	}
	
	@Override
	public void run(XmlData xmlData, String startDate) {

		
		MySqlConnector mysqlConnector = new MySqlConnector();
		sqlConnection = mysqlConnector.getConnection();
		PreparedStatement pstmt = null;
		
		
		String html = getHtml("http://www.shopstyle.com/action/apiVisitRetailer?pid=sugar&id=" + xmlData.getId());
		Parser parser = Parser.createParser(html, "UTF-8");
		try {
			rootNodeList = parser.parse(null);
		} catch (ParserException e) {
			log.error("Parsing Exception");
			e.printStackTrace();
		}
		log.error(html);
		getPriceAndColor();
		String productName = getProductName();
		
		if(isAvailable(html) == false){
			String sql = "insert into shopping(product_id, retailer, brand, product_name, isAvailable, start_date) values (?, ?, ?, ?, ?, ?)";
			try {
				pstmt = sqlConnection.prepareStatement(sql);
				pstmt.setString(1, xmlData.getId());
				pstmt.setString(2, xmlData.getRetailer());
				pstmt.setString(3, xmlData.getBrand());
				pstmt.setString(4, null);
				pstmt.setString(5, "false");
				pstmt.setString(6, startDate);
				pstmt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally{
				try {
					if(pstmt != null)
						pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			return ;
		}
		
		
		int start = html.indexOf("new Attributes") + "new Attributes".length() + 1;
		int end = html.indexOf(");", start);
		String str = html.substring(start, end);
		start = str.indexOf(",[") + 2;
		end = str.indexOf("]", start);
		String sizeString = str.substring(start, end);
		sizeString = sizeString.replace("'", "");
		
		StringTokenizer sizeToken = new StringTokenizer(sizeString, ",");
		
		start = str.indexOf(",[") + 2;
		end = str.indexOf("]", start);
		String quantitytring = str.substring(start, end);
		quantitytring = quantitytring.replace("'", "");
		
		
		ArrayList<String> sizeList = new ArrayList<>();
		while(sizeToken.hasMoreElements()){
			String size = sizeToken.nextToken();
			sizeList.add(size);
		}
		
		String sql = "insert into shopping(product_id, retailer, brand, product_name, original_price, discounted_price, size, color, quantity, isAvailable, start_date) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		try {
			pstmt = sqlConnection.prepareStatement(sql);
		} catch (SQLException e1) {
			log.error(e1.toString());
			e1.printStackTrace();
		}
		
		int i = 0;
		StringTokenizer quantityToken = new StringTokenizer(quantitytring, ",");
		while(quantityToken.hasMoreElements()){
			String size = sizeList.get(i);
			String quantity = quantityToken.nextToken();
			String available = "true";
			
			if(quantity == "0")
				available = "false";
			
			try {
				pstmt.setString(1, xmlData.getId());
				pstmt.setString(2, xmlData.getRetailer());
				pstmt.setString(3, xmlData.getBrand());
				pstmt.setString(4, productName);
				
				pstmt.setString(5, originalPrice);
				pstmt.setString(6, discountPrice);
				pstmt.setString(7, size);
				pstmt.setString(8, quantity);
				pstmt.setString(9, "0");
				pstmt.setString(10, available);
				pstmt.setString(11, startDate);
				pstmt.executeUpdate();
				
			} catch (SQLException e) {
				log.error(e.toString());
				e.printStackTrace();
			}
			i++;
		}
		
		if(pstmt != null){
			try {
				pstmt.close();
			} catch (SQLException e) {
				log.error(e.toString());
				e.printStackTrace();
			}
		}
		
	}
	
	@Override
	public String getHtml(String hostUrl) {
		HttpMethod method = new GetMethod(hostUrl);
		HttpClient client = new HttpClient();
		client.getHttpConnectionManager().getParams().setConnectionTimeout(5 * 1000);
		
		int status;
		try {
			status = client.executeMethod(method);
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String str = null;
		try {
			str = method.getResponseBodyAsString();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		int start = str.indexOf("window.location.replace('");
		start += "window.location.replace('".length();
		int end = str.indexOf("'", start);
		String url = (String)str.subSequence(start, end);
		
		url = url.replace("\\", "");
		method = new GetMethod(url);
		
			//method 로 html 문서를 요청한다
			
		try {
			status = client.executeMethod(method);
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

			// HTML을 요청하는 Thread 를 돌리기 위한 함수이다.
		try {
			str = method.getResponseBodyAsString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return str;
	}

}
