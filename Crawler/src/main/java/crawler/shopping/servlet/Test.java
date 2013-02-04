package crawler.shopping.servlet;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import crawler.shopping.data.XmlData;
import crawler.shopping.http.HttpGetRequest;
import crawler.shopping.parser.Asos;
import crawler.shopping.parser.ParserImpl;
import crawler.shopping.parser.SixPm;
import crawler.shopping.parser.XmlProductParser;
import crawler.shopping.utils.ShoppingMallList;

/**
 * Servlet implementation class Test3
 */
@WebServlet(description = "Test", urlPatterns = { "/Test" })
public class Test extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final int NUMBER = 11;
	private Logger log = Logger.getLogger(this.getClass());
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Test() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#service(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ArrayList<String> mallList = ShoppingMallList.getShoppingMallList();
		
		try {
			Class c = Class.forName("crawler.shopping.parser." + mallList.get(NUMBER));
			ParserImpl parser = (ParserImpl) c.newInstance();
			
			int count = 0;
			int gap = 100;
			int start = 0;
			ArrayList<XmlData> xmlDataList = new ArrayList<>();
			XmlProductParser xmlParser = new XmlProductParser();
			
			SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat ( "yyyy.MM.dd HH:mm:ss", Locale.KOREA );
			Date currentTime = new Date();
			String mTime = mSimpleDateFormat.format ( currentTime );

			
			while(true){
				xmlDataList.clear();
				xmlDataList = xmlParser.getProduceCodeList(NUMBER, start, start + gap);
				count += xmlDataList.size();
				
				for(int i = 0 ; i < xmlDataList.size(); i++){
					parser.run(xmlDataList.get(i), mTime);
				}
			
				System.out.println(count);
				log.info(count + " DONE");
				if(xmlDataList.size() != gap){
					break;
				}
			}
			
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		/*HttpGetRequest get = new HttpGetRequest();
		PrintWriter out = response.getWriter();
		String str;
		str = get.getHttpRequest("http://www.shopstyle.com/action/apiVisitRetailer?pid=sugar&id=350012307");
		BufferedWriter writer = new BufferedWriter(new FileWriter("c:\\text.txt"));
		writer.write(str);
		writer.close();
		out.close();*/
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
