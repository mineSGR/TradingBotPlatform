package bot;

import java.util.ArrayList;

public class Shuffler extends Thread {
	
	private main.Start parent;
	
	public Shuffler(main.Start parent) {
		this.parent = parent;
		
		this.start();
	}
	
	public void run() {
		parent.scout.lock.writeLock().lock();
		try {
			for(int j = 0; j < parent.scout.stocks.size(); j++) {
				main.Start.stock tmpStock = parent.scout.stocks.get(j).get(0);
				parent.scout.stocks.get(j).remove(0);
				for(int i = 0; i < parent.allUsers.size(); i++) {
					boolean foundPlace = false;
					parent.allUsers.get(i).bot.aktieLock.writeLock().lock();
					try {
						for(int m = 0; m < parent.allUsers.get(i).bot.aktier.size(); m++) {
							if(parent.allUsers.get(i).bot.aktier.get(m).get(0).name.equals(tmpStock.name)) {
								parent.allUsers.get(i).bot.aktier.get(m).add(tmpStock);
								foundPlace = true;
							}
						}
						if(!foundPlace) {
							parent.allUsers.get(i).bot.aktier.add(new ArrayList<main.Start.stock>());
							parent.allUsers.get(i).bot.aktier.get(parent.allUsers.get(i).bot.aktier.size()-1).add(tmpStock);
						}
					} finally {
						parent.allUsers.get(i).bot.aktieLock.writeLock().unlock();
					}
				}
			}
		} finally {
			parent.scout.lock.writeLock().unlock();
		}
	}
}
