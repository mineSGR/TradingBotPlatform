package bot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Crawler extends Thread {

	private final String website = "https://www.avanza.se/aktier/lista.html";
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
			try {
				Document src = Jsoup.connect(website).get();
				aktier = src.select("td");
			} catch(Throwable t) {
				main.Start.errorLogg(t.toString());
			}
			if(aktier != null && aktier.size() != 0) {
				ArrayList<String> names = new ArrayList<String>();
				ArrayList<String> storage = new ArrayList<String>();
				for(int i = 0; i < aktier.size(); i++) {
					if(!aktier.get(i).toString().contains("orderbookTools")) {
						if(i < 200) {
							String[] first = aktier.get(i).toString().split("</span>");
							String[] second = first[first.length-1].split("</a>");
							boolean exi = false;
							for(int j = 0; j < second.length; j++) {
								if(second[j].contains("Köp")) {
									exi = true;
								}
							}
							if(!exi) {
								names.add(second[0]);
							}
						} else {
							String[] tmp = aktier.get(i).toString().split(">");
							for(int j = 0; j < tmp.length; j++) {
								String[] temper = tmp[j].split("</");
								for(int m = 0; m < temper.length; m++) {
									
									if(temper[m] != null && !temper[m].contains("span") && !temper[m].contains("td") && !temper[m].equals(" ")) {
										if(temper[m].contains(",")) {
											String[] commTmp = temper[m].split(",");
											temper[m] = commTmp[0] + "." + commTmp[1];
										}
										String[] blankTmp = temper[m].split(" ");
										temper[m] = blankTmp[blankTmp.length-1];
										if(temper[m].contains("&nbsp")) {
											String[] comp = temper[m].split("&nbsp;");
											storage.add(comp[0] + " " + comp[1]);
										} else {
											storage.add(temper[m]);
										}
									}
								}
							}
						}
					}
				}
				convertStock(names, storage);
				shuffler();
			}
			try {
				Thread.sleep(300000);
			} catch(Throwable t) {
				main.Start.errorLogg(t.toString());
			}
		}
	}
	
	private void convertStock(ArrayList<String> names, ArrayList<String> storage) {
		lock.writeLock().lock();
		try {
			int valueCount = 0;
			for(int i = 0; i < names.size(); i++) {
				boolean foundPlace = false;
				for(int j = 0; j < stocks.size(); j++) {
					if(stocks.get(j).size() > 0) {
						if(stocks.get(j).get(0).name.equals(names.get(i))) {
							stocks.get(j).add(new main.Start.stock(names.get(i), new BigDecimal(storage.get(valueCount))));
							foundPlace = true;
						}
					}
				}
				if(!foundPlace) {
					stocks.add(new ArrayList<main.Start.stock>());
					stocks.get(stocks.size()-1).add(new main.Start.stock(names.get(i), new BigDecimal(storage.get(valueCount))));
				}
				valueCount += 8;
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private void shuffler() {
		main.Start.scout.lock.writeLock().lock();
		try {
			for(int j = 0; j < main.Start.scout.stocks.size(); j++) {
				if(main.Start.scout.stocks.get(j).size() > 0) {
					main.Start.stock tmpStock = main.Start.scout.stocks.get(j).get(0);
					main.Start.scout.stocks.get(j).remove(0);
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
			main.Start.scout.lock.writeLock().unlock();
		}
	}
}
