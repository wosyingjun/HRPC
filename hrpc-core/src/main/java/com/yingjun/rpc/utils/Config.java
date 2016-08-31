package com.yingjun.rpc.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * 读取配置文件
 *
 * @author yingjun
 */
public class Config {

    public static final int ZK_SESSION_TIMEOUT = 5000;
    public static final String ZK_ROOT_PATH = "/HRPC";

    private static Properties properties;

    static {
       try {
           InputStream inputStream = Config.class.getClassLoader().getResourceAsStream("system.properties");
           BufferedReader bf = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
           properties = new Properties();
           properties.load(bf);
       } catch (IOException e1) {
           e1.printStackTrace();
       }
   }

    public static int getIdntProperty(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    public static String getSdtringProperty(String key) {
        return properties.getProperty(key);
    }
}
