package crawler.shopping.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.StringTokenizer;

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


public class Footcandy implements ParserImpl {

	private Logger log = Logger.getLogger(this.getClass());
	private NodeList rootNodeList;
	
	private final int REPEAT_COUNT = 5;
	
	int originalPrice, discountPrice;
	
	public Footcandy() {
		
	}
	
	private int getNextValue(String result, String html, String code, int start){
		String str = code + " = '";
		int s = html.indexOf(str, start) + str.length();
		int end = html.indexOf("';", s);
		result = html.substring(s, end);
		return end + 1;
	}
	
	private int getNextArrayValue(String result, String html, String code, int start){
		String str = code + "('";
		int s = html.indexOf(str, start) + str.length();
		int end = html.indexOf("'", s);
		result = html.substring(s, end);
		return end + 1;
	}
	
	private void getPrice(String html){
		
	}
	
	
	@Override
	public String getProductName(String html){
		NodeList nodeList = this.rootNodeList;
		NodeFilter spanFilter = new TagNameFilter("td");
		HasAttributeFilter attributeFilter = new HasAttributeFilter("class", "value istartext");
		nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, attributeFilter), true);
		if(nodeList.size() < 1)
			return null;
		return nodeList.elementAt(0).getFirstChild().getText().trim();
	}
	
	public boolean isAvailable(String html){
		if(html.contains("The item you have requested is no longer available.") == true){
			return false;
		}
		return true;
	}
	
	@Override
	public void run(XmlData xmlData, String startDate) {
		
		MySqlConnector mysqlConnector = new MySqlConnector();
		Connection sqlConnection = mysqlConnector.getConnection();
		PreparedStatement pstmt = null;
		
		xmlData.setId("138740780");
		String html = getHtml("http://www.shopstyle.com/action/apiVisitRetailer?pid=sugar&id=" + xmlData.getId());
		Parser parser = Parser.createParser(html, "UTF-8");
		try {
			rootNodeList = parser.parse(null);
		} catch (ParserException e) {
			log.error("Parsing Exception");
			e.printStackTrace();
		}
		
		log.info(html);
		
		String productName = getProductName(html);
		
		String str = "var colors = new Array(";
		int start = html.indexOf(str) + str.length();
		int end = html.indexOf(");", start);
		str = html.substring(start, end);
		str = str.replace("'", "");
		StringTokenizer codeToken = new StringTokenizer(str, ",");
		
		/*while(codeToken.hasMoreTokens()){
			String color = codeToken.nextToken();
			String code = codeToken.nextToken();
			String originalPrice = getOriginalPrice(html, code);
			String discountPrice = getDiscountPrice(html, code);
			ArrayList<String> size = getSizeList(html, code);
			if(size.size() == 0){
				String sql = "insert into shopping(product_id, retailer, brand, product_name, original_price, discounted_price, color, quantity,isAvailable, start_date) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
				try {
					pstmt = sqlConnection.prepareStatement(sql);
					pstmt.setString(1, xmlData.getId());
					pstmt.setString(2, xmlData.getRetailer());
					pstmt.setString(3, xmlData.getBrand());
					pstmt.setString(4, productName);
					pstmt.setString(5, originalPrice);
					pstmt.setString(6, discountPrice);
					pstmt.setString(7, color);
					pstmt.setString(8, "0");
					pstmt.setString(9, "false");
					pstmt.setString(10, startDate);
					pstmt.executeUpdate();
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
				continue;
			}
			
			String sql = "insert into shopping(product_id, retailer, brand, product_name, original_price, discounted_price, size, color, quantity, isAvailable, start_date) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			
			for(int i = 0; i < size.size(); i++){
				try {
					pstmt = sqlConnection.prepareStatement(sql);
					pstmt.setString(1, xmlData.getId());
					pstmt.setString(2, xmlData.getRetailer());
					pstmt.setString(3, xmlData.getBrand());
					pstmt.setString(4, productName);
					
					pstmt.setString(5, originalPrice);
					pstmt.setString(6, discountPrice);
					pstmt.setString(7, size.get(i));
					pstmt.setString(8, color);
					pstmt.setString(9, "0");
					pstmt.setString(10, "true");
					pstmt.setString(11, startDate);
					pstmt.executeUpdate();
					
				} catch (SQLException e) {
					log.error(e.toString());
					e.printStackTrace();
				}
			}
			
			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		}
		
		*/
	}
	
	@Override
	public String getHtml(String hostUrl) {
		HttpClient client = new DefaultHttpClient();
		HttpContext context = new BasicHttpContext();
		CookieStore cookie = new BasicCookieStore();
		HttpGet httpGet = new HttpGet(hostUrl);
		context.setAttribute(ClientContext.COOKIE_STORE, cookie);
		
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
