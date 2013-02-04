package crawler.shopping.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
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


public class UrbanOutfitters implements ParserImpl {

	private Logger log = Logger.getLogger(this.getClass());
	private NodeList rootNodeList;
	
	private final int REPEAT_COUNT = 5;
	
	public UrbanOutfitters() {
		
	}
	
	@Override
	public String getProductName(String html){
		NodeList nodeList = this.rootNodeList;
		NodeFilter spanFilter = new TagNameFilter("h2");
		HasAttributeFilter attributeFilter = new HasAttributeFilter("id", "prodTitle");
		nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, attributeFilter), true);
		if(nodeList.size() <= 0)
			return null;
		return nodeList.elementAt(0).getFirstChild().getText();
	}
	
	public boolean isAvailable(String html){
		
		if(html.contains("This product is currently unavailable") == true){
			return false;
		}
		return true;
	}
	
	@Override
	public void run(XmlData xmlData, String startDate) {

		MySqlConnector mysqlConnector = new MySqlConnector();
		Connection sqlConnection = mysqlConnector.getConnection();
		PreparedStatement pstmt = null;
	//	xmlData.setId("213194751");
		String html = getHtml("http://www.shopstyle.com/action/apiVisitRetailer?pid=sugar&id=" + xmlData.getId());
		Parser parser = Parser.createParser(html, "UTF-8");
		try {
			rootNodeList = parser.parse(null);
		} catch (ParserException e) {
			log.error("Parsing Exception");
			e.printStackTrace();
		}
		
		String productName = getProductName(html);
		
		
		//get color
		TagNameFilter spanFilter = new TagNameFilter("span");
		HasAttributeFilter classFilter = new HasAttributeFilter("class", "swatches");
		NodeList nodeList = this.rootNodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, classFilter), true);
		spanFilter = new TagNameFilter("img");
		nodeList = nodeList.extractAllNodesThatMatch(spanFilter, true);
		Map<String, String> colorMap = new HashMap<>();
		for(int i = 0; i < nodeList.size(); i++){
			Tag tag = (Tag) nodeList.elementAt(i);
			String id = tag.getAttribute("id");
			String color = tag.getAttribute("title");
			colorMap.put(id, color);
		}
		
		//get price
		spanFilter = new TagNameFilter("span");
		classFilter = new HasAttributeFilter("itemprop", "price");
		nodeList = this.rootNodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, classFilter), true);
		String price = null;
		if(nodeList.size() >= 1)
			price = nodeList.elementAt(0).getFirstChild().getText();

		
		spanFilter = new TagNameFilter("div");
		classFilter = new HasAttributeFilter("class", "size");
		nodeList = this.rootNodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, classFilter), true);
		spanFilter = new TagNameFilter("span");
		nodeList = nodeList.extractAllNodesThatMatch(spanFilter, true);
		

		String sql = "insert into shopping(product_id, retailer, brand, product_name, original_price, discounted_price, size, color, quantity,isAvailable, start_date) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		spanFilter = new TagNameFilter("a");
		
		for(int i = 0; i < nodeList.size(); i++){
			Tag tt = (Tag) nodeList.elementAt(i);
			String id = tt.getAttribute("id");
			NodeList l = nodeList.elementAt(i).getChildren().extractAllNodesThatMatch(spanFilter, true);
			for(int j = 0; j < l.size(); j++){
				Tag t = (Tag) l.elementAt(j);
				String available = t.getAttribute("data-msg");
				if(available.contentEquals("Sold Out"))
					available = "false";
				else 
					available = "true";
				String size = l.elementAt(j).getFirstChild().getText();
				String color = colorMap.get(id);
				
				try {
					pstmt = sqlConnection.prepareStatement(sql);
					pstmt.setString(1, xmlData.getId());
					pstmt.setString(2, xmlData.getRetailer());
					pstmt.setString(3, xmlData.getBrand());
					pstmt.setString(4, productName);
					
					pstmt.setString(5, price);
					pstmt.setString(6, price);
					pstmt.setString(7, size);
					pstmt.setString(8, color);
					pstmt.setString(9, "0");
					pstmt.setString(10, available);
					pstmt.setString(11, startDate);
					pstmt.executeUpdate();
					
				} catch (SQLException e) {
					log.error(e.toString());
					e.printStackTrace();
				} finally{
					if(pstmt != null){
						try {
							pstmt.close();
						} catch (SQLException e) {
							log.error(e.toString());
							e.printStackTrace();
						}
					}
				}

				
			}
		}
		
		
		/*
		
		if(isAvailable(html) == false){
			String sql = "insert into shopping(product_id, retailer, brand, product_name ,isAvailable, start_date) values (?, ?, ?, ?, ?, ?)";
			try {
				pstmt = sqlConnection.prepareStatement(sql);
				pstmt.setString(1, xmlData.getId());
				pstmt.setString(2, xmlData.getRetailer());
				pstmt.setString(3, xmlData.getBrand());
				pstmt.setString(4, productName);
				
				pstmt.setString(5, "false");
				pstmt.setString(6, startDate);
			} catch (SQLException e) {
				log.error(e.toString());
				e.printStackTrace();
			}
			
			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			return ;
		}
		
		
				
		
		int start = html.indexOf("MACYS.pdp.upcmap[") + "MACYS.pdp.upcmap[".length();
		start= html.indexOf("[{", start);
		int end = html.indexOf("}];", start) + 2;
		
		String jsonString = html.substring(start, end);

		if(!jsonString.startsWith("["))
			jsonString = "[" + jsonString;
		if(!jsonString.endsWith("]"))
			jsonString += "]";
		jsonString = "{html = " + jsonString + "}";
		JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
		
		JSONArray result =  (JSONArray) json.get("html");
		
		String sql = "insert into shopping(product_id, retailer, brand, product_name, original_price, discounted_price, size, color, quantity,isAvailable, start_date, etc) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		String price = null;
		String discountPrice = null;
		*/
		/*NodeList nodeList = this.rootNodeList;
		NodeFilter spanFilter = new TagNameFilter("div");
		HasAttributeFilter attributeFilter = new HasAttributeFilter("class", "standardProdPricingGroup");
		nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, attributeFilter), true);
		spanFilter = new TagNameFilter("span");
		nodeList = nodeList.extractAllNodesThatMatch(spanFilter, true);
		if(nodeList.size() >= 1)
			price = nodeList.elementAt(0).getFirstChild().getText();
		
		attributeFilter = new HasAttributeFilter("class", "priceSale");
		nodeList = nodeList.extractAllNodesThatMatch(attributeFilter, true);
		if(nodeList.size() >= 1)	
			discountPrice = nodeList.elementAt(0).getFirstChild().getText();
		
		
		
		
		for(int i = 0; i < result.size(); i++){
			JSONObject obj = result.getJSONObject(i);
			String color = obj.getString("color");
			String size = obj.getString("size");
			String isAvailable = obj.getString("isAvailable");
			String etc =  obj.getString("availabilityMsg");
			
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
				pstmt.setString(9, "0");
				pstmt.setString(10, isAvailable);
				pstmt.setString(11, startDate);
				pstmt.setString(12, etc);
				pstmt.executeUpdate();
			} catch (SQLException e) {
				log.error(e.toString());
				e.printStackTrace();
			} 
		}
		
		try {
			pstmt.close();
		} catch (SQLException e) {
			log.error(e.toString());
			e.printStackTrace();
		}
		*/
	}
	

	@Override
	public String getHtml(String hostUrl) {
		HttpClient client = new DefaultHttpClient();
		HttpContext context = new BasicHttpContext();
		CookieStore cookie = new BasicCookieStore();
		HttpGet httpGet = new HttpGet(hostUrl);
		HttpParams params = new BasicHttpParams();
		context.setAttribute(ClientContext.COOKIE_STORE, cookie);
		
		client.getParams().setParameter("http.socket.timeout", 5000);
		client.getParams().setBooleanParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
		
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
