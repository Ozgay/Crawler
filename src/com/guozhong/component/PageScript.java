package com.guozhong.component;

import java.util.List;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.guozhong.model.Proccessable;

public  interface  PageScript {
	/**
	 * ������ִ�����JS���롣��ִ��JS����Ĺ���������Է���һЩ�µ�Proccessable����
	 * @param driver
	 * @return
	 * @throws Exception
	 */
	public  List<Proccessable> executeJS(HtmlUnitDriver driver)throws Exception;
}
