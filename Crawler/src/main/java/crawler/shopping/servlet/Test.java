package crawler.shopping.servlet;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import crawler.shopping.http.HttpGetRequest;
import crawler.shopping.parser.SixPm;

/**
 * Servlet implementation class Test3
 */
@WebServlet(description = "Test", urlPatterns = { "/Test" })
public class Test extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
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
		
		SixPm sixPm = new SixPm();
		sixPm.run();
		
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
