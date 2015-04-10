package com.guozhong.component.listener;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import com.guozhong.downloader.impl.SimpleHttpClient;
import com.guozhong.exception.DriverCreateException;

public interface HttpClientLifeListener {

	/**
	 * ��������httpClientʵ��
	 * ÿ������һ����ص����·���
	 * ����:������������ȵ�¼
	 */
	public void onCreated(int index,SimpleHttpClient httpClient);
	
	
	/**
	 * �����ȸ�httpClientʵ��
	 * ������֮ǰ����Ĳ��������˳���¼
	 */
	public void onQuit(int index,SimpleHttpClient httpClient);
}
