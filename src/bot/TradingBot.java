package bot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TradingBot extends Thread {

	private final int maxSameAktie = 5;
	public ArrayList<ArrayList<main.Start.stock>> aktier;
	public ReentrantReadWriteLock aktieLock;
	private ArrayList<main.Start.stock[]> boughtAktie;
	public boolean runner;
	public BigDecimal money;
	
	public TradingBot() {
		runner = true;
		aktier = new ArrayList<ArrayList<main.Start.stock>>();
		aktieLock = new ReentrantReadWriteLock();
		boughtAktie = new ArrayList<main.start.Stock[]>();
		this.start();
	}
	
	public void run() {
		while(runner) {
			for(int i = 0; i < aktier.size(); i++) {
				if(aktier.get(i).get(aktier.get(i).size()-1).newAktie) {
					
					aktier.get(i).get(aktier.get(i).size()-1).newAktie = false;
				}
			}
			for(int i = 0; i < boughtAktie.size(); i++) {
				
			}
		}
	}
}
