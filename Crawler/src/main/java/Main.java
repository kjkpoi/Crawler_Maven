

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

public class Main {
	
	public static void main(String[] args) {
		HttpClient client = new DefaultHttpClient();
		HttpContext context = new BasicHttpContext();
		CookieStore cookie = new BasicCookieStore();
		HttpGet get = new HttpGet("http://us.topshop.com/webapp/wcs/stores/servlet/ProductDisplay?beginIndex=1&viewAllFlag=&catalogId=33060&storeId=13052&productId=6719578&langId=-1&sort_field=Relevance&categoryId=208634&parent_categoryId=208580&pageSize=20");
		HttpResponse response = null;
		try {
			response = client.execute(get);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		InputStream is = null;
		try {
			is = response.getEntity().getContent();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] b = new byte[10024];
		String str = new String();
		
		try {
			while(is.read(b, 0, 10024) != -1)
			{
				str += new String(b);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(str);
	}

}
