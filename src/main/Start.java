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

import bot.Crawler;
import bot.Shuffler;
import bot.TradingBot;

public class Start {
	
	public static class stock {
		
		public String name;
		public BigDecimal value;
		public boolean newAktie;
		
		public stock(String name, BigDecimal value) {
			this.name = name;
			this.value = value;
			newAktie = true;
		}
	}
	
	public static class user {
		public TradingBot bot;
		public String username;
		public String password;
		public connection conn;
		
		public user(String username, String password) {
			this.username = username;
			this.password = password;
			bot = new TradingBot();
		}
	}
	
	public static class connection extends Thread {
		private Socket s;
		private Scanner reader;
		private PrintWriter writer;
		private String uName;
		
		public connection(Socket s) throws Throwable {
			this.s = s;
			this.reader = new Scanner(s.getInputStream());
			this.writer = new PrintWriter(s.getOutputStream());
			login();
		}
		
		public void login() {
			String choose = reader.nextLine();
			String name = reader.nextLine();
			String psw = reader.nextLine();
			userListLock.writeLock().lock();
			boolean notFound = true;
			try {
				if(choose.equals("Login")) {
					for(int i = 0; i < allUsers.size(); i++) {
						if(name.equals(allUsers.get(i).username) && psw.equals(allUsers.get(i).password) && notFound) {
							allUsers.get(i).conn = this;
							uName = allUsers.get(i).username;
							notFound = false;
						}
					}
				} else {
					if(!nameExists(name)) {
						
					}
				}
			} finally {
				userListLock.writeLock().unlock();
			}
			if(!notFound) {
				this.start();
			}
		}
		
		@Override
		public void run() {
			while(!s.isClosed()) {
				String input = reader.nextLine();
				if(input.equals("CLOSE")) {
					try {
						s.close();
						main.Start.closeConnection(uName);
					} catch (Throwable t) {}
				}
				
				//fixa fler alternativ
			}
		}
	}
	
	private static String map;
	public static Crawler scout;
	public static Shuffler dealer;
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
		dealer = new Shuffler();
		load();
		
		try {
			ss = new ServerSocket(8989);
			serverInput = new Thread(() -> {
				while(serverRunning) {
					try {
						ss.wait();
						new Thread(() -> {try {new connection(ss.accept());} catch (Throwable t) {}});
					} catch(Throwable t) {
						main.Start.errorLogg("Connecting to server: " + t.toString());
					}
				}
			});
			serverInput.start();
		} catch(Throwable t) {
			errorLogg("Tried to start the server: " + t.toString());
		}
		save();
	}
	
	public static void closeConnection(String uName) {
		userListLock.writeLock().lock();
		try {
			for(int i = 0; i < allUsers.size(); i++) {
				if(allUsers.get(i).username.equals(uName)) {
					allUsers.get(i).conn.stop();
					allUsers.get(i).conn = null;
				}
			}
		} finally {
			userListLock.writeLock().unlock();
		}
	}

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
