package crawler.shopping.db;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * DBConnector 을 구현한 MySql Connection Class
 * 
 * @author kjkpoi
 * 
 */
public class MySqlConnector {

	private Logger log = Logger.getLogger(this.getClass());
	private Map<String, String> conf;
	private final String filePath = "/mysql_db_connection.properties";
	private Properties properties;

	public Connection getConnection() {

		this.properties = new Properties();
		try {
			// 한글이 깨지는 문제 때문에 인코딩을 지정해서 읽을수 있도록 함.
			this.properties.load(new BufferedReader(new InputStreamReader(
					new FileInputStream(filePath), "UTF-8")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
		String url = properties.getProperty("mysql_url");
		String id = conf.get("mysql_id");
		String pwd = conf.get("mysql_pwd");
		Connection conn = null;

		try {
			Class.forName("org.gjt.mm.mysql.Driver");
			conn = DriverManager.getConnection(url, id, pwd);
		} catch (ClassNotFoundException e) {
			log.warn(e.toString());
			e.printStackTrace();
		} catch (SQLException e) {
			log.warn(e.toString());
			e.printStackTrace();
		}

		return conn;
	}

}
