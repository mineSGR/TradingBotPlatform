package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.JOptionPane;

import bot.Crawler;
import bot.TradingBot;

public class Start {
	
	public static class stock {
		
		public String name;
		public BigDecimal price;
		public BigDecimal change;
		public int volume;
		public int algo;
		
		public stock(String name, BigDecimal price, BigDecimal change, int volume) {
			this.name = name;
			this.price = price;
			this.change = change;
			this.volume = volume;
		}
	}
	
	public static class Recipt {
		public String name;
		public BigDecimal boughtPrice;
		public BigDecimal sellPrice;
		
		public Recipt(String name, BigDecimal boughtPrice, BigDecimal sellPrice) {
			this.name = name;
			this.boughtPrice = boughtPrice;
			this.sellPrice = sellPrice;
		}
	}
	
	public static class user {
		public TradingBot bot;
		public String username;
		public String password;
		
		public user(String username, String password) {
			this.username = username;
			this.password = password;
			bot = new TradingBot();
		}
	}
	
	public static class connection {
		private Socket s;
		private Scanner txtReader;
		private PrintWriter txtWriter;
		private ObjectOutputStream objectWriter;
		
		public connection(Socket s) throws Throwable {
			this.s = s;
			this.txtReader = new Scanner(s.getInputStream());
			this.txtWriter = new PrintWriter(s.getOutputStream());
			this.objectWriter = new ObjectOutputStream(s.getOutputStream());
		}
		
		//Methoden som hanterar när en client kopplar upp sig mot servern
		public void activate() {
			String choose = txtReader.nextLine();
			String name = txtReader.nextLine();
			String psw = txtReader.nextLine();
			boolean found = false;
			if(choose.equals("LOGIN")) {
				userListLock.readLock().lock();
				try {
					for(int i = 0; i < allUsers.size(); i++) {
						if(name.equals(allUsers.get(i).username) && psw.equals(allUsers.get(i).password) && !found) {
							found = true;
							txtWriter.println("TRUE");
							txtWriter.flush();
							try {
								allUsers.get(i).bot.aktieLock.readLock().lock();
								allUsers.get(i).bot.reciptLock.readLock().lock();
								try {
									objectWriter.writeObject(allUsers.get(i).bot.boughtAktie);
									objectWriter.writeObject(allUsers.get(i).bot.recipts);
									String bdValue = "0";
									if(allUsers.get(i).bot.money.compareTo(new BigDecimal(0)) != 0) {
										bdValue = allUsers.get(i).bot.money.toString();
									}
									txtWriter.println(bdValue);
									objectWriter.flush();
									txtWriter.flush();
								} finally {
									allUsers.get(i).bot.aktieLock.readLock().unlock();
									allUsers.get(i).bot.reciptLock.readLock().unlock();
								}
							} catch(Throwable t) {
								main.Start.errorLogg(t.toString());
								try {s.close();} catch(Throwable t2) {}
							}
						}
					}
				} finally {
					userListLock.readLock().unlock();
				}
				if(!found) {
					txtWriter.println("FALSE");
					txtWriter.flush();
				}
			} else if(choose.equals("CREATEACCOUNT")) {
				if(!nameExists(name)) {
					userListLock.writeLock().lock();
					try {
						allUsers.add(new user(name, psw));
						txtWriter.println("TRUE");
					} finally {
						userListLock.writeLock().unlock();
					}
				} else {
					txtWriter.println("FALSE");
				}
				txtWriter.flush();
			} else if(choose.equals("MONEY")) {
				int input = txtReader.nextInt();
				userListLock.readLock().lock();
				try {
					for(int i = 0; i < allUsers.size(); i++) {
						if(name.equals(allUsers.get(i).username) && psw.equals(allUsers.get(i).password)) {
							try {
								allUsers.get(i).bot.money = allUsers.get(i).bot.money.add(new BigDecimal(input));
							} catch(Throwable t) {
								main.Start.errorLogg(t.toString());
								try {s.close();} catch(Throwable t2) {}
							}
						}
					}
				} finally {
					userListLock.readLock().unlock();
				}
			}
			try {
				s.close();
			} catch(Throwable t) {
				errorLogg("Kunde inte stänga socket: " + t.toString());
			}
		}
	}
	
