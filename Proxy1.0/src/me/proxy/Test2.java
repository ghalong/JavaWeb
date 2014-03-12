package me.proxy;

import java.util.ArrayList;
import java.util.List;

public class Test2 {
	public static List<Integer> list = new ArrayList<Integer>();

	public static void main(String[] args) {
		new Thread(){public void run() {
			System.out.println("run 1");
			add(2);
			System.out.println("end 1");
		}}.start();
		new Thread(){public void run() {
			System.out.println("run 2");
			contains(2);;
			System.out.println("end 2");
		}}.start();
	}
	
	public static void add(int i){
		synchronized (Test2.class) {
			clock(2000);
			list.add(i);
		}
	}
	
	public static void contains(int i){
		synchronized (Test2.class) {
			System.out.println(list.contains(i));
		}
	}
	
	public static void clock(long t){
		try {
			Thread.sleep(t);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
