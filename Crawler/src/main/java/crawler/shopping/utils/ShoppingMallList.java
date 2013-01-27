package crawler.shopping.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ShoppingMallList {

	final static String MALL_PATH = "/conf/ShoopingMallList.txt";

	public static ArrayList<String> getShoppingMallList() {
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

		return list;
	}

}
