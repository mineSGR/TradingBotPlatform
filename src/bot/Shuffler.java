package bot;

import java.util.ArrayList;

public class Shuffler extends Thread {
	
	public Shuffler() {
		
	}
	
	public void run() {
		main.Start.scout.lock.writeLock().lock();
		try {
			for(int j = 0; j < main.Start.scout.stocks.size(); j++) {
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
		} finally {
			main.Start.scout.lock.writeLock().unlock();
		}
	}
}
