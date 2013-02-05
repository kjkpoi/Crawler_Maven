package crawler.shopping.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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

import sun.util.locale.StringTokenIterator;
import crawler.shopping.data.XmlData;
import crawler.shopping.db.MySqlConnector;


public class BergdorfGoodman implements ParserImpl {

	private Connection sqlConnection;
	private Logger log = Logger.getLogger(this.getClass());
	private NodeList rootNodeList;
	private String originalPrice, discountPrice;
	
	private final int REPEAT_COUNT = 5;
	
	public BergdorfGoodman() {
		
	}
	
	@Override
	public String getProductName(String html){
		return null;
	}
	
	private void getPrice(){
		NodeList nodeList = this.rootNodeList;
		NodeFilter spanFilter = new TagNameFilter("span");
		HasAttributeFilter attributeFilter = new HasAttributeFilter("class", "price");
		nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, attributeFilter), true);
		if(nodeList.size() > 0){
			originalPrice = discountPrice = nodeList.elementAt(0).getFirstChild().getText(); 
		} else {
			nodeList = this.rootNodeList;
			spanFilter = new TagNameFilter("div");
			attributeFilter = new HasAttributeFilter("class", "price pos2");
			nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, attributeFilter), true);
			if(nodeList.size() > 0)
				originalPrice = nodeList.elementAt(0).getFirstChild().getText();
				
			nodeList = this.rootNodeList;
			spanFilter = new TagNameFilter("div");
			attributeFilter = new HasAttributeFilter("class", "price pos1");
			nodeList = nodeList.extractAllNodesThatMatch(new AndFilter(spanFilter, attributeFilter), true);
			if(nodeList.size() > 0)
				discountPrice = nodeList.elementAt(0).getFirstChild().getText();
		}
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
	
	public String getProductId(){
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
		
		xmlData.setId("397128790");
		
		String html = getHtml("http://www.shopstyle.com/action/apiVisitRetailer?pid=sugar&id=" + xmlData.getId());
		Parser parser = Parser.createParser(html, "UTF-8");
		try {
			rootNodeList = parser.parse(null);
		} catch (ParserException e) {
			log.error("Parsing Exception");
			e.printStackTrace();
		}
		log.error(html);
		getPrice();
		
		String productId = getProductId();
		
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
		
		int start = 0, end = 0;
		
		String sql = "insert into shopping(product_id, retailer, brand, product_name, original_price, discounted_price, size, color, quantity, isAvailable, start_date) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		try {
			pstmt = sqlConnection.prepareStatement(sql);
		} catch (SQLException e1) {
			log.error(e1.toString());
			e1.printStackTrace();
		}
		
		while(true)
		{
			start = html.indexOf("new product(", start);
			if(start == -1)
				break;
			start += "new product(".length();
			end = html.indexOf(");", start);
			String t = html.substring(start, end);
			boolean flag = false;
			String str = "";
			t = t.replace("\'", "");
			for(int i = 0 ; i < t.length(); i++){
				if(t.charAt(i) == '\'')
					flag = flag ^ true;
				
				if(flag == true && t.charAt(i) == ',')
					continue;
				str += t.charAt(i);
			}
			
			start = end;
			StringTokenIterator st = new StringTokenIterator(str, ",");
			String size = null;
			String pId = null;
			pId = st.next();
			pId = pId.replace("'", "");
			if(!pId.contentEquals(productId))
				continue;
			for(int i = 0; i < 2; i++)
				size = st.next();
			String color = st.next();
			String productName = st.next();
			String available = null;
			for(int i = 0; i < 3; i++)
				available = st.next();
			
			color = color.replace("'", "");
			productName = productName.replace("'", "");
			size = size.replace("'", "");
			available = available.replace("'", "");
			if(available.length() > 0)
				available = "false";
			else 
				available = "true";
			
			try {
				pstmt.setString(1, xmlData.getId());
				pstmt.setString(2, xmlData.getRetailer());
				pstmt.setString(3, xmlData.getBrand());
				pstmt.setString(4, productName);
				
				pstmt.setString(5, originalPrice);
				pstmt.setString(6, discountPrice);
				pstmt.setString(7, size);
				pstmt.setString(8, color);
				pstmt.setString(9, "0");
				pstmt.setString(10, available);
				pstmt.setString(11, startDate);
				pstmt.executeUpdate();
				
			} catch (SQLException e) {
				log.error(e.toString());
				e.printStackTrace();
			}
			
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
