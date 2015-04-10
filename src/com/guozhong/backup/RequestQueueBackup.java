package com.guozhong.backup;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.guozhong.Request;
import com.guozhong.StartContext;

public interface RequestQueueBackup {
	
	/**
	 * Ĭ�ϱ�������10����
	 */
	public static final long DEFAULT_BACKUP_PERIOD = 1000 * 60 * 10;

	/**
	 * �ύ�������
	 * @param req
	 */
	public void commitRequestQueue(BlockingQueue<Request> requestQueue);
	
	/**
	 * �ύ��ʼ�������
	 * @param req
	 */
	public void commitStartRequestQueue(BlockingQueue<StartContext> startRequests);
	
	/**
	 * ��������URL����
	 * @param req
	 */
	public void commitStartBackups(List<StartContext> allStartBackups);
	
	
	
	/**
	 * ��ʼ����
	 */
	public void startBackup();//01089678706
	
	/**
	 * ���ñ���ʱ����
	 */
	public void setBackupPeriod(long millisecond);
	
	/**
	 * ֹͣ����
	 */
	public void stopBackup();
	
	/**
	 * ȡ�����һ�εı��ݶ���
	 * @return
	 * @throws Exception 
	 */
	public BlockingQueue<Request> getLastRequestQueue() throws Exception;
	
	/**
	 * ��ʼ�������
	 * @return
	 */
	public BlockingQueue<StartContext> getLastStartRequestQueue() throws Exception;
	
	
	/**
	 * ȡ����������URL����
	 * @return
	 */
	public List<StartContext> getLastStartBackups() throws Exception;
	
	/**
	 *  ������еı���
	 */
	public void clearBackup();
	
	/**
	 * �ж���һ�α������Ƿ������˳�
	 * @return
	 */
	public boolean lastBackupStoped();
	
}
