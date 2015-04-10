package com.guozhong.backup;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.guozhong.Request;
import com.guozhong.StartContext;

public interface RequestQueueBackup {
	
	/**
	 * 默认备份周期10分钟
	 */
	public static final long DEFAULT_BACKUP_PERIOD = 1000 * 60 * 10;

	/**
	 * 提交请求队列
	 * @param req
	 */
	public void commitRequestQueue(BlockingQueue<Request> requestQueue);
	
	/**
	 * 提交开始请求队列
	 * @param req
	 */
	public void commitStartRequestQueue(BlockingQueue<StartContext> startRequests);
	
	/**
	 * 所有种子URL备份
	 * @param req
	 */
	public void commitStartBackups(List<StartContext> allStartBackups);
	
	
	
	/**
	 * 开始备份
	 */
	public void startBackup();//01089678706
	
	/**
	 * 设置备份时间间隔
	 */
	public void setBackupPeriod(long millisecond);
	
	/**
	 * 停止备份
	 */
	public void stopBackup();
	
	/**
	 * 取得最后一次的备份队列
	 * @return
	 * @throws Exception 
	 */
	public BlockingQueue<Request> getLastRequestQueue() throws Exception;
	
	/**
	 * 开始请求队列
	 * @return
	 */
	public BlockingQueue<StartContext> getLastStartRequestQueue() throws Exception;
	
	
	/**
	 * 取得所有种子URL备份
	 * @return
	 */
	public List<StartContext> getLastStartBackups() throws Exception;
	
	/**
	 *  清楚所有的备份
	 */
	public void clearBackup();
	
	/**
	 * 判断上一次备份器是否正常退出
	 * @return
	 */
	public boolean lastBackupStoped();
	
}
