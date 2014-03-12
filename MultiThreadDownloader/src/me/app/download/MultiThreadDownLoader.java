package me.app.download;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class MultiThreadDownLoader {
	private URL url; // 代表下载地址的统一资源定位符
	private int threadNum; // 下载使用的线程数
	private long partLen; // 每个线程要下载的数据的大小
	private File downloadFile; // 下载到本地的文件
	private boolean isPause; // 是否暂停下载
	private File tempFile; // 记录每个线程已经下载的数据长度的文件
	private long totalDownload; // 所有线程已经下载的数据的总长度
	private long totalLen; // 要下载的数据的总长度
	private long beginTime;
	private long usedTime;
	private long recordUsedTime;

	/**
	 * 
	 * @param address
	 *            下载地址
	 * @param threadNum
	 *            下载使用的线程数
	 * @param downloadDir
	 *            下载文件的本地存放路径
	 * @throws Exception
	 */
	public MultiThreadDownLoader(String address, int threadNum,
			String downloadDir) throws Exception {
		this.url = new URL(address);
		this.threadNum = threadNum;
		this.downloadFile = new File(downloadDir);
	}

	public void setPause() {
		isPause = true;
	}

	public void download() throws Exception {
		isPause = false;
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(3000);
		int code = conn.getResponseCode();
		if (code != 200) {
			System.out.println("服务器返回未期望的响应码 " + code
					+ " ，期望 200 ，请求的资源可能不存在或暂时不可用");
			return;
		}
		totalLen = conn.getContentLength();
		conn.disconnect();

		if (!downloadFile.getParentFile().exists()) {
			downloadFile.getParentFile().mkdirs();
		}
		tempFile = new File(downloadFile.getAbsolutePath() + ".temp");
		if (!downloadFile.exists() || downloadFile.length() != totalLen) {
			RandomAccessFile fileRaf = new RandomAccessFile(downloadFile, "rws");
			fileRaf.setLength(totalLen);
			fileRaf.close();
			tempFile.delete();// 下面的语句不能创建临时文件！！
		}
		if (!tempFile.exists()) {
			RandomAccessFile tempRaf = new RandomAccessFile(tempFile, "rws");
			for (int i = 0; i < threadNum + 2; i++) {// 向文件中写入线程数、空白的线程下载数据、已用时间，开始8个字节记录线程数，最后8个字节记录时间，中间是线程数据
				if (i != 0)
					tempRaf.writeLong(0);
				else
					tempRaf.writeLong(threadNum);
			}
			tempRaf.close();
		} else {
			RandomAccessFile tempRaf = new RandomAccessFile(tempFile, "rws");
			threadNum = (int) tempRaf.readLong();
			tempRaf.seek(8 * (1 + threadNum));
			usedTime = tempRaf.readLong();
			recordUsedTime = usedTime;
			tempRaf.close();
		}
		partLen = (totalLen + threadNum - 1) / threadNum;
		beginTime = System.currentTimeMillis();
		for (int i = 1; i <= threadNum; i++) {
			new DownloadThread(downloadFile, tempFile, i).start();
		}
	}

	class DownloadThread extends Thread {
		private int id;
		private RandomAccessFile fileRaf;
		private RandomAccessFile tempRaf;
		private long start; // 该线程要向服务器请求下载的文件的起始位置，以字节度量
		private long end; // 该线程要想服务器请求下载的文件的结束位置，若超出了文件的总长度，服务器只会返回应有的数据
		private long threadDownload; // 该线程已下载的数据总量
		private boolean isRun;

		/**
		 * 
		 * @param file
		 *            代表下载到本地的文件
		 * @param tempFile
		 *            存放线程下载信息的临时文件
		 * @param id
		 *            线程id
		 * @throws Exception
		 */
		public DownloadThread(File file, File tempFile, int id)
				throws Exception {
			this.id = id;
			isRun = true;
			fileRaf = new RandomAccessFile(file, "rws");
			tempRaf = new RandomAccessFile(tempFile, "rws");
			tempRaf.seek(id * 8);
			try {
				threadDownload = tempRaf.readLong();
				synchronized (MultiThreadDownLoader.this) {
					totalDownload += threadDownload;
				}
				start = partLen * (id - 1) + threadDownload;// start记录的位置是当前已下载完毕数据的后一个，所以若数据全部下载完毕了，
				// start会指向最后一个数据的后面，即一个不存在的数据或end+1的数据，这样发送给服务器就会接收到406
				end = partLen * id - 1;
			} catch (EOFException e) {
				isRun = false;
				System.out.println("EOFException结束异常");
			}
		}

		@Override
		public void run() {
			if (!isRun)
				return;
			if (start >= totalLen)// 最后的线程会出现此情况，即start=totalLen+1
				return;
			if (start >= end)// 前面的线程会出现此情况，即start=end+1
				return;
			try {
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setConnectTimeout(6000);
				conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
				System.out.println(conn.getResponseCode() + "第" + id
						+ "号线程：bytes=" + start + "-" + end);
				if (conn.getResponseCode() != 200&&conn.getResponseCode() != 206) {
					System.out.println("响应码不是期望值 206或200,而是"
							+ conn.getResponseCode());
					conn.disconnect();
					tempRaf.close();
					fileRaf.close();
					return;
				}

				BufferedInputStream bis = new BufferedInputStream(
						conn.getInputStream());
				fileRaf.seek(start);
				byte[] buff = new byte[1024 * 100];
				int len = -1;
				while (!isPause && (len = bis.read(buff)) != -1) {
					fileRaf.write(buff, 0, len);
					threadDownload += len;
					// System.out.println(id + ":" + threadDownload + ":"
					// + partLen);
					tempRaf.seek(8 * (1 + threadNum));
					synchronized (MultiThreadDownLoader.this) {
						totalDownload += len;
						usedTime = recordUsedTime + System.currentTimeMillis()
								- beginTime;
						tempRaf.writeLong(usedTime);
					}

					tempRaf.seek(id * 8);
					tempRaf.writeLong(threadDownload);// 此语句必须要在向文件写入数据语句之后执行
					System.out.println(id+"号线程已下载数据："+threadDownload+"bit，需要下载"+partLen+"bit，所有线程总共下载："+totalDownload/1024/1024/8+"KB，文件总长度："+totalLen/1024/1024/8+"KB，平均下载速度"+(totalDownload*1000/(usedTime*1024))+"KB/s，已用时间"+(usedTime/1000)+"秒");
				}
				bis.close();
				conn.disconnect();
				fileRaf.close();
				if (!isPause) {
					System.out.println("第 " + id + " 号线程下载完毕！！");
					if (totalDownload == totalLen) {
						tempFile.delete();
						System.out.println(downloadFile.getName()
								+ " 下载完毕！！总计用时 " + (usedTime / 1000) + " 秒");
//						Runtime.getRuntime().exec("shutdown -s -t 100");
					}
				}
				tempRaf.close();
			} catch (Exception e) {
				e.printStackTrace();
				try {
					new DownloadThread(downloadFile, tempFile, id).start();
				} catch (Exception e1) {
					e1.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		}
	}
}
