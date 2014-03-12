package me.web.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class TaoBaoProxy
 */
public class TaoBaoProxy extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public TaoBaoProxy() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		final ServletInputStream sis = request.getInputStream();
		final ServletOutputStream sos = response.getOutputStream();
		final byte[] sbuf = new byte[1024];

		Socket s = new Socket(request.getRequestURL().toString().substring(7,request.getRequestURL().toString().length()-1), 80);
		final OutputStream os = s.getOutputStream();
		final InputStream is = s.getInputStream();
		final byte[] buf = new byte[1024];

		new Thread() {
			public void run() {
				int sLen = -1;
				try {
					while ((sLen = sis.read(sbuf)) != -1) {
						os.write(sbuf, 0, sLen);
						os.flush();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}.start();
		new Thread() {
			int len = -1;
			public void run() {
				try {
					while((len = is.read(buf))!=-1){
						sos.write(buf, 0, len);
						sos.flush();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

}
