package bot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import main.Start.Recipt;

public class TradingBot extends Thread {

	public static class tracement {
		public int highTrace;
		public int lowTrace;
		public int highCount;
		public BigDecimal minSell;
		
		public tracement() {
			this.highTrace = 0;
			this.lowTrace = 0;
			this.highCount = 0;
			minSell = null;
		}
	}
	
	public static class trendSaver {
		public ArrayList<main.Start.stock> compDay = null;
		public ArrayList<main.Start.stock> newDay = null;
		public int compareValue = 0;
		public int upptrendValue = 0;
		public int counter = 0;
	}
	
	private final int maxSameAktie = 5;
	private final int antalSparadeDagar = 70;
	public ArrayList<ArrayList<ArrayList<main.Start.stock>>> aktier;
	private ArrayList<tracement> traces;
	private trendSaver[] trendValues;
	public ReentrantReadWriteLock aktieLock;
	public ArrayList<main.Start.stock[]> boughtAktie;
	public ArrayList<Recipt> recipts;
	public ReentrantReadWriteLock reciptLock;
	public boolean runner;
	public BigDecimal money;
	
	public TradingBot() {
		runner = true;
		money = new BigDecimal(0);
		aktier = new ArrayList<ArrayList<ArrayList<main.Start.stock>>>();
		traces = new ArrayList<tracement>();
		trendValues = new trendSaver[20];
		aktieLock = new ReentrantReadWriteLock();
		boughtAktie = new ArrayList<main.Start.stock[]>();
		recipts = new ArrayList<Recipt>();
		reciptLock = new ReentrantReadWriteLock();
	}
	
	public void run() {
		while(runner) {
			aktieLock.writeLock().lock();
			try {
				for(int i = 0; i < aktier.get(aktier.size()-1).size(); i++) {
					if(aktier.size() > antalSparadeDagar) {
						aktier.remove(0);
					}
					if(traces.get(i).highCount == 0) {
						int dagar = 0;
						if(higestLowest(0, aktier.get(aktier.size()-1).get(i)).compareTo(higestLowest(0, aktier.get(aktier.size()-2).get(i))) == 1 && higestLowest(1, aktier.get(aktier.size()-1).get(i)).compareTo(higestLowest(1, aktier.get(aktier.size()-2).get(i))) == 1) {
							traces.get(i).highTrace++;
							dagar = traces.get(i).lowTrace;
							traces.get(i).lowTrace = 0;
						} else if(higestLowest(0, aktier.get(aktier.size()-1).get(i)).compareTo(higestLowest(0, aktier.get(aktier.size()-2).get(i))) == -1 && higestLowest(1, aktier.get(aktier.size()-1).get(i)).compareTo(higestLowest(1, aktier.get(aktier.size()-2).get(i))) == -1) {
							traces.get(i).lowTrace++;
							dagar = traces.get(i).highTrace;
							traces.get(i).highTrace = 0;
						}
						if(dagar > 4 && traces.get(i).lowTrace > 0) {
							traces.get(i).highCount = Math.floorDiv(dagar, 4)+1;
						}
					} else if(traces.get(i).highCount > 0) {
						if(traces.get(i).highCount == 1) {
							buy(i, 0);
						}
						traces.get(i).highCount--;
					}
					
					
					if(trendValues[i].compDay == null) {
						trendValues[i].compDay = aktier.get(aktier.size()-1).get(i);
					} else {
						trendValues[i].newDay = aktier.get(aktier.size()-1).get(i);
						if(trendValues[i].counter < 22) {
							trendValues[i].counter++;
							trendValues[i].compareValue = higestLowest(0, trendValues[i].compDay).compareTo(higestLowest(0, trendValues[i].newDay));
						} else if(trendValues[i].compareValue > 4 && trendValues[i].upptrendValue < 8) {
								if(higestLowest(0, trendValues[i].compDay).compareTo(higestLowest(0, trendValues[i].newDay)) == -1) {
									trendValues[i].upptrendValue++;
								} else if(higestLowest(0, trendValues[i].compDay).compareTo(higestLowest(0, trendValues[i].newDay)) == 1) {
									if(trendValues[i].upptrendValue > 0) {
										trendValues[i].upptrendValue--;
									}
									trendValues[i].compDay = trendValues[i].newDay;
								}
								for(int n = 0; n < aktier.get(aktier.size()-1).get(i).size(); n++) {
									if(aktier.get(aktier.size()-1).get(i).get(n).volume > (aktier.get(aktier.size()-2).get(i).get(aktier.get(aktier.size()-2).get(i).size()-1).volume*2)) {
										if(money.compareTo(aktier.get(aktier.size()-1).get(i).get(aktier.get(aktier.size()-1).get(i).size()-1).price) == 1) {
											for(int j = 0; j < boughtAktie.size(); j++) {
												if(boughtAktie.get(j)[0].name.equals(aktier.get(aktier.size()-1).get(i).get(aktier.get(aktier.size()-1).get(i).size()-1).name)) {
													if(längd(boughtAktie.get(j)) < 5) {
														money.subtract(aktier.get(aktier.size()-1).get(i).get(aktier.get(aktier.size()-1).get(i).size()-1).price);
														boolean foundPlace = false;
														for(int m = 0; m < boughtAktie.get(j).length; m++) {
															if(boughtAktie.get(j)[m] == null && !foundPlace) {
																boughtAktie.get(j)[m] = aktier.get(aktier.size()-1).get(i).get(n);
																boughtAktie.get(j)[m].algo = 1;
																foundPlace = true;
															}
														}
													}
												}
											}
										}
										int priceCounter = 0;
										for(int o = n; o < aktier.get(aktier.size()-1).get(i).size(); o++) {
											if(aktier.get(aktier.size()-1).get(i).get(o).price.compareTo(aktier.get(aktier.size()).get(i).get(n).price) == 1) {
												priceCounter++;
											} else if(aktier.get(aktier.size()-1).get(i).get(o).price.compareTo(aktier.get(aktier.size()).get(i).get(n).price) == -1) {
												priceCounter--;
											}
											if(priceCounter == 2 || priceCounter == -2) {
												for(int t = 0; t < boughtAktie.size(); t++) {
													for(int j = 0; j < aktier.get(aktier.size()-1).size(); j++) {
														if(boughtAktie.get(t)[0].name.equals(aktier.get(aktier.size()-1).get(j).get(0).name)) {
															for(int m = 0; m < längd(boughtAktie.get(t)); m++) {
																if(boughtAktie.get(t)[m].algo == 1)  {
																	money.add(higestLowest(0, aktier.get(aktier.size()-1).get(j)));
																	reciptLock.writeLock().lock();
																	try {
																		recipts.add(new Recipt(boughtAktie.get(t)[m].name, boughtAktie.get(t)[m].price, higestLowest(0, aktier.get(aktier.size()-1).get(j))));
																		boughtAktie.get(t)[m] = null;
																	} finally {
																		reciptLock.writeLock().unlock();
																	}
																}
															}
														}
													}
												}
												o = aktier.get(aktier.size()).get(i).size();
											}
										}
									}
								}
								
						} else {
							trendValues[i].compDay = null;
							trendValues[i].counter = 0;
							trendValues[i].newDay = null;
							trendValues[i].compareValue = 0;
							trendValues[i].upptrendValue = 0;
						}
					}
					
					
					
					
				}	
			} finally {
				aktieLock.writeLock().unlock();
			}
			for(int i = 0; i < boughtAktie.size(); i++) {
				for(int j = 0; j < aktier.get(aktier.size()-1).size(); j++) {
					if(boughtAktie.get(i)[0].name.equals(aktier.get(aktier.size()-1).get(j).get(0).name)) {
						for(int m = 0; m < längd(boughtAktie.get(i)); m++) {
							BigDecimal countDec = new BigDecimal(boughtAktie.get(i)[m].price.toString()).multiply(new BigDecimal("1.1"));
							if(higestLowest(0, aktier.get(aktier.size()-1).get(j)).compareTo(countDec) == 1 && boughtAktie.get(i)[m].algo == 0)  {
								money.add(higestLowest(0, aktier.get(aktier.size()-1).get(j)));
								reciptLock.writeLock().lock();
								try {
									recipts.add(new Recipt(boughtAktie.get(i)[m].name, boughtAktie.get(i)[m].price, higestLowest(0, aktier.get(aktier.size()-1).get(j))));
									boughtAktie.get(i)[m] = null;
								} finally {
									reciptLock.writeLock().unlock();
								}
							}
						}
					}
				}
			}
		}
	}
	
