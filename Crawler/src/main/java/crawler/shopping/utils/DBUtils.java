package crawler.shopping.utils;

import java.sql.Connection;
import java.sql.DriverManager;

import org.apache.log4j.Logger;

public class DBUtils {
	private static final Logger log = Logger.getLogger(DBUtils.class);

	/**
	 * Driver와 URL, User, Password를 이용해 Connection객체를 반환한다.
	 * @param driver JDBC Driver Name (Oracle or MySQL 둘만 지원)
	 * @param url 접속 URL (IP 및 포트 및 SID 혹은 DB 지정)
	 * @param user 접속 사용자 아이디
	 * @param pwd 접속 사용자 패스워드
	 * @see #getConnection(DbConfiguration) 
	 * @return Connection 객체
	 */
	
	
	public static Connection getConnection(String driver, String url, String user, String pwd)  {
		Connection conn = null;
		try {
			if (driver.equalsIgnoreCase("Oracle")) {
				DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
				conn = DriverManager.getConnection(url, user,  pwd);
			}
			else if (driver.equalsIgnoreCase("MySQL")) {
				Class.forName("org.gjt.mm.mysql.Driver");
				conn = DriverManager.getConnection(url, user, pwd);	
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return conn;
	}
}
