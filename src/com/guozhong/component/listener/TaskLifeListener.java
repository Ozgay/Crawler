package com.guozhong.component.listener;

import com.guozhong.CrawlTask;

/**
 * ���Ҫ��������������������ھͿ���ʵ�������
 * @author Administrator
 *
 */
public interface TaskLifeListener {
	
	/**
	 * ������ʼץȡ��ʱ��ص�
	 * @param task
	 */
	public void onStart(CrawlTask task);
	
	/**
	 * ���������ʱ��ص�
	 * @param task
	 */
	public void onFinished(CrawlTask task);
	
	
	/**
	 * ����ָ���ץʱ�ص�
	 * @param task
	 */
	public void onRecover(CrawlTask task);

}
