package me.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {
	public static HashSet<String> scanedUrl = new HashSet<String>();

	public static void main(String[] args) throws IOException,
			InterruptedException {
		 test4();
//		HashSet<String> hosts = new HashSet<String>();
//		getHosts("www.taobao.com", 3, hosts, true);
//		System.out.println("below is the urls:");
//		for (String url : hosts) {
//			System.err.println(url);
//		}
//		System.out.println("size is " + hosts.size());
	}

	public static void test4() throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(
				"http://www.taobao.com").openConnection();
		System.out
				.println(conn.getResponseCode() + ":" + conn.getContentType());
		BufferedReader br = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		StringBuffer sBuf = new StringBuffer();
		String tmp = null;
		while ((tmp = br.readLine()) != null) {
			sBuf.append(tmp + "\n");
		}
//		Pattern p = Pattern.compile("(\\w{1,10}\\.){1,3}(com|cn)(\\/|\\w)*");
		Pattern p = Pattern.compile("(\\w{1,16}\\.){1,3}(com|cn)(\\/|\\w|\\=|\\?)*");
		Matcher m = p.matcher(sBuf.toString());
		while (m.find()) {
			System.out.println(m.group());
		}
	}

	/**
	 * 
	 * @param url
	 * @param deep
	 * @param hosts
	 */
	public static void getHosts(String url, int deep,
			final HashSet<String> hosts, boolean urlDistinguish) {
		StringBuffer pageContent = new StringBuffer();
		hosts.add(getHost(url));
		if (urlDistinguish)
			scanedUrl.add(url);
		else {
			scanedUrl.add(getHost(url));
		}
		// String ip = null;
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL("http://"
					+ url).openConnection();
			conn.setReadTimeout(1000);
			if (conn.getResponseCode() == 200
					&& conn.getContentType().toUpperCase().startsWith("TEXT")) {
				BufferedReader br = new BufferedReader(new InputStreamReader(
						conn.getInputStream()));
				String tmp = null;
				while ((tmp = br.readLine()) != null) {
					pageContent.append(tmp + "\n");
				}
			} else {
				System.err.println("The url " + url
						+ " returned unexpected response code: "
						+ conn.getResponseCode());
			}
		} catch (Exception e) {
			System.out.println("exception occured" + e.getMessage());
		}
		// Pattern p = Pattern.compile("(\\w{1,10}\\.){1,3}(com|cn)");
		Pattern p = Pattern.compile("(\\w{1,16}\\.){1,3}(com|cn)(\\/|\\w)*");
		Matcher m = p.matcher(pageContent.toString());
		while (m.find()) {
			final String urlTmp = m.group();
			String host = getHost(urlTmp);
			hosts.add(host);
			if (urlDistinguish) {
				if (!scanedUrl.contains(urlTmp) && deep > 1) {
					int deepTmp = deep - 1;
					getHosts(urlTmp, deepTmp, hosts, urlDistinguish);
				}
			} else {
				if (!scanedUrl.contains(host) && deep > 1) {
					int deepTmp = deep - 1;
					getHosts(urlTmp, deepTmp, hosts, urlDistinguish);

				}
			}
		}
	}

	public static void addScaned(String url) {
		synchronized (Test.class) {

		}
	}

	public static void containScaned(String url) {
		synchronized (Test.class) {
			// return
		}
	}

	public static String getHost(String url) {
		int index = url.indexOf("/");
		if (index > 0)
			url = url.substring(0, index);
		return url;
	}

	public static void test3() {
		HashSet<String> scaned = new HashSet<String>();
		String first = "www.taobao.com";
		HashSet<String> urls = test2(first);
		HashSet<String> copy = new HashSet<String>();
		copy.addAll(urls);
		scaned.add(first);
		for (String url : copy) {
			if (scaned.contains(url))
				continue;
			urls.addAll(test2(url));
			scaned.add(url);
		}
		copy.addAll(urls);
		for (String url : copy) {
			if (scaned.contains(url))
				continue;
			urls.addAll(test2(url));
			scaned.add(url);
		}
		System.out.println("size " + urls.size());
		StringBuffer hosts = new StringBuffer();
		for (String url : urls) {
			System.out.println(url);
			hosts.append("115.28.132.207 " + url + "\r\n");
		}
		try {
			FileOutputStream f = new FileOutputStream("hosts.txt");
			f.write(hosts.toString().getBytes());
			f.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(hosts);
	}

	public static HashSet<String> test2(String addr) {
		addr = "http://" + addr;
		StringBuffer sb = new StringBuffer();
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(addr)
					.openConnection();
			conn.setReadTimeout(3000);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					conn.getInputStream(), "GBK"));
			String tmp = null;

			while ((tmp = br.readLine()) != null) {
				sb.append(tmp + "\n");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		// System.out.println(sb);
		// Pattern p = Pattern.compile("(\\w{1,10}\\.){1,3}com");
		Pattern p = Pattern.compile("(\\w{1,10}\\.){1,3}(com|cn)");
		Matcher m = p.matcher(sb.toString());
		HashSet<String> urls = new HashSet<String>();
		while (m.find()) {
			urls.add(m.group());
		}
		return urls;
	}

	public void test1() throws IOException {
		// ServerSocket ss = new ServerSocket(80);
		// Socket s = ss.accept();
		// System.out.println(s.get);
		System.out.println("Abcf".startsWith("abc"));
		FileOutputStream f = new FileOutputStream("aa.txt");
		f.write("abcdefg\r\nhijklmn\nopqrstuvwxyz".getBytes());
		f.close();
		FileInputStream in = new FileInputStream("aa.txt");
		BufferedInputStream bis = new BufferedInputStream(in);
		System.out.println("xxx\r\nooo\nfff\reee\n\rjj");
		// if()
		int tmp = -1;
		while ((tmp = in.read()) != -1) {
			if (tmp == '\r') {
				System.out.println("r");
				in.read();
			} else if (tmp == '\n') {
				System.out.println("n");
			}
		}
		System.out.println(in.read());
		InputStreamReader isr = new InputStreamReader(in);
		StringWriter sw = new StringWriter();
		System.out.println("len " + "\r".length());
		char[] buf = new char[2];
		System.out.println(isr.read(buf));
		System.out.println(buf[0] + ":" + buf[1]);
	}

}
