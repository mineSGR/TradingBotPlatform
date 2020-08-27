package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;

import bot.Crawler;
import bot.TradingBot;

public class Start {
	
	public static class stock {
		
		public String name;
		public BigDecimal value;
		
		public stock(String name, BigDecimal value) {
			this.name = name;
			this.value = value;
		}
	}
	
	private static String map;
	public static Crawler scout;
	public static ArrayList<TradingBot> botArmy;
	
	public static void main(String[] args) {
		load();
		map = System.getenv("Appdata") + "\\SebbeProduktion\\TradingBot";
		botArmy = new ArrayList<TradingBot>();
		scout = new Crawler();
		save();
	}
	
	private static void save() {
		File file = new File(map);
		if(!file.exists()) {
			file.mkdirs();
		}
		file = new File(map + "\\SaveFile.txt");
		if(!file.exists()) {
			try {
				file.createNewFile();
			} catch (Throwable t) {
				errorLogg(t.toString());
			}
		}
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
			oos.writeObject(botArmy);
			scout.lock.readLock().lock();
			try {
				oos.writeObject(scout.stocks);
			} finally {
				scout.lock.readLock().unlock();
			}
			oos.close();
		} catch(Throwable t) {
			errorLogg(t.toString());
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void load() {
		File file = new File(map);
		if(!file.exists()) {
			file.mkdirs();
		}
		file = new File(map + "\\SaveFile.txtt");
		if(!file.exists()) {
			try {
				file.createNewFile();
			} catch (Throwable t) {
				errorLogg(t.toString());
			}
		}
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
			ArrayList<TradingBot> tmpArrList = (ArrayList<TradingBot>) ois.readObject();
			scout.lock.writeLock().lock();
			try {
				ArrayList<ArrayList<stock>> tmpArrArrList = (ArrayList<ArrayList<stock>>) ois.readObject();
				if(tmpArrArrList != null) {
					scout.stocks = tmpArrArrList;
				}
			} finally {
				scout.lock.writeLock().unlock();
			}
			if(tmpArrList != null) {
				botArmy = tmpArrList;
			}
			ois.close();
		} catch(Throwable t) {
			errorLogg(t.toString());
		}
	}
	
	public static void errorLogg(String error) {
		File file = new File(map);
		if(!file.exists()) {
			file.mkdirs();
		}
		file = new File(map + "\\Error.txt");
		if(!file.exists()) {
			try {
				file.createNewFile();
			} catch (Throwable t) {}
		}
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
			bw.append("Error: " + error + "\n");
			bw.close();
		} catch (Throwable t) {}
	}
}
