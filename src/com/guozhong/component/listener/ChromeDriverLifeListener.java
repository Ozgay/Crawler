package com.guozhong.component.listener;

import org.openqa.selenium.chrome.ChromeDriver;

/**
 * �����ȸ�������Ĵ���������
 * @author Administrator
 *
 */
public interface ChromeDriverLifeListener {

	/**
	 * ���������ȸ������
	 * ÿ������һ����ص����·���
	 * ����:������������ȵ�¼
	 * @param chromeDriver
	 */
	public void onCreated(int index,com.guozhong.downloader.impl.ChromeDriver chromeDriver);
	
	
	/**
	 * �����ȸ����������
	 * ������֮ǰ����Ĳ��������˳���¼
	 * @param chromeDriver
	 */
	public void onQuit(int index,com.guozhong.downloader.impl.ChromeDriver chromeDriver);
}
