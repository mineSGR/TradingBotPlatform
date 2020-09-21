package bot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TradingBot extends Thread {

	private final int maxSameAktie = 5;
	private final int antalSparadeAktier = 100;
	public ArrayList<ArrayList<main.Start.stock>> aktier;
	public ReentrantReadWriteLock aktieLock;
	private ArrayList<main.Start.stock[]> boughtAktie;
	public boolean runner;
	public BigDecimal money;
	
	public TradingBot() {
		runner = true;
		aktier = new ArrayList<ArrayList<main.Start.stock>>();
		aktieLock = new ReentrantReadWriteLock();
		boughtAktie = new ArrayList<main.Start.stock[]>();
		this.start();
	}
	
	public void run() {
		while(runner) {
			aktieLock.writeLock().lock();
			try {
				for(int i = 0; i < aktier.size(); i++) {
					if(aktier.get(i).size() > antalSparadeAktier) {
						aktier.get(i).remove(0);
					}
					if(aktier.get(i).get(aktier.get(i).size()-1).newAktie) {
						
						aktier.get(i).get(aktier.get(i).size()-1).newAktie = false;
					}
				}
			} finally {
				aktieLock.writeLock().unlock();
			}
			for(int i = 0; i < boughtAktie.size(); i++) {
				for(int j = 0; j < längd(boughtAktie.get(i)); i++) {
					
				}
			}
		}
	}
	
	public int längd(main.Start.stock[] arr) {
		int längd = 0;
		for(int i = 0; i < arr.length; i++) {
			if(arr[i] != null) {
				längd++;
			}
		}
		return längd;
	}
}
