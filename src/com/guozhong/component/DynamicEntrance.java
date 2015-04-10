package com.guozhong.component;

import java.util.List;
import java.util.Map;

import com.guozhong.StartContext;

/**
 * ��̬�������URL��
 * �������������URLÿ�ζ����ǹ̶��Ļ�����ô��������һ��DynamicEntrance
 * @author Administrator
 *
 */
public interface DynamicEntrance {
	
	/**
	 * �Ƿ����֮ǰ�����URL
	 * @return
	 */
	public boolean isClearLast();
	
	/**
	 * ��ʼ����֮ǰ�ص�onStartLoad
	 */
	public void onStartLoad();
	
	/**
	 * �������URL
	 * @return
	 */
	public List<String> load();
	
	/**
	 * �������URL  �����Ӳ����ķ���
	 * @return
	 */
	public Map<String,Map<String,Object>> load2();
	
	public List<StartContext> getStartContext();
	
	/**
	 * �������֮��ص�
	 */
	public void onLoadComplete();
	
	
	/**
	 * ���URLҳ��ı����ʽ������Ĭ��gbk
	 * @return
	 */
	public String getEntranceCharSet();
}
