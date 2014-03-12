package me.app.download;

public class Main {
	public static void main(String[] args) throws Exception {
		MultiThreadDownLoader downLoader = new MultiThreadDownLoader("http://dl.google.com/tag/s/appguid%3D%7B65E60E95-0DE9-43FF-9F3F-4F7D2DFF04B5%7D%26iid%3D%7B1563317F-5FCB-A106-C257-0FA6FE3619B7%7D%26lang%3Dundefined%26browser%3D3%26usagestats%3D1%26appname%3DGoogle%2520Earth%2520Pro%26needsadmin%3DTrue%26brand%3DGGGE/earth/client/GoogleEarthProSetup.exe", 5, "D:/google.exe");
		downLoader.download();
	}
}
