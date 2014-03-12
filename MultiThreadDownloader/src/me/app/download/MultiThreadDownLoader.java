package me.app.download;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class MultiThreadDownLoader {
	private URL url; // �������ص�ַ��ͳһ��Դ��λ��
	private int threadNum; // ����ʹ�õ��߳���
	private long partLen; // ÿ���߳�Ҫ���ص����ݵĴ�С
	private File downloadFile; // ���ص����ص��ļ�
	private boolean isPause; // �Ƿ���ͣ����
	private File tempFile; // ��¼ÿ���߳��Ѿ����ص����ݳ��ȵ��ļ�
	private long totalDownload; // �����߳��Ѿ����ص����ݵ��ܳ���
	private long totalLen; // Ҫ���ص����ݵ��ܳ���
	private long beginTime;
	private long usedTime;
	private long recordUsedTime;

	/**
	 * 
	 * @param address
	 *            ���ص�ַ
	 * @param threadNum
	 *            ����ʹ�õ��߳���
	 * @param downloadDir
	 *            �����ļ��ı��ش��·��
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
			System.out.println("����������δ��������Ӧ�� " + code
					+ " ������ 200 ���������Դ���ܲ����ڻ���ʱ������");
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
			tempFile.delete();// �������䲻�ܴ�����ʱ�ļ�����
		}
		if (!tempFile.exists()) {
			RandomAccessFile tempRaf = new RandomAccessFile(tempFile, "rws");
			for (int i = 0; i < threadNum + 2; i++) {// ���ļ���д���߳������հ׵��߳��������ݡ�����ʱ�䣬��ʼ8���ֽڼ�¼�߳��������8���ֽڼ�¼ʱ�䣬�м����߳�����
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
		private long start; // ���߳�Ҫ��������������ص��ļ�����ʼλ�ã����ֽڶ���
		private long end; // ���߳�Ҫ��������������ص��ļ��Ľ���λ�ã����������ļ����ܳ��ȣ�������ֻ�᷵��Ӧ�е�����
		private long threadDownload; // ���߳������ص���������
		private boolean isRun;

		/**
		 * 
		 * @param file
		 *            �������ص����ص��ļ�
		 * @param tempFile
		 *            ����߳�������Ϣ����ʱ�ļ�
		 * @param id
		 *            �߳�id
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
				start = partLen * (id - 1) + threadDownload;// start��¼��λ���ǵ�ǰ������������ݵĺ�һ��������������ȫ����������ˣ�
				// start��ָ�����һ�����ݵĺ��棬��һ�������ڵ����ݻ�end+1�����ݣ��������͸��������ͻ���յ�406
				end = partLen * id - 1;
			} catch (EOFException e) {
				isRun = false;
				System.out.println("EOFException�����쳣");
			}
		}

		@Override
		public void run() {
			if (!isRun)
				return;
			if (start >= totalLen)// �����̻߳���ִ��������start=totalLen+1
				return;
			if (start >= end)// ǰ����̻߳���ִ��������start=end+1
				return;
			try {
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setConnectTimeout(6000);
				conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
				System.out.println(conn.getResponseCode() + "��" + id
						+ "���̣߳�bytes=" + start + "-" + end);
				if (conn.getResponseCode() != 200&&conn.getResponseCode() != 206) {
					System.out.println("��Ӧ�벻������ֵ 206��200,����"
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
					tempRaf.writeLong(threadDownload);// ��������Ҫ�����ļ�д���������֮��ִ��
					System.out.println(id+"���߳����������ݣ�"+threadDownload+"bit����Ҫ����"+partLen+"bit�������߳��ܹ����أ�"+totalDownload/1024/1024/8+"KB���ļ��ܳ��ȣ�"+totalLen/1024/1024/8+"KB��ƽ�������ٶ�"+(totalDownload*1000/(usedTime*1024))+"KB/s������ʱ��"+(usedTime/1000)+"��");
				}
				bis.close();
				conn.disconnect();
				fileRaf.close();
				if (!isPause) {
					System.out.println("�� " + id + " ���߳�������ϣ���");
					if (totalDownload == totalLen) {
						tempFile.delete();
						System.out.println(downloadFile.getName()
								+ " ������ϣ����ܼ���ʱ " + (usedTime / 1000) + " ��");
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
