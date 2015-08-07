package com.mxl.comm;

public class App {
	public static void main(String []args){
		String serverPath = new String("/");
		String serverFileName = new String("数据结构.pdf");
		String localFileName = new String("I:\\Documents\\Downloads\\open_data_structures.pdf");
		CFtpClient client = new CFtpClient();
		if(client.UploadFileToServer(serverPath, serverFileName, localFileName)==1){
			System.out.println("上传成功");
		}else{
			System.out.println("上传失败");
		}
		client.close();
	}
}
