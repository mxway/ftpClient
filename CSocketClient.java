package com.mxl.comm;
/***
 * @author mxl
 * @data   2014-08-26
 * 功能描述：本文件的功能是对java的socket进行简单的封装，封装可以设置连接服务器的IP地址及端口。
 * 并提供了向服务器发送数据及从服务器接收数据的两个方法
 * 
 */
import java.io.*;
import java.net.*;


public class CSocketClient {
	Socket	clientSocket;
	OutputStream outputStream;
	InputStream  inputStream;
	String		 serverIp;
	int			 port;
	public CSocketClient()
	{
		clientSocket = null;
		outputStream = null;
		inputStream  = null;
		serverIp     = "127.0.0.1";
		port		 = 21;
	}
	
	public CSocketClient(String serverIp, int port)
	{
		this.serverIp = serverIp;
		this.port     = port;
	}
	
	public String getServerIp() {
		return serverIp;
	}

	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	/***
	 * 连接FTP服务器
	 * @return
	 */
	public int connect()
	{
		try {
			clientSocket = new Socket(serverIp,port);
			outputStream = clientSocket.getOutputStream();
			inputStream  = clientSocket.getInputStream();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
		return 1;
	}
	
	/***
	 * 向服务器发送数据
	 * 
	 * @param sendData 要发送到服务器的数据，以字节表示
	 * @param length 要发送到服务器数据的长度
	 * @return 返回发送数据的长度
	 */
	public int	SendData(byte sendData[], int sendLength)
	{
		if(clientSocket != null)
		{
			try {
				outputStream.write(sendData, 0, sendLength);
				outputStream.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return 0;
			}
		}
		return sendLength;
	}
	
	/***
	 * 接收服务发送到客户端的数据
	 * @param data 将接收到的数据以二进制数据存储到data中
	 * @return 接收数据的长度
	 */
	public String	RecvData()
	{
		byte data[] = new byte[1024];
		int recvLength = 0;
		try {
			recvLength = inputStream.read(data, 0, 1024);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
		//去掉服务器数据的最后的\r\n
		String str = new String(data,0, recvLength-2);
		return str;
	}
	
	/***
	 * 关闭数据连接
	 */
	public void close()
	{
		
		try {
			if(inputStream != null){
				inputStream.close();
				inputStream = null;
			}
			if(outputStream != null){
				outputStream.close();
				outputStream = null;
			}
			if(clientSocket != null){
				clientSocket.close();
				clientSocket = null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			//如果上面的inputStream或是outStream关闭失败，那么就要保证clientSocket一定要被关闭
			if(clientSocket != null){
				try {
					clientSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
