package crawler.shopping.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;

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


public class Lacoste implements ParserImpl {

	private Logger log = Logger.getLogger(this.getClass());
	private NodeList rootNodeList;
	
	private final int REPEAT_COUNT = 5;
	
	public Lacoste() {
		
	}
	
	@Override
	public String getProductName(String html){
		NodeList nodeList = this.rootNodeList;
		NodeFilter spanFilter = new TagNameFilter("meta");
		HasAttributeFilter attributeFilter = new HasAttributeFilter("name");
		attributeFilter.setAttributeValue("Keywords");
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
		
		xmlData.setId("189995743");
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
		
		int start = html.indexOf("var detailData = ") + "var detailData = ".length();
		int end = html.indexOf("};", start) + 1;
		
		String jsonString = html.substring(start, end);

		if(!jsonString.startsWith("["))
			jsonString = "[" + jsonString;
		if(!jsonString.endsWith("]"))
			jsonString += "]";
		jsonString = "{html = " + jsonString + "}";
		JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
		
		JSONArray result =  (JSONArray) json.get("html");
		
		JSONObject obj = result.getJSONObject(0);
		obj = obj.getJSONObject("colorData");
		
		//WAYNE BLUE
		
		Iterator<String> it = obj.keys();
		while(it.hasNext()){
			String color = it.next();
			
			JSONObject o = obj.getJSONObject(color);
			JSONArray a = o.getJSONArray("sizes");
			String price = o.getString("priceLabel");
			
			for(int i = 0; i < a.size(); i++){
				String size = (String) a.get(i);
				
			}
		}
		
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
