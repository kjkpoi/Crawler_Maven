package crawler.shopping.parser;

import org.htmlparser.util.NodeList;

public abstract class HtmlParser {
	
	protected NodeList rootNodeList;
	
	abstract public void run();
	abstract protected String getHtml(String hotUrl);
	abstract protected String getProductName();
	
}
