package crawler.shopping.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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


public class SixPm implements ParserImpl {

	private Logger log = Logger.getLogger(this.getClass());
	private Map<String, String> httpParams;
	
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
	
	@Override
	public void run() {
		String html = getHtml("http://www.shopstyle.com/action/apiVisitRetailer?pid=sugar&id=350012307");
		log.info(html);
		
		JSONArray stock = getJsonArray(html, "stockJSON");
		JSONObject color = getJsonArray(html, "colorNames").getJSONObject(0);
		JSONObject size = getJsonArray(html, "valueIdToNameJSON").getJSONObject(0);
		JSONObject price = getJsonArray(html, "colorPrices").getJSONObject(0);
		
		System.out.println(color.size());
		Iterator<String> colorIt = color.keys();
		
		while(colorIt.hasNext()){
			String colorkey = colorIt.next();
			String colorValue = color.getString(colorkey);
			System.out.println(colorkey + " " + colorValue);
			
			Iterator<String> sizeIt = size.keys();
			while(sizeIt.hasNext()){
				String sizeKey = sizeIt.next();
				JSONObject sizeValue = size.getJSONObject(sizeKey);
				System.out.println(sizeKey + " " + sizeValue);
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
		
		byte[] b = new byte[1024];
		String str = new String();
		
		try {
			while(is.read(b, 0, 1024) != -1)
			{
				str += new String(b);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	
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
				
		str = "";
		byte[] byteArray = new byte[1024];
		ByteArrayOutputStream outputStream;
		outputStream = new ByteArrayOutputStream();
		
		try {
			while ((count = is.read(byteArray, 0, byteArray.length)) > 0) {	
				outputStream.write(byteArray, 0, count);
				outputStream.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return outputStream.toString();
	}



}