	private static String map;
	public static Crawler scout;
	public static ArrayList<user> allUsers;
	public static ReentrantReadWriteLock userListLock;
	
	private static boolean serverRunning = true;
	public static ServerSocket ss;
	private static Thread serverInput;
	
	public static void main(String[] args) {
		map = System.getenv("Appdata") + "\\SebbeProduktion\\TradingBot";
		allUsers = new ArrayList<user>();
		userListLock = new ReentrantReadWriteLock();
		scout = new Crawler();
		load();
		scout.start();
		try {
			ss = new ServerSocket(8989);
			serverInput = new Thread(() -> {
				while(serverRunning) {
					try {
						Socket s = ss.accept();
						new Thread (() -> {try {connection con = new connection(s); con.activate();} catch(Throwable t) {main.Start.errorLogg("Error connecting to client: " + t.toString());}}).start();
						
					} catch(Throwable t) {
						main.Start.errorLogg("ServerError: " + t.toString());
					}
				}
			});
			serverInput.start();
		} catch(Throwable t) {
			errorLogg("Tried to start the server: " + t.toString());
			serverInput.stop();
			try {ss.close();} catch (Throwable t2) {}
			scout.stop();
			for(int i = 0; i < allUsers.size(); i++) {
				allUsers.get(i).bot.stop();
			}
			save();
			System.exit(0);
		}
		JOptionPane.showMessageDialog(null, "Servern körs, klicka okej för att stoppa");
		JOptionPane.showMessageDialog(null, "Stänger av efter denna");
		serverInput.stop();
		try {ss.close();} catch (Throwable t2) {}
		scout.stop();
		for(int i = 0; i < allUsers.size(); i++) {
			allUsers.get(i).bot.stop();
		}
		save();
		System.exit(0);
	}
	
	//Methoden kollar ifall användarnamnet redan finns
	private static boolean nameExists(String name) {
		userListLock.readLock().lock();
		boolean found = false;
		try {
			for(int i = 0; i < allUsers.size(); i++) {
				if(allUsers.get(i).username.equals(name)) {
					found = true;
				}
			}
		} finally {
			userListLock.readLock().unlock();
		}
		return found;
	}
	
	//Methoden sparar den nödvändiga informationen innan nedstängning
	private static void save() {
		File file = new File(map);
		if(!file.exists()) {
			file.mkdirs();
		}
		file = new File(map + "\\" + "SaveFile.txt");
		if(!file.exists()) {
			try {
				file.createNewFile();
			} catch (Throwable t) {
				errorLogg(t.toString());
			}
		}
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
			userListLock.writeLock().lock();
			try {
				oos.writeObject(allUsers);
			} finally {
				userListLock.writeLock().unlock();
			}
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
	
	//Methoden laddar in nödvändig information när programmet startas
	private static void load() {
		File file = new File(map);
		if(!file.exists()) {
			file.mkdirs();
		}
		file = new File(/*map + "\\*/"SaveFile.txt");
		if(!file.exists()) {
			try {
				file.createNewFile();
			} catch (Throwable t) {
				errorLogg(t.toString());
			}
		}
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
			ArrayList<user> tmpArrUser = (ArrayList<user>) ois.readObject();
			ArrayList<ArrayList<stock>> tmpArrArrStock = (ArrayList<ArrayList<stock>>) ois.readObject();
			if(tmpArrArrStock != null) {
				scout.lock.writeLock().lock();
				try {
					scout.stocks = tmpArrArrStock;
				} finally {
					scout.lock.writeLock().unlock();
				}
			}
			if(tmpArrUser != null) {
				userListLock.writeLock().lock();
				try {
					allUsers = tmpArrUser;
				} finally {
					userListLock.writeLock().unlock();
				}
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
		file = new File(map + "\\" + "Error.txt");
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