	//0 ger högsta värdet och 1 ger lägsta värdet
	private BigDecimal higestLowest(int val, ArrayList<main.Start.stock> input) {
		BigDecimal curr = input.get(0).price;
		for(int i = 1; i < input.size(); i++) {
			if(val == 0) {
				if(curr.compareTo(input.get(i).price) == 1) {
					curr = input.get(i).price;
				}
			} else if(val == 1) {
				if(curr.compareTo(input.get(i).price) == -1) {
					curr = input.get(i).price;
				}
			}
		}
		return curr;
	}
	
	private void buy(int i, int a) {
		if(money.compareTo(aktier.get(aktier.size()-1).get(i).get(aktier.get(aktier.size()-1).get(i).size()-1).price) == 1) {
			for(int j = 0; j < boughtAktie.size(); j++) {
				if(boughtAktie.get(j)[0].name.equals(aktier.get(aktier.size()-1).get(i).get(aktier.get(aktier.size()-1).get(i).size()-1).name)) {
					if(längd(boughtAktie.get(j)) < 5) {
						money.subtract(aktier.get(aktier.size()-1).get(i).get(aktier.get(aktier.size()-1).get(i).size()-1).price);
						boolean foundPlace = false;
						for(int m = 0; m < boughtAktie.get(j).length; m++) {
							if(boughtAktie.get(j)[m] == null && !foundPlace) {
								boughtAktie.get(j)[m] = aktier.get(aktier.size()-1).get(i).get(aktier.get(aktier.size()-1).get(i).size()-1);
								boughtAktie.get(j)[m].algo = a;
								foundPlace = true;
							}
						}
					}
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
