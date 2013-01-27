package crawler.shopping.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import crawler.shopping.http.HttpGetRequest;

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
		HttpGetRequest get = new HttpGetRequest();
		PrintWriter out = response.getWriter();
		out.println(get.getHttpRequest("http://us.topshop.com/webapp/wcs/stores/servlet/ProductDisplay?beginIndex=1&viewAllFlag=&catalogId=33060&storeId=13052&productId=6719578&langId=-1&sort_field=Relevance&categoryId=208634&parent_categoryId=208580&pageSize=20"));
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
