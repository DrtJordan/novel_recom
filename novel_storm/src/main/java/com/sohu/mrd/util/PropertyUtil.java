/**
 * Project Name:NewsPreprocessorFrontend
 * File Name:PropertyUtil.java
 * Package Name:com.sohu.mrd.preprocess.util
 * Date:2014年11月27日下午4:33:41
 * Copyright (c) 2014, alexma@sohu-inc.com All Rights Reserved.
 *
*/
/**
 * Project Name:NewsPreprocessorFrontend
 * File Name:PropertyUtil.java
 * Package Name:com.sohu.mrd.preprocess.util
 * Date:2014年11月27日下午4:33:41
 * Copyright (c) 2014, alexma@sohu-inc.com All Rights Reserved.
 *
 */

package com.sohu.mrd.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/**
 * ClassName:PropertyUtil
 * Date:     2014年11月27日 下午4:33:41
 * @author   alexma
 * @version  	 
 */

/**
 * ClassName: PropertyUtil
 * Date: 2014年11月27日 下午4:33:41
 *
 * @author alexma
 * @version 
 */
public class PropertyUtil {
    
    public static Properties getPropertiesFromClassResFile(String classResFile) {
        if (classResFile == null) {
            return null;
        }
        InputStream input = PropertyUtil.class.getClassLoader().getResourceAsStream(classResFile);
        return loadProperties(input);
    }

    private static Properties loadProperties(InputStream input) {
        InputStreamReader reader;
        Properties props = new Properties();
        try {
            reader = new InputStreamReader(input, "utf8");
            props.load(reader);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return props;
    }

    /**
     * 
     */
    public PropertyUtil() {
        // TODO Auto-generated constructor stub
    }

    /**
     *
     * @author alexma
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
    	Properties pro = 	getPropertiesFromClassResFile("politicsConfig.properties");
    	Set s = pro.keySet();
    	Iterator<String> it = s.iterator();  
    	while (it.hasNext()){  
    	  String str = it.next();  
    	  System.out.println(str);  
    	}  
//    	System.out.println(pro.keySet());
    }

}
