package com.guozhong.util;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

public final class JavaScriptUtil {
	
	public static final Object click(HtmlUnitDriver driver,WebElement ele){
		Object o = driver.executeScript("arguments[0].click();", ele);
		try {
			Thread.sleep(1000);//ִ��js����Ĭ�ϵ�1S ��֤htmlԴ�����±�����
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return o;
	}

}
