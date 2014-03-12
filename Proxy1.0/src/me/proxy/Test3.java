package me.proxy;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test3 {
	public static String preUrl = null;

	public static void main(String[] args) {
//		System.out.println(getHost("fs.taobao.com?search=y&scid=500665996&scname=z8TXsM"));
		HashSet<String> hosts = getHosts("www.taobao.com", 1, true);
		System.out.println("below is scaned hosts");
		for (String host : hosts) {
			System.out.println(host);
		}
		System.out.println("size is " + hosts.size());
	}

	public static HashSet<String> getHosts(String url, int deep,
			boolean urlDistinguish) {
		HashSet<String> hosts = new HashSet<String>();
		hosts.add(getHost(url));
		HashSet<String> scaned = new HashSet<String>();
		if (urlDistinguish)
			getHosts1(url, deep, hosts, scaned);
		else
			getHosts2(url, deep, hosts, scaned);
		scaned.clear();
		return hosts;
	}

	public static void getHosts1(String url, int deep, HashSet<String> hosts,
			HashSet<String> scaned) {
		if (deep < 1 || scaned.contains(url))
			return;
		System.err.println(deep + "<<<---start iterate--->>>> " + url
				+ ", pre is >>>> " + preUrl);
		scaned.add(url);
		StringBuffer sBuf = new StringBuffer();
		HttpURLConnection conn = null;
		InputStreamReader isr = null;
		try {
			conn = (HttpURLConnection) new URL("http://" + url)
					.openConnection();
			conn.setReadTimeout(1000);
			if (conn.getResponseCode() != 200 || conn.getContentType() == null
					|| !conn.getContentType().toUpperCase().startsWith("TEXT")) {
				System.out.println(conn.getContentType() + ":"
						+ conn.getResponseCode()
						+ "----unexpected code or content type");
				return;
			}
			isr = new InputStreamReader(conn.getInputStream());
			char[] tmp = new char[1024 * 32];
			int len = -1;
			while ((len = isr.read(tmp)) != -1) {
				sBuf.append(tmp, 0, len);
			}
		} catch (Exception e) {
			System.err.println("EEEEEEEE error occured->>>>>>>"
					+ e.getMessage());
		} finally {
			try {
				if (isr != null)
					isr.close();
				conn.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Pattern p = Pattern
				.compile("(\\w{1,15}\\.){1,4}(com|cn)(\\/(\\w|\\.|\\-){0,50}){0,10}\\??(\\w{0,20}\\=\\w{0,30}&?){0,20}");
		Matcher m = p.matcher(sBuf.toString());
		while (m.find()) {
			preUrl = url;
			String urlTmp = m.group();
			hosts.add(getHost(urlTmp));
			getHosts1(urlTmp, deep - 1, hosts, scaned);
		}
	}

	public static void getHosts2(String url, int deep, HashSet<String> hosts,
			HashSet<String> scaned) {

	}

	public static String getHost(String url) {
		int index = url.indexOf("/");
		if (index > 0)
			url = url.substring(0, index);
		index = url.indexOf("?");
		if (index > 0)
			url = url.substring(0, index);
		return url;
	}
}
