package com.guozhong.downloader;

import com.guozhong.Request;

public  interface FileDownloader {
	
	/**
	 * �����ļ�
	 * @param req
	 * @param saveFile
	 */
	public void downloadFile(Request req );
	
	/**
	 * �����ӳ�ʱ��
	 * @param time
	 */
	public void setDelayTime(int time);
	
	/**
	 * �����������
	 */
	public void saveRequestTask();

}
