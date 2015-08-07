package com.mxl.comm;

/***
 * @author mxl
 * @data   2014-08-26
 * 功能描述：本文件实现了RFC959的部分功能，提供了一个方法将本机的文件上传到服务的功能
 * 
 */
import java.util.StringTokenizer;
import java.io.*;

public class CFtpClient {
	CSocketClient	controlSocket;
	public CFtpClient()
	{
		controlSocket = new CSocketClient();
		String serverIp    = ReadConfigValueByKey("FTP_SERVER");
		String serverPort  = ReadConfigValueByKey("FTP_SERVERPORT");
		if(serverIp.length()>0 && serverPort.length()>0)
		{
			controlSocket.setServerIp(serverIp);
			controlSocket.setPort(Integer.valueOf(serverPort));
		}
	}
	
	/***
	 * 根据 RFC959 协议将本机文件上传到ftp服务器上
	 * @return 创建sokcet失败返回0，无法连接到服务器返回0，配置的用户名和密码验证错误返回-1，正确执行完成返回1
	 */
	public int UploadFileToServer(String serverPath, String serverFileName, String localFileName)
	{
		String dataSocketInfo = "";
		String serverIp = "";
		int	   port = 0;
		if(controlSocket == null)
		{
			return 0;
		}
		if(controlSocket.connect() != 1)
		{//创建连接失败
			return 0;
		}
		controlSocket.RecvData();
		//用于向FTP服务器发送用户名和密码进验证
		if(login()==0)
		{
			System.out.println("用户名密码验证错误!!!");
			return -1;
		}
		
		//获取用于文件数据传送的IP地址及端口号
		dataSocketInfo = getDataServerAndPort();
		serverIp = interpretServerIp(dataSocketInfo);
		port	 = interpretServerPort(dataSocketInfo);
		//创建专门用于传输数据的socket
		CSocketClient dataSocket = new CSocketClient(serverIp,port);
		
		/*** 初始化Ftp服务准备接收文件之前的环境设置，如设置新上传文件在服务器存储的目录、服务接收文件的格式等 **/
		InitFtpMode(serverPath, serverFileName);
		
		if(dataSocket.connect()==0)
		{
			System.out.println("无法连接到数据端口");
		}
		SendFileData(dataSocket, localFileName);
		exitFtpServer();
		return 1;
	}
	
	/***
	 * ftp服务器用户验证
	 */
	public int login()
	{
		String resultCode = "";
		String configUserName = ReadConfigValueByKey("FTP_USERNAME");
		String configPassword = ReadConfigValueByKey("FTP_PASSWORD");
		StringBuffer userName = new StringBuffer("USER ");
		userName.append(configUserName);
		userName.append("\r\n");
		
		StringBuffer password = new StringBuffer("PASS ");
		password.append(configPassword);
		password.append("\r\n");
		executeFtpCommand(userName.toString());
		String resultStr = executeFtpCommand(password.toString());
		resultCode = interpretCode(resultStr);
		if(!resultCode.equals("230"))
		{
			return 0;
		}
		return 1;
	}
	
	/***
	 * 发送PASV命令
	 * 发送PASV命令后得到的服务器的应答形式为:227 Entering Passive Mode (127,0,0,1,175,200)
	 * 其中空格前面的三位数字表示服务器对pasv响应的状态，"("及 ")"中间的表示用于向服务器传送数据的IP地址及端口号其形式如
	 * (IP1,IP2,IP3,IP4,PORT1,PORT2)其中IP1,IP2,IP3,IP4与点分十进制IP地址相对应 PORT1,PORT2都是八位二进制数据。
	 * 其中PORT1对应16位端口号的高8位，PORT2对应16位端口号的低8位。
	 * 端口号的计算方法:PORT1<<8+PORT2
	 *  
	 * @return 返回的是"("及")"中间的值，不过返回值中把所有的","都转换成"."
	 */
	public String getDataServerAndPort()
	{
		int leftPos = 0;
		int rightPos = 0;
		String recvStr = "";
		String sendStr = new String("PASV\r\n");
		recvStr = executeFtpCommand(sendStr);
		leftPos  = recvStr.indexOf('(');
		rightPos = recvStr.indexOf(')');
		//取得"("与")"之间的字符串
		String temp = recvStr.substring(leftPos+1, rightPos);
		//把字符串中所有的","换成"."
		recvStr = temp.replace(',','.');
		return recvStr;
	}
	
	/***
	 * 从recvBuf中解析出服务器返回 的代码
	 * 服务器返回字符串格式为 三位数字+一位空格+详细描述信息
	 * 具体见RFC959
	 * @param recvBuf 从服务器接收到的字符串信息
	 */
	public String interpretCode(String recvBuf)
	{
		String resultStr = "";
		StringTokenizer token = new StringTokenizer(recvBuf, " ");
		if(token.hasMoreTokens())
		{
			resultStr = token.nextToken();//空格前的三位数字码
		}
		return resultStr;
	}
	
	/***
	 * 从recvBuf中解析出服务器返回详细描述信息
	 * 服务器返回字符串格式为 三位数字+一位空格+详细描述信息
	 * 具体见RFC959
	 * @param recvBuf 从服务器接收到的字符串信息
	 */
	public String interpretInfo(String recvBuf)
	{
		String resultStr = "";
		StringTokenizer token = new StringTokenizer(recvBuf, " ");
		if(token.hasMoreTokens())
		{
			token.nextToken();//空格前的三位数字码
		}
		if(token.hasMoreTokens())
		{
			resultStr = token.nextToken();
		}
		return resultStr;
	}
	
