package crawler.shopping.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
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
	
	public Map<String, String> getProperties(String path){
		Map<String, String> m = new HashMap<String, String>();
		Properties pros = null;
		InputStream in = null;
		
		try {
			in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
			pros = new Properties();
			pros.load(in);
		} catch (IOException e) {
			log.error(e.toString()); 
			e.printStackTrace();
		}
		
		Iterator<Object> it =  pros.keySet().iterator();
		String key = null;

		while(it.hasNext()){
			key = (String)it.next();
			m.put(key, (String)pros.get(key));
		}
		return m;
	}

	public Connection getConnection() {
		
		conf = getProperties(filePath);
		String url = conf.get("mysql_url");
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
