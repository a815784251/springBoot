package com.web.common;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * <p>https body带参请求</p>
 *
 * @author JingHe
 * @version 1.0
 * @since 2019/5/20
 */
public class HttpsUtil {

    public static String postData(String action, String json) {
        URL url;
        String result = "";
        try {

            url = new URL(action);

            HttpURLConnection http = (HttpURLConnection) url.openConnection();

            http.setRequestMethod("POST");

            http.setRequestProperty("Content-Type",

                    "application/json;charset=utf-8");

            http.setDoOutput(true);

            http.setDoInput(true);

            System.setProperty("sun.net.client.defaultConnectTimeout", "30000");// 连接超时30秒

            System.setProperty("sun.net.client.defaultReadTimeout", "30000"); // 读取超时30秒

            http.connect();

            OutputStream os = http.getOutputStream();

            os.write(json.getBytes("UTF-8"));// 传入参数

            InputStream is = http.getInputStream();

            int size = is.available();

            byte[] jsonBytes = new byte[size];

            is.read(jsonBytes);

            result = new String(jsonBytes, "UTF-8");

//            System.out.println("请求返回结果:"+result);

            os.flush();

            os.close();

        } catch (Exception e) {

            e.printStackTrace();

        }
        return result;
    }

}
