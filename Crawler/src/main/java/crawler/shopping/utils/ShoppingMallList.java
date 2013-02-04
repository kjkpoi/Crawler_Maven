package crawler.shopping.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ShoppingMallList {

	final static String MALL_PATH = "/ShoppingMallList.txt";

	public static ArrayList<String> getShoppingMallList() {
		ArrayList<String> list = new ArrayList<String>();

		InputStreamReader iReader = null;
		BufferedReader bufReader = null;
		InputStream in = null;
		in = Thread.currentThread().getContextClassLoader().getResourceAsStream(MALL_PATH);
		iReader = new InputStreamReader(in);
		bufReader = new BufferedReader(iReader);
		
		String buf = null;

		try {
			while ((buf = bufReader.readLine()) != null) {
				list.add(buf);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return list;
	}
	
	public static String getShoppingMallName(int number) {
		ArrayList<String> list = new ArrayList<String>();

		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(MALL_PATH));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		String buf = null;

		try {
			while ((buf = in.readLine()) != null) {
				list.add(buf);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return list.get(number);
	}

}
