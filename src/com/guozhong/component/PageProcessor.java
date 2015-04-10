package com.guozhong.component;

import java.util.List;
import java.util.regex.Pattern;


import com.guozhong.StartContext;
import com.guozhong.model.Proccessable;
import com.guozhong.page.OkPage;
import com.guozhong.page.Page;

/**
 * ��ҳ�����ӿ�
 * @author Administrator
 *
 */
public interface PageProcessor {
	/**
	 * ������PageProcessor��Ӧ�ô�������Request�����ҳ��   ��ʼURL���Է���null
	 * @return
	 */
	public String getTag();
	
	/**
	 * �����Ҫҳ�涯̬����JS������һ��PageScript����
	 * @return
	 */
	public PageScript getJavaScript();
	
	/**
	 * ����������Ip����ʱ��Ҫ��д�˷���������������ҳӦ�ô��е��ַ�����ʶ������www.baidu.com���С��ٶȡ�
	 * @return
	 */
	public Pattern getNormalContain();
	
	/**
	 * ����һ��ҳ��
	 * @param page
	 * @param context
	 * @return
	 */
	public void process(OkPage page,StartContext context,List<Proccessable> result)throws Exception; 
	
	/**
	 * �������ҳ��
	 * @param page
	 * @param context
	 */
	public void proccessErrorPage(Page page,StartContext context)throws Exception;
}
