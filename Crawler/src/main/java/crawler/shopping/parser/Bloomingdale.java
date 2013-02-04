package crawler.shopping.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import crawler.shopping.data.XmlData;
import crawler.shopping.db.MySqlConnector;


public class Bloomingdale implements ParserImpl {

	private Connection sqlConnection;
	private Logger log = Logger.getLogger(this.getClass());
	private Map<String, String> httpParams;
	private NodeList rootNodeList;
	private XmlData xmlData;
	private String startDate, price, discountPrice, productName;
	
	private final int REPEAT_COUNT = 5;
	
	public Bloomingdale() {
		httpParams = new HashMap<String, String>();
	}
	
	
	@Override
	public String getProductName(String html){
		NodeList nodeList = this.rootNodeList;
		NodeFilter spanFilter = new TagNameFilter("div");
		HasAttributeFilter attributeFilter = new HasAttributeFilter("class", "product-designer");
		nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, attributeFilter), true);
		spanFilter = new TagNameFilter("h2");
		nodeList = nodeList.extractAllNodesThatMatch(spanFilter, true);
		if(nodeList.size() < 1)
			return null;
		return nodeList.elementAt(0).getFirstChild().getText();
	}
	
	public NodeList isVariation(){
		NodeList nodeList = this.rootNodeList;
		NodeFilter spanFilter = new TagNameFilter("div");
		HasAttributeFilter attributeFilter = new HasAttributeFilter("class", "product-variations");
		nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, attributeFilter), true);
		spanFilter = new TagNameFilter("div");
		attributeFilter = new HasAttributeFilter("class", "image-thumbnail-container");
		nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, attributeFilter), true);
		
		if(nodeList.size() < 1)
			return null;
		return nodeList;
	}
	
	public boolean isAvailable(String html){
		TagNameFilter tagFilter = new TagNameFilter("div");
		HasAttributeFilter atFitler = new HasAttributeFilter("class", "soldout-label");
		NodeList nodeList = this.rootNodeList.extractAllNodesThatMatch(new AndFilter(tagFilter, atFitler), true);
		if(nodeList.size() > 0){
			if(nodeList.elementAt(0).getFirstChild().getText().contentEquals("SOLD OUT") == true)
				return false;
		}
		return true;
	}
	
	private void parsing(String html, String color){
		PreparedStatement pstmt = null;
		NodeList nodeList = null;
		Parser parser = Parser.createParser(html, "UTF-8");
		try {
			nodeList = parser.parse(null);
		} catch (ParserException e) {
			log.error("Parsing Exception");
			e.printStackTrace();
			return;
		}
		
		NodeFilter tagFilter = new TagNameFilter("ul");
		HasAttributeFilter attributeFilter = new HasAttributeFilter("class", "product-size options");
		
		nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(tagFilter, attributeFilter), true);
		tagFilter = new TagNameFilter("li");
		nodeList = nodeList.extractAllNodesThatMatch(tagFilter, true);
		String sql = "insert into shopping(product_id, retailer, brand, product_name, original_price, discounted_price, size, color, quantity, isAvailable, start_date) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		for(int i = 0; i < nodeList.size(); i++){
			Tag t = (Tag) nodeList.elementAt(i);
			String size = t.getAttribute("data-size");
			String quantity = t.getAttribute("data-stock");
			
			try {
				pstmt = sqlConnection.prepareStatement(sql);
				pstmt.setString(1, xmlData.getId());
				pstmt.setString(2, xmlData.getRetailer());
				pstmt.setString(3, xmlData.getBrand());
				pstmt.setString(4, productName);
				pstmt.setString(5, price);
				pstmt.setString(6, discountPrice);
				pstmt.setString(7, size);
				pstmt.setString(8, color);
				pstmt.setString(9, quantity);
				if(quantity != "0")
					pstmt.setString(10, "true");
				else
					pstmt.setString(10, "false");
				pstmt.setString(11, startDate);
				pstmt.executeUpdate();
			} catch (SQLException e1) {
				log.error(e1.toString());
				e1.printStackTrace();
			}
		}
		
		try {
			if(pstmt != null)
				pstmt.close();
		} catch (SQLException e) {
			log.error(e.toString());
			e.printStackTrace();
		}
		
		
	}
	
	@Override
	public void run(XmlData xmlData, String startDate) {
		this.xmlData = xmlData;
		this.startDate = startDate;

		MySqlConnector mysqlConnector = new MySqlConnector();
		sqlConnection = mysqlConnector.getConnection();
		
		//xmlData.setId("226058790");
		String html = getHtml("http://www.shopstyle.com/action/apiVisitRetailer?pid=sugar&id=" + xmlData.getId());
		
		Parser parser = Parser.createParser(html, "UTF-8");
		try {
			rootNodeList = parser.parse(null);
		} catch (ParserException e) {
			log.error("Parsing Exception");
			e.printStackTrace();
		}
		
		this.productName = getProductName(html);
		
		TagNameFilter tagFilter = new TagNameFilter("span");
		HasAttributeFilter attributeFilter = new HasAttributeFilter("class", "priceBig");
		NodeList nodeList = this.rootNodeList.extractAllNodesThatMatch(new AndFilter(tagFilter, attributeFilter), true);
		if(nodeList.size() > 0){
			price = nodeList.elementAt(0).getFirstChild().getText();
		}
		
		tagFilter = new TagNameFilter("input");
		attributeFilter = new HasAttributeFilter("class", "netPrice");
		nodeList = this.rootNodeList.extractAllNodesThatMatch(new AndFilter(tagFilter, attributeFilter), true);
		if(nodeList.size() > 0){
			Tag t = (Tag) nodeList.elementAt(0);
			discountPrice = t.getAttribute("value");
		}
		
			
		if(isAvailable(html) == false){
			try {
				PreparedStatement pstmt = null;
				String sql = "insert into shopping(product_id, retailer, brand, product_name, isAvailable, start_date, original_price, discounted_price) values (?, ?, ?, ?, ?, ?, ?, ?)";
				pstmt = sqlConnection.prepareStatement(sql);
				pstmt.setString(1, xmlData.getId());
				pstmt.setString(2, xmlData.getRetailer());
				pstmt.setString(3, xmlData.getBrand());
				pstmt.setString(4, productName);
				pstmt.setString(5, "false");
				pstmt.setString(6, startDate);
				pstmt.setString(7, price);
				pstmt.setString(8, discountPrice);
				pstmt.executeUpdate();
				
				if(pstmt != null)
					pstmt.close();
			} catch (SQLException e1) {
				log.error(e1.toString());
				e1.printStackTrace();
			} 
			
			return;
		}
		
		if((nodeList = isVariation()) == null){
			parsing(html, null);
			return;
		}
		
		tagFilter = new TagNameFilter("img");
		nodeList = nodeList.extractAllNodesThatMatch(tagFilter, true);
		
		for(int i = 0; i < nodeList.size(); i++){
			Tag t = (Tag) nodeList.elementAt(i).getFirstChild();
			String u = t.getAttribute("src");
			String color = t.getAttribute("title");
			System.out.println(u);
			String h = getHtml(u);
			parsing(h, color);
		}
		
		
	}
	
	
	public void addHttpParams(String key, String value){
		httpParams.put(key, value);
	}

	@Override
	public String getHtml(String hostUrl) {
		HttpClient client = new DefaultHttpClient();
		HttpContext context = new BasicHttpContext();
		CookieStore cookie = new BasicCookieStore();
		HttpGet httpGet = new HttpGet(hostUrl);
		HttpParams params = new BasicHttpParams();
		context.setAttribute(ClientContext.COOKIE_STORE, cookie);
		
		//Http Get Param Setting
		if(!hostUrl.endsWith("?") && httpParams.size() > 0)
			hostUrl += "?";
		Iterator<String> it = httpParams.keySet().iterator();
		
		while(it.hasNext())
		{
			String key = it.next();
			String value = httpParams.get(key);
			hostUrl += key + "=" + value;
		}
		
		client.getParams().setParameter("http.socket.timeout", 5000);
		
		HttpResponse response = null;
	
		int count = 10;

		while(count > 0){
			count--;
			try{
				response = client.execute(httpGet, context);
				break;
			} catch (SocketTimeoutException e) {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					log.error(e1.toString());
					e1.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
		
		InputStream is = null;
		try {
			is = response.getEntity().getContent();
		} catch (IllegalStateException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		byte[] byteArray = new byte[1024];
		ByteArrayOutputStream outputStream;
		outputStream = new ByteArrayOutputStream();

		count = REPEAT_COUNT;

		while (count-- > 0) {
			try {
				int length = 0;
				while ((length = is.read(byteArray, 0, byteArray.length)) > 0) {
					outputStream.write(byteArray, 0, length);
					outputStream.flush();
				}
				break;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (count <= 0) {
			log.error("fail to read html");
			return null;
		}
		
		String str = outputStream.toString();		
	
		int start = str.indexOf("window.location.replace('");
		start += "window.location.replace('".length();
		int end = str.indexOf("'", start);
		String url = (String)str.subSequence(start, end);
		
		url = url.replace("\\", "");
		
		httpGet = new HttpGet(url);
		
		while(count > 0){
			count--;
			try{
				response = client.execute(httpGet, context);
				break;
			} catch (SocketTimeoutException e) {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					log.error(e1.toString());
					e1.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
		
		try {
			is = response.getEntity().getContent();
		} catch (IllegalStateException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
				
		
		outputStream = new ByteArrayOutputStream();

		count = REPEAT_COUNT;

		while (count-- > 0) {
			try {
				int length = 0;
				while ((length = is.read(byteArray, 0, byteArray.length)) > 0) {
					outputStream.write(byteArray, 0, length);
					outputStream.flush();
				}
				break;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (count <= 0) {
			log.error("fail to read html");
			return null;
		}

		return outputStream.toString();
	}

}
