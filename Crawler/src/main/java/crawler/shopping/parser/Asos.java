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
import org.htmlparser.Tag;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.HasParentFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.MetaTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import crawler.shopping.data.XmlData;
import crawler.shopping.db.MySqlConnector;


public class Asos implements ParserImpl {

	private Logger log = Logger.getLogger(this.getClass());
	private Map<String, String> httpParams;
	private NodeList rootNodeList;
	
	private final int REPEAT_COUNT = 5;
	
	public Asos() {
		httpParams = new HashMap<String, String>();
	}
	
	
	@Override
	public String getProductName(String html){
		NodeList nodeList = this.rootNodeList;
		NodeFilter spanFilter = new TagNameFilter("meta");
		HasAttributeFilter attributeFilter = new HasAttributeFilter("name");
		attributeFilter.setAttributeValue("og:title");
		nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, attributeFilter), true);
		MetaTag meta = (MetaTag) nodeList.elementAt(0);
		if(meta == null)
			return null;
		return meta.getAttribute("content");
	}
	
	public boolean isAvailable(String html){
		
		if(html.contains("Whoops! We couldn't find the pages you were looking for.") == true || html.contains("WE COULDN'T FIND THE PAGE YOU WERE LOOKING FOR")){
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
		
		String productName = getProductName(html);
		
		int start = 0, index, end;
		String var = "arrSzeCol_ctl00_ContentMainPage_ctlSeparateProduct[";
		String sql = "insert into shopping(product_id, retailer, brand, product_name, original_price, discounted_price, size, color, quantity, isAvailable, start_date) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		while(true){
			index = html.indexOf(var, start);
			if(index == -1)
				break;
			start = html.indexOf("(", index + var.length());
			start++;
			end = html.indexOf(");", start);
			String value = html.substring(start, end);
			System.out.println(value);
			int s = 0;
			int i = 0;
			int e = 0;
			
			boolean available;
			String size, color, discountPrice, price;
			e = value.indexOf(",", s);
			s = e + 1;
			e = value.indexOf(",", s);
			size = value.substring(s, e);
			size = size.replaceAll("\"", "");
			
			s = e + 1;
			e = value.indexOf(",", s);
			color = value.substring(s, e);
			color = color.replaceAll("\"", "");
			
			s = e + 1;
			e = value.indexOf(",", s);
			if(value.substring(s, e).contains("True"))
				available = true;
			else
				available = false;
			
			s = e + 1;
			e = value.indexOf(",", s);
			s = e + 1;
			e = value.indexOf(",", s);
			discountPrice = value.substring(s, e);
			discountPrice = discountPrice.replaceAll("\"", "");
			
			s = e + 1;
			e = value.indexOf(",", s);
			price = value.substring(s, e);
			price = price.replaceAll("\"", "");
			
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
				if(available)
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
			pstmt.close();
		} catch (SQLException e) {
			log.error(e.toString());
			e.printStackTrace();
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
