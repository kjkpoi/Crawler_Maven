package crawler.shopping.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
import org.htmlparser.Tag;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.HasParentFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.MetaTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import crawler.shopping.db.MySqlConnector;


public class SixPm implements ParserImpl {

	private Logger log = Logger.getLogger(this.getClass());
	private Map<String, String> httpParams;
	private NodeList rootNodeList;
	
	private final int REPEAT_COUNT = 5;
	
	public SixPm() {
		httpParams = new HashMap<String, String>();
		
	}
	
	JSONArray getJsonArray(String html, String name) {
		int start = html.indexOf(name + " = ") + name.length() + " = ".length();
		int end = html.indexOf(";", start);
		String jsonString = html.substring(start, end);
		System.out.println(jsonString);
		if(!jsonString.startsWith("["))
			jsonString = "[" + jsonString;
		if(!jsonString.endsWith("]"))
			jsonString += "]";
		jsonString = "{html = " + jsonString + "}";
		JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
		
		JSONArray result =  (JSONArray) json.get("html");
		
		return result;
	}
	
	private String getProductName(String html){
		NodeList nodeList = this.rootNodeList;
		NodeFilter spanFilter = new TagNameFilter("meta");
		HasAttributeFilter attributeFilter = new HasAttributeFilter("property");
		attributeFilter.setAttributeValue("og:title");
		nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, attributeFilter), true);
		MetaTag meta = (MetaTag) nodeList.elementAt(0);
		return meta.getAttribute("content");
	}
	
	@Override
	public void run() {
		
		MySqlConnector mysqlConnector = new MySqlConnector();
		Connection sqlConnection = mysqlConnector.getConnection();
		PreparedStatement pstmt = null;
		
		String html = getHtml("http://www.shopstyle.com/action/apiVisitRetailer?pid=sugar&id=350012307");
		log.info(html);
		
		Parser parser = Parser.createParser(html, "UTF-8");
		try {
			rootNodeList = parser.parse(null);
		} catch (ParserException e) {
			log.error("Parsing Exception");
			e.printStackTrace();
		}
		
		String retailer = "6pm.com";
		String brand = "Dissel";
		String productName = getProductName(html);
				
		JSONArray stock = getJsonArray(html, "stockJSON");
		JSONObject color = getJsonArray(html, "colorNames").getJSONObject(0);
		JSONObject size = getJsonArray(html, "valueIdToNameJSON").getJSONObject(0);
		JSONObject price = getJsonArray(html, "colorPrices").getJSONObject(0);
		
		Iterator<String> colorIt = color.keys();
		Iterator<String> sizeIt = size.keys();
		Set<String> stockSet = new HashSet<>();
		
		String sql = "insert into shopping(retailer, brand, product_name, original_price, discounted_price, size, color, isAvailable) values (?, ?, ?, ?, ?, ?, ?, ?)";
		try {
			pstmt = sqlConnection.prepareStatement(sql);
		} catch (SQLException e) {
			log.error(e.toString());
			e.printStackTrace();
		}
		
		for(int i = 0; i < stock.size(); i++){
			JSONObject stockObj = stock.getJSONObject(i);
			String colorKey = stockObj.getString("color");
			String sizeKey = stockObj.getString("d13");
			stockSet.add(colorKey + sizeKey);
			//db insert
			try {
				pstmt.setString(1, retailer);
				pstmt.setString(2, brand);
				pstmt.setString(3, productName);
				
				//price
				JSONObject priceObj = price.getJSONObject(stockObj.getString("color"));
				
				pstmt.setString(4, priceObj.getString("wasInt"));
				pstmt.setString(5, priceObj.getString("nowInt"));
				pstmt.setString(6, size.getString(sizeKey));
				pstmt.setString(7, color.getString(colorKey));
				pstmt.setInt(8, 1);
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
		
		
		while(colorIt.hasNext()){
			String colorkey = colorIt.next();
			String colorValue = color.getString(colorkey);
			
			sizeIt = size.keys();
			while(sizeIt.hasNext()){
				String sizeKey = sizeIt.next();
				JSONObject sizeObj = size.getJSONObject(sizeKey);
				String sizeValue = sizeObj.getString("value");
				if(!stockSet.contains(colorkey + sizeKey)){
					// db insert
				}
			}
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
		
	/*	byte[] b = new byte[1024];
		String str = new String();
		
		try {
			while(is.read(b, 0, 1024) != -1)
			{
				str += new String(b);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}*/
		
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
