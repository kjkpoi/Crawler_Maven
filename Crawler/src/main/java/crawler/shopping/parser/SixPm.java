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
		if(!jsonString.startsWith("["))
			jsonString = "[" + jsonString;
		if(!jsonString.endsWith("]"))
			jsonString += "]";
		jsonString = "{html = " + jsonString + "}";
		JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
		
		JSONArray result =  (JSONArray) json.get("html");
		
		return result;
	}
	
	@Override
	public String getProductName(String html){
		NodeList nodeList = this.rootNodeList;
		NodeFilter spanFilter = new TagNameFilter("meta");
		HasAttributeFilter attributeFilter = new HasAttributeFilter("property");
		attributeFilter.setAttributeValue("og:title");
		nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, attributeFilter), true);
		MetaTag meta = (MetaTag) nodeList.elementAt(0);
		return meta.getAttribute("content");
	}
	
	public boolean isAvailable(String html){
		if(html.contains("We are not able to find the page you requested...") == true){
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
		
		if(isAvailable(html) == false || getProductName(html) == null){
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
				
		JSONArray stock = getJsonArray(html, "stockJSON");
		JSONObject color = getJsonArray(html, "colorNames").getJSONObject(0);
		JSONObject size = getJsonArray(html, "valueIdToNameJSON").getJSONObject(0);
		
		JSONObject price = getJsonArray(html, "colorPrices").getJSONObject(0);
		JSONObject dimension = getJsonArray(html, "dimensionIdToNameJson").getJSONObject(0);
		String sizeCode = null;
		Iterator<String> dimensionIt = dimension.keys();
		
		while(dimensionIt.hasNext()){
			String dimensionKey = dimensionIt.next();
			String dimensionValue = dimension.getString(dimensionKey);
			if(dimensionValue.contentEquals("size") == true){
				sizeCode = dimensionKey;
				break;
			}
		}
		
		JSONObject sizeArray = getJsonArray(html, "dimToUnitToValJSON").getJSONObject(0);
		sizeArray = (JSONObject) sizeArray.getJSONObject(sizeCode);
		JSONArray sizeList = new JSONArray();
		if(sizeArray.size() != 0){
			Collection<JSONArray> c = sizeArray.values();
			sizeList = new ArrayList<JSONArray>(c).get(0);
		}

		Iterator<String> colorIt = color.keys();
		//Iterator<String> sizeIt = size.keys();
		Set<String> stockSet = new HashSet<>();
		
		String sql = "insert into shopping(product_id, retailer, brand, product_name, original_price, discounted_price, size, color, quantity,isAvailable, start_date) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		for(int i = 0; i < stock.size(); i++){
			JSONObject stockObj = stock.getJSONObject(i);
			String colorKey = stockObj.getString("color");
			String sizeKey = null;
			if(stockObj.containsKey(sizeCode) == true)
				sizeKey = stockObj.getString(sizeCode);
			
			if((color.containsKey(colorKey) == false) || (sizeKey != null && size.containsKey(sizeKey) == false))
				continue;
			stockSet.add(colorKey + sizeKey);
			
			//db insert
			try {
				pstmt = sqlConnection.prepareStatement(sql);
				pstmt.setString(1, xmlData.getId());
				pstmt.setString(2, xmlData.getRetailer());
				pstmt.setString(3, xmlData.getBrand());
				pstmt.setString(4, productName);
				
				//price
				JSONObject priceObj = price.getJSONObject(colorKey);
				
				pstmt.setString(5, priceObj.getString("wasInt"));
				pstmt.setString(6, priceObj.getString("nowInt"));
				if(sizeKey != null){
					JSONObject sizeObj = size.getJSONObject(sizeKey);
					pstmt.setString(7, sizeObj.getString("value"));
				} else{
					pstmt.setNull(7, java.sql.Types.VARCHAR);
				}
				pstmt.setString(8, color.getString(colorKey));
				if(stockObj.containsKey("onHand") == true)
					pstmt.setString(9, stockObj.getString("onHand"));
				else
					pstmt.setNull(9, java.sql.Types.VARCHAR);
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
			log.error(e.toString());
			e.printStackTrace();
		}
		
		
		while (colorIt.hasNext()) {
			String colorKey = colorIt.next();
			String colorValue = color.getString(colorKey);

			if (sizeList.size() == 0) {

			} else {
				for (int i = 0; i < sizeList.size(); i++) {
					String sizeKey = sizeList.getString(i);
					String sizeValue = size.getString(sizeKey);
					if (!stockSet.contains(colorKey + sizeKey)) {
						sql = "insert into shopping(product_id, retailer, brand, product_name, original_price, discounted_price, size, color, quantity,isAvailable, start_date) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
						try {
							pstmt = sqlConnection.prepareStatement(sql);
							pstmt.setString(1, xmlData.getId());
							pstmt.setString(2, xmlData.getRetailer());
							pstmt.setString(3, xmlData.getBrand());
							pstmt.setString(4, productName);

							// price
							JSONObject priceObj = price.getJSONObject(colorKey);

							pstmt.setString(5, priceObj.getString("wasInt"));
							pstmt.setString(6, priceObj.getString("nowInt"));
							if (sizeKey != null) {
								JSONObject sizeObj = size
										.getJSONObject(sizeKey);
								pstmt.setString(7, sizeObj.getString("value"));
							} else {
								pstmt.setNull(7, java.sql.Types.VARCHAR);
							}
							pstmt.setString(8, color.getString(colorKey));

							pstmt.setString(9, "0");
							pstmt.setString(10, "noStock");
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
						log.error(e.toString());
						e.printStackTrace();
					}

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
