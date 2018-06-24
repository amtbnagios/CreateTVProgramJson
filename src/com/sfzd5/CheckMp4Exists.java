package com.sfzd5;

import com.sfzd5.tv.Program;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckMp4Exists {

    Logger log = Logger.getLogger(CheckMp4Exists.class.toString());
    String server = "";

    /**
     * 功能：检测当前URL是否可连接或是否有效,
     * 描述：视为该地址不可用
     * @param urlStr 指定URL网络地址
     * @return URL
     */
    public boolean isConnect(String urlStr) {
        if (urlStr == null || urlStr.length() <= 0) {
            return false;
        }
        try {
            URL url = new URL(urlStr);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            int state = con.getResponseCode();
            con.disconnect();
            return state == HttpURLConnection.HTTP_OK;
        } catch (Exception ex) {
            log.warning("URL不可用");
        }
        return false;
    }

    private boolean checkMediaExit(Program program, String url) {
        //http://js1.amtb.cn/media/mp4/02/02-041/02-041-0001.mp4
        String urlFormat = "http://%s/media/mp4/%s/%s/%s";
        //检测文件
        //http://amtbsg.cloudapp.net/redirect/media/mp4/02/02-041/02-041-0001.mp4
        //http://amtbsg.cloudapp.net/redirect/media/mp3/02/02-041/02-041-0001.mp3
        String s = program.identifier.substring(0, program.identifier.indexOf("-"));
        String surl = String.format(urlFormat, getServer(), s, program.identifier, url); // "http://amtbsg.cloudapp.net/redirect/media/%s/%s/%s/%s";
        return isConnect(surl);
    }

    //获取最快的服务器域名
    public String getServer() {
        if (server.isEmpty()) {
            String json = getHtmlcodeWithoutHeader("http://amtbsg.cloudapp.net/loadbalancer/amtbservers.php?servertype=httpserver&mediatype=media&media=mp4&singleserver=1", "utf-8");
            if (!json.isEmpty()) {
                Pattern p = Pattern.compile("\"domain\":\"([^\"]*)\"");
                Matcher m = p.matcher(json);
                if (m.find()) {
                    server = m.group(1);
                }
            }
            if(server.isEmpty())
                server = "js1.amtb.cn";
        }
        return server;

    }

    //下载网页html
    public String getHtmlcodeWithoutHeader(String pageUrl, String encoding) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(pageUrl).build();
        try {
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

}
