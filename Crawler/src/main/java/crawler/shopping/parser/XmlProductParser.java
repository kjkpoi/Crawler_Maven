package crawler.shopping.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import crawler.shopping.data.XmlData;

public class XmlProductParser {

	private final int REPEAT_COUNT = 5;

	private Logger log = Logger.getLogger(this.getClass());

	public ArrayList<XmlData> getProduceCodeList(int retailer, int start,
			int length) {

		HttpClient client = new DefaultHttpClient();
		HttpContext context = new BasicHttpContext();
		CookieStore cookie = new BasicCookieStore();
		String hostUrl = "http://lashop.co.kr/xml.php?retailer="
				+ retailer + "&page=" + start + "&limit=" + length;
		HttpGet httpGet = new HttpGet(hostUrl);
		context.setAttribute(ClientContext.COOKIE_STORE, cookie);


		client.getParams().setParameter("http.socket.timeout", 5000);

		HttpResponse response = null;

		int count = 10;

		while (count > 0) {
			count--;
			try {
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
				int len = 0;
				while ((len = is.read(byteArray, 0, byteArray.length)) > 0) {
					outputStream.write(byteArray, 0, len);
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
		
		try {
			outputStream.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		client.getConnectionManager().shutdown();
		httpGet.reset();
		
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		InputStream in = null;

		try {
			in = new ByteArrayInputStream(str.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			log.error(e1.toString());
			e1.printStackTrace();
		}
		
		ArrayList<XmlData> result = new ArrayList<>();
		
		try {
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(in, null);
			NodeList list = doc.getFirstChild().getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				if(list.item(i).getNodeName() == "#text")
					continue;
				
				Element element = (Element) list.item(i);
				
				XmlData data = new XmlData();
				data.setBrand(element.getElementsByTagName("brand").item(0).getTextContent());
				data.setRetailer(element.getElementsByTagName("retailer").item(0).getTextContent());
				data.setId(element.getElementsByTagName("id").item(0).getTextContent());
				result.add(data);
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		

		return result;
	}

}
