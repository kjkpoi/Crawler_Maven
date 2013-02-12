package crawler.shopping.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

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
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.MetaTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import crawler.shopping.data.XmlData;
import crawler.shopping.db.MySqlConnector;


public class Target implements ParserImpl {

	private Logger log = Logger.getLogger(this.getClass());
	private Map<String, String> httpParams;
	private NodeList rootNodeList;
	private String originalPrice, discountPrice;
	
	private final int REPEAT_COUNT = 5;
	
	public Target() {
		httpParams = new HashMap<String, String>();
	}
	
	JSONArray getJsonArray(String jsonString) {
	
		jsonString = "{html = " + jsonString + "}";
		JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
		
		JSONArray result =  (JSONArray) json.get("html");
		
		return result;
	}
	
	private void getPrice(){
		NodeList nodeList = this.rootNodeList;
		NodeFilter spanFilter = new TagNameFilter("p");
		HasAttributeFilter attributeFilter = new HasAttributeFilter("class", "module-title salePrice");
		nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, attributeFilter), true);
		discountPrice = nodeList.elementAt(0).getFirstChild().getText();
		
		spanFilter = new TagNameFilter("li");
		attributeFilter = new HasAttributeFilter("class", "DisPrice");
		nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, attributeFilter), true);
		spanFilter = new TagNameFilter("del");
		nodeList = nodeList.extractAllNodesThatMatch(spanFilter, true);
		if(nodeList.size() > 0){
			originalPrice = nodeList.elementAt(0).getFirstChild().getText(); 
		}
		else
			originalPrice = discountPrice;
		
		if(discountPrice != null)
			discountPrice = discountPrice.trim();
		if(originalPrice != null)
			originalPrice = originalPrice.trim();
	}
	
	@Override
	public String getProductName(String html){
		NodeList nodeList = this.rootNodeList;
		NodeFilter spanFilter = new TagNameFilter("p");
		HasAttributeFilter attributeFilter = new HasAttributeFilter("class", "productName");
		nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, attributeFilter), true);
		return nodeList.elementAt(0).getFirstChild().getFirstChild().getText();
	}
	
	public boolean isAvailable(String html){
		if(html.contains("oops, let's try that again.") == true){
			return false;
		}
		return true;
	}
	
	@Override
	public void run(XmlData xmlData, String startDate) {
		
		MySqlConnector mysqlConnector = new MySqlConnector();
		Connection sqlConnection = mysqlConnector.getConnection();
		PreparedStatement pstmt = null;
		
		//xmlData.setId("263352273");
		String html = getHtml("http://www.shopstyle.com/action/apiVisitRetailer?pid=sugar&id=" + xmlData.getId());
		Parser parser = Parser.createParser(html, "UTF-8");
		try {
			rootNodeList = parser.parse(null);
		} catch (ParserException e) {
			log.error("Parsing Exception");
			e.printStackTrace();
		}
		
		log.info(html);
		
		if(isAvailable(html) == false){
			String sql = "insert into shopping(product_id, retailer, brand, isAvailable, start_date) values (?, ?, ?, ?, ?)";
			try {
				pstmt = sqlConnection.prepareStatement(sql);
				pstmt.setString(1, xmlData.getId());
				pstmt.setString(2, xmlData.getRetailer());
				pstmt.setString(3, xmlData.getBrand());
				pstmt.setString(4, "false");
				pstmt.setString(5, startDate);
				pstmt.executeUpdate();
			} catch (SQLException e) {
				log.error(e.toString());
				e.printStackTrace();
			}
			
			try {
				pstmt.close();
			} catch (SQLException e) {
				log.error(e.toString());
				e.printStackTrace();
			}

			return;
		}
		
		
		String productName = getProductName(html);
		
		NodeList nodeList = this.rootNodeList;
		NodeFilter spanFilter = new TagNameFilter("div");
		HasAttributeFilter attributeFilter = new HasAttributeFilter("id", "entitledItem");
		nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, attributeFilter), true);
		if(nodeList.size() < 1)
			return;
		String str = nodeList.elementAt(0).getText();
		if(str.startsWith("\""))
			str = str.substring(1);
		if(str.endsWith("\""))
			str = str.substring(0, str.length() - 1);
		JSONObject obj = getJsonArray(str).getJSONObject(0);
		
		
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
