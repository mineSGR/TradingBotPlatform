package bot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import main.Start.stock;

public class Crawler extends Thread {

	private final String website = "https://finviz.com/screener.ashx?v=111&f=cap_small,sh_avgvol_o1000,sh_curvol_o500,sh_price_u10,sh_relvol_o1";
	public boolean running;
	public ArrayList<ArrayList<main.Start.stock>> stocks;
	public ReadWriteLock lock;
	
	public Crawler() {
		running = true;
		stocks = new ArrayList<ArrayList<main.Start.stock>>();
		lock = new ReentrantReadWriteLock();
		
		this.start();
	}
	
	public void run() {
		while(running) {
			Elements aktier = null;
			ArrayList<String> storage = new ArrayList<String>();
			try {
				Document src = Jsoup.connect(website).get();
				aktier = src.select("body");
			} catch(Throwable t) {
				main.Start.errorLogg(t.toString());
			}
			if(aktier != null && aktier.size() != 0) {
				Elements el = aktier.select("table").get(16).select("tr");
				for(int i = 1; i < el.size(); i++) {
					for(int j = 1; j < el.get(i).select("td").size(); j++) {
						if(j > 7 || j == 1) {
							String[] tmp = el.get(i).select("td").get(j).toString().split(">");
							int numb = 0;
							if(j == 8 || j == 9) {
								numb = 3;
							} else {
								numb = 2;
							}
							String done = tmp[tmp.length-numb].split("</")[0];
							done = cutter(cutter(cutter(done, ","), " "), "%");
							storage.add(done);
						}
					}
				}				
			}
			convertStock(storage);
			shuffler();
		}
		try {
			Thread.sleep(300000);
		} catch(Throwable t) {
			main.Start.errorLogg(t.toString());
		}
	}

	private static String cutter(String done, String regex) {
		if(done.contains(regex)) {
			String[] tmpSplit = done.split(regex);
			done = "";
			for(int s = 0; s < tmpSplit.length; s++) {
				done += tmpSplit[s];
			}
		}
		return done;
	}
	
	private void convertStock(ArrayList<String> storage) {
		lock.writeLock().lock();
		try {
			for(int i = 0; i < storage.size(); i += 11) {
				boolean foundPlace = false;
				main.Start.stock tmpStock = new main.Start.stock(storage.get(i+1), new BigDecimal(storage.get(i+8)), new BigDecimal(storage.get(i+9)), Integer.parseInt(storage.get(i+10)));
				for(int j = 0; j < stocks.size(); j++) {
					if(stocks.get(j).size() > 0) {
						if(stocks.get(j).get(0).name.equals(storage.get(i+1))) {
							stocks.get(j).add(tmpStock);
							foundPlace = true;
						}
					}
				}
				if(!foundPlace) {
					stocks.add(new ArrayList<main.Start.stock>());
					stocks.get(stocks.size()-1).add(tmpStock);
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private void shuffler() {
		lock.writeLock().lock();
		try {
			for(int j = 0; j < stocks.size(); j++) {
				if(stocks.get(j).size() > 0) {
					main.Start.stock tmpStock = stocks.get(j).get(0);
					stocks.get(j).remove(0);
					for(int i = 0; i < main.Start.allUsers.size(); i++) {
						boolean foundPlace = false;
						main.Start.allUsers.get(i).bot.aktieLock.writeLock().lock();
						try {
							for(int m = 0; m < main.Start.allUsers.get(i).bot.aktier.size(); m++) {
								if(main.Start.allUsers.get(i).bot.aktier.get(m).get(0).name.equals(tmpStock.name)) {
									main.Start.allUsers.get(i).bot.aktier.get(m).add(tmpStock);
									foundPlace = true;
								}
							}
							if(!foundPlace) {
								main.Start.allUsers.get(i).bot.aktier.add(new ArrayList<main.Start.stock>());
								main.Start.allUsers.get(i).bot.aktier.get(main.Start.allUsers.get(i).bot.aktier.size()-1).add(tmpStock);
							}
						} finally {
							main.Start.allUsers.get(i).bot.aktieLock.writeLock().unlock();
						}
					}
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
}
