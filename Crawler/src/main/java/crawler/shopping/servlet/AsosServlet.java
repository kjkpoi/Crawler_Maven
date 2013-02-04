package crawler.shopping.servlet;

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

import crawler.shopping.data.XmlData;
import crawler.shopping.parser.SixPm;
import crawler.shopping.parser.XmlProductParser;

/**
 * Servlet implementation class AsosServlet
 */
@WebServlet("/Asos")
public class AsosServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AsosServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#service(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		int count = 0;
		int gap = 100;
		int start = 0;
		ArrayList<XmlData> xmlDataList = new ArrayList<>();
		XmlProductParser xmlParser = new XmlProductParser();
		SixPm sixPmParser = new SixPm();
		
		SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat ( "yyyy.MM.dd HH:mm:ss", Locale.KOREA );
		Date currentTime = new Date ( );
		String mTime = mSimpleDateFormat.format ( currentTime );

		
		while(true){
			xmlDataList.clear();
			xmlDataList = xmlParser.getProduceCodeList(3, start, start + gap);
			count += xmlDataList.size();
			
			for(int i = 0 ; i < xmlDataList.size(); i++){
				sixPmParser.run(xmlDataList.get(i), mTime);
			}
			
			if(count % (gap * 10) == 0){
				out.println(count + " parsed");
				out.flush();
			}
			
			
			if(xmlDataList.size() != gap){
				break;
			}
		}
		out.close();
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
