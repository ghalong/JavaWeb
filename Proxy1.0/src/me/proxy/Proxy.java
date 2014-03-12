package me.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Proxy {
	public static int count = 0;

	public static void main(String[] args) {
		ServerSocket ss;
		System.err.println("start....");
		try {
			ss = new ServerSocket(80);
			// ss.
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		while (true) {
			final Socket cS;
			try {
				cS = ss.accept();
				// cS.set
			} catch (IOException e) {
				System.err.println(e.getMessage());
				continue;
			}
			new Thread() {
				public void run() {
					BufferedInputStream in = null;
					BufferedOutputStream out = null;
					try {
						in = new BufferedInputStream(cS.getInputStream());
						out = new BufferedOutputStream(cS.getOutputStream());
						StringBuffer data = new StringBuffer();
						int tmp = -1;
						String host = null;
						StringBuffer line = new StringBuffer();
						while ((tmp = in.read()) != -1) {
							data.append((char) tmp);
							line.append((char) tmp);
							if (tmp == '\n') {
								String lineTmp = line.toString();
								if (lineTmp.toUpperCase().startsWith("HOST")) {
									host = lineTmp.split(":")[1].trim();
									if (host.contains("115.28.132.207")) {
										cS.close();
									} else {
										System.out.println(host+":"+cS.getRemoteSocketAddress()+":"+(++count));
										communicate(in, out, host,
												data.toString(),cS);
									}

									break;
								}
								line.delete(0, line.length());
							}
						}
					} catch (Exception e) {
						System.err.println(e.getMessage());
					} finally {
						try {
							in.close();
							out.close();
							cS.close();
							count--;
						} catch (IOException e) {
							System.err.println(e.getMessage());
						}
					}
				}
			}.start();
		}
	}

	public static void communicate(final InputStream in,
			final OutputStream out, String host, final String data,
			final Socket cS) throws UnknownHostException, IOException {
		final Socket s = new Socket(host, 80);

		// 读取浏览器端数据，并将其发送给淘宝
		new Thread() {
			BufferedOutputStream bos = new BufferedOutputStream(
					s.getOutputStream());

			public void run() {
				try {
					bos.write(data.getBytes());
					byte[] buf = new byte[1024 * 8];
					int len = -1;
					while ((len = in.read(buf)) != -1) {
						bos.write(buf, 0, len);
						bos.flush();
					}
				} catch (IOException e) {
					if (!cS.isClosed())
						throw new RuntimeException(e);
				} finally {
					try {
						bos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();

		// 读取淘宝服务端数据，并将其返回给浏览器
		new Thread() {
			BufferedInputStream bis = new BufferedInputStream(
					s.getInputStream());

			public void run() {
				try {
					byte[] buf = new byte[1024 * 64];
					int len = -1;
					while ((len = bis.read(buf)) != -1) {
						out.write(buf, 0, len);
						out.flush();
					}
				} catch (IOException e) {
					if (!cS.isClosed())
						throw new RuntimeException(e);
				} finally {
					try {
						bis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	public static void log(String s) {
		System.out.println(s);
	}
}
