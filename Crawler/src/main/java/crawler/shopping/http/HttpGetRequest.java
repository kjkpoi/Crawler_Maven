package crawler.shopping.http;

//import java.io.IOException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

public class HttpGetRequest {
	
	private Logger log = Logger.getLogger(this.getClass());
	
	private Map<String, String> httpParams;
	
	
	public HttpGetRequest() {
		httpParams = new HashMap<String, String>();
	}
	
	public void addHttpParams(String key, String value){
		httpParams.put(key, value);
	}
	
	public String getHttpRequest(String hostUrl){


		/*HttpMethod method = new GetMethod(hostUrl);
		HttpClient client = new HttpClient();
		String tempHtml = null;
	
		log.info("start execute");
		method.setFollowRedirects(true);
		client.getParams().setParameter("http.socket.timeout", 5000);
		
		int count = 0;
		int status = 0;
					
		log.info("Try to get Http Response");
		while(count++ <= 10){
			try{
				status = client.executeMethod(method);
				break;
			} catch (Exception e) {
				log.error(hostUrl + ", " + e.toString() + ", Retry!");
			}
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				log.error(e1.toString());
			}
			log.info("Count: " + count);
		}
		
		if(count > 10){
			log.error("Fail to get Http Response");
		}
		log.info("Success to get Http Response");
		
		count = 0;
		log.info("Try to get Html");
		while(count++ <= 10){
			try {
				tempHtml = method.getResponseBodyAsString();
				if(tempHtml != null)
					break;

			} catch (IOException e1) {
				e1.printStackTrace();
			}
			log.error("Retry: " + count);
			try{
				Thread.sleep(5000);
			} catch (Exception e) {
				log.error(e.toString());
			}
			log.info("Count: " + count);
		}
		
		if(count > 10){
			log.error("Fail to get Html");
		}
		log.info("Success to get Html");
			
	
		return tempHtml;
		*/
		HttpClient client = new DefaultHttpClient();
		HttpContext context = new BasicHttpContext();
		CookieStore cookie = new BasicCookieStore();
		HttpGet httpGet = new HttpGet(hostUrl);
		HttpParams params = new BasicHttpParams();
		
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
		
		RedirectStrategy redirect = new DefaultRedirectStrategy();
		try {
			System.out.println(redirect.isRedirected(httpGet, response, context));
		} catch (ProtocolException e2) {
			e2.printStackTrace();
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
		try {
			while(is.read(b, 0, 1024) != -1)
			{
				str += new String(b);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return str;
	}
}