	/***
	 * 从形如:h1.h2.h3.h4.p1.p2中找到字符串h1.h2.h3.h4
	 * @param str 
	 * @return 返回h1.h2.h3.h4
	 */
	public String interpretServerIp(String str)
	{
		int pos = str.lastIndexOf('.');
		pos = str.lastIndexOf('.', pos-1);
		String serverIp = str.substring(0, pos);
		return serverIp;
	}
	
	/***
	 * 从形如:h1.h2.h3.h4.p1.p2的字符串中计算 p1*256+p2的值
	 * 详见RFC959
	 * @param str
	 * @return p1*256+p2
	 */
	public int interpretServerPort(String str)
	{
		int port = 0;
		int leftPos = 0;
		int	rightPos = 0;
		
		rightPos = str.lastIndexOf('.');
		leftPos  = str.lastIndexOf('.', rightPos-1);
		//获取最后一个"."之后数字
		port = Integer.valueOf(str.substring(rightPos+1));
		//System.out.println(str.substring());
		//获取倒数第二个"."与最后一个"."之间的数字
		port = Integer.valueOf(str.substring(leftPos+1, rightPos))*256+port;
		
		return port;
	}
	
	/***
	 * 
	 * @param clientSocket
	 * @return
	 */
	public int SendFileData(CSocketClient clientSocket, String localFileName)
	{
		byte fileData[] = new byte[1024];
		try {
			FileInputStream finStream = new FileInputStream
					(new File(localFileName));
			int totalSize = finStream.available();
			/*** 每次发送1024个字节 **/
			while(totalSize/1024 > 0)
			{
				finStream.read(fileData, 0, 1024);
				clientSocket.SendData(fileData, 1024);
				totalSize = totalSize-1024;
			}
			//未发送的数据不足1024字节
			if(totalSize > 0)
			{
				finStream.read(fileData, 0, totalSize);
				clientSocket.SendData(fileData, totalSize);
			}
			finStream.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		} finally{
			clientSocket.close();
		}
		return 1;
	}
	
	/***
	 * 关闭socket连接
	 */
	public void close()
	{
		controlSocket.close();
	}
	
	/***
	 * 从客户端向服务器发送ftp命令，命令字符不区分大小写
	 * 详细见RFC959
	 * @param command要发送到服务器的命令字符串
	 * @return 发送成功则返回从服务器返回的字符串，否则返回""
	 */
	public String executeFtpCommand(String command)
	{
		String resultStr = "";
		byte   sendData[];
		//发送到服务器的命令不是以\r\n结束
		if(!command.endsWith("\r\n"))//所有从客户端发送到服务器的一条完整命令都是以\r\n结束，详见RFC959
		{
			command = command + "\r\n";
		}
		try {
			sendData = command.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
		if(controlSocket.SendData(sendData, sendData.length) == 0)
		{
			return "";
		}
		//根据RFC959 每向FTP服务器发送一个命令，FTP服务器就需要有一次响应，如果不调用RecvData
		//后面如果有需要服务器响应信息时就会取到错误信息
		resultStr = controlSocket.RecvData();
		return resultStr;
	}
	
	/***
	 * 向服务器发送 quit命令，退出服务器与客户的连接
	 */
	public void exitFtpServer()
	{
		String command = new String("QUIT\r\n");
		executeFtpCommand(command);
	}
	
	/**
	 * 在服务器准备接收文件之前对环境进行设置
	 * @设置服务器的工作目录为serverPath
	 * @设置上传到服务器的文件名为：serverFileName
	 * @设置服务器接收文件的格式为二进制流
	 * 
	 * @param serverPath
	 * @param serverFileName
	 */
	public void InitFtpMode(String serverPath, String serverFileName)
	{
		//String         utf8Command = new String("OPTS UTF8 ON");
		//改变服务工作目录命令
		StringBuffer   changeDirectoryPath = new StringBuffer("CWD ");
		changeDirectoryPath.append(serverPath);
		changeDirectoryPath.append("\r\n");
		//设置文件传输的格式 I表示以二进制流的方式进行传输
		String fileTypeCommand 		  = new String("TYPE I\r\n");
		//上传到服务器后文件名命令
		StringBuffer   storFileCommand = new StringBuffer("STOR ");
		storFileCommand.append(serverFileName);
		storFileCommand.append("\r\n");
		
		//executeFtpCommand(utf8Command);
		executeFtpCommand(changeDirectoryPath.toString());
		executeFtpCommand(fileTypeCommand);
		String result = executeFtpCommand(storFileCommand.toString());
		System.out.println(result);
	}
	
	/***
	 * config.dat中的每一行形式如:KEY=VALUE
	 * 找到配置文件中KEY字符串与key相等的行，如果存在相等的，则把VALUE值返回。
	 * @param key
	 */
	public String ReadConfigValueByKey(String key)
	{
		BufferedReader bufReader = null;
		String str = "";
		String configKey = "";
		String configValue = "";
		String resultStr = "";
		try {
			bufReader = new BufferedReader(new FileReader
					(new File("config.dat")));
			str = bufReader.readLine();
			while(str!=null && str.length()>0)
			{
				StringTokenizer token = new StringTokenizer(str, "=");
				if(token.hasMoreTokens())
				{
					configKey = token.nextToken();
				}
				if(token.hasMoreTokens())
				{
					configValue = token.nextToken();
				}
				configKey = configKey.trim();
				configValue = configValue.trim();
				if(configKey.equalsIgnoreCase(key))
				{
					resultStr = configValue;
					break;
				}
				str = bufReader.readLine();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(bufReader!=null)
		{
			try {
				bufReader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return resultStr;
	}
}
