package crawler.shopping.parser;

import crawler.shopping.data.XmlData;

public interface ParserImpl {
	public void run(XmlData xmlData, String startDate);
	public String getHtml(String hotUrl);
	public String getProductName(String html);
}
