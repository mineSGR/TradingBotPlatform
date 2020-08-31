package bot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TradingBot extends Thread {

	public final int userID;
	private final int maxSameAktie = 5;
	public ArrayList<ArrayList<main.Start.stock>> aktier;
	public ReentrantReadWriteLock aktieLock;
	public boolean runner;
	public BigDecimal money;
	
	public TradingBot(int userID) {
		this.userID = userID;
		runner = true;
		aktier = new ArrayList<ArrayList<main.Start.stock>>();
		aktieLock = new ReentrantReadWriteLock();
		
		this.start();
	}
	
	public void run() {
		while(runner) {
			
		}
	}
}
