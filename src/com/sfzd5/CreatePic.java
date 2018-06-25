package com.sfzd5;

import com.sfzd5.tv.Program;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreatePic {

    static Pattern idPattern = Pattern.compile("chunklist_([^\\.]*)\\.m3u8");
    int total=0;

    //服务器，每个服务器一个线程
    List<String> servers;
    //线程安全list存放结果
    private Object obj = new Object();
    private Stack<Program> createProgramPicItemStack;

    private int threadCount = 0;

    public CreatePic(List<Program> programs){
        createProgramPicItemStack = new Stack<>();
        createProgramPicItemStack.addAll(programs);
        total = programs.size();
        try {
            servers = FileUtils.readLines(new File("update", "servers.txt"), "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        for (int i = 0; i < servers.size(); i++) {
            final String[] server = {servers.get(i)};
            new Thread(new Runnable() {
                @Override
                public void run() {
                    threadRun(server[0]);
                }
            }).start();
        }
    }

    public void threadRun(String server) {
        threadCount++;
        OkHttpClient client = new OkHttpClient();
        Program item;

        synchronized (obj) {
            try {
                item = createProgramPicItemStack.pop();
            } catch (EmptyStackException ese){
                item = null;
            }
        }
        while (item!=null){
            total--;
            File tsFile = new File("ts",item.identifier+".ts");
            String filename = item.files.get(0);
            if(!tsFile.exists()) {
                String[] us = item.identifier.split("-");
                //http://js1.amtb.cn                 /vod/_definst_/mp4/02/02-041/02-041-0001.mp4/playlist.m3u8
                //hz1.hwadzan.net
                //hz.hwadzan.net
                //http://amtbsg.cloudapp.net/redirect/vod/_definst_/mp4/02/02-041/02-041-0001.mp4/playlist.m3u8
                String mediaUrl = "http://" + server + "/vod/_definst_/mp4/" + us[0] + "/" + item.identifier + "/" + filename + "/";
                String m3u8 = downHtml(client, mediaUrl + "playlist.m3u8");
                Matcher matcher = idPattern.matcher(m3u8);
                if (matcher.find()) {
                    //media_w1259047207_9.ts
                    boolean ok = downFile(client, mediaUrl + "media_" + matcher.group(1) + "_9.ts", tsFile);
                    if(ok && tsFile.exists()){
                        boolean t = CreateProgramPicByFFMpeg.makeScreenCut(tsFile.getAbsolutePath(), "00:00:06", item.identifier + "_bg.jpg", item.identifier + "_card.jpg", false);
                        if (t)
                            System.out.println("创建缩图完成 " + item.identifier);
                        else
                            System.out.println("创建缩图出错 " + item.identifier);
                    }
                    System.out.println(String.format("完成 %s ，还有 %d 个", item.identifier, total));
                } else {
                    System.out.println("出错 " + item.identifier);
                }
            }
            synchronized (obj) {
                try {
                    item = createProgramPicItemStack.pop();
                } catch (EmptyStackException ese){
                    item = null;
                }
            }
        }

        threadCount--;
        if(threadCount==0){
            System.out.println("完成");
        }
    }

    public static File downTs(String identifier, String filename){
        String server = "js1.amtb.cn";
        File tsFile = new File("ts",identifier+".ts");
        boolean ok = false;
        OkHttpClient client = new OkHttpClient();
        if(!tsFile.exists()) {
            String[] us = identifier.split("-");
            String mediaUrl = "http://" + server + "/vod/_definst_/mp4/" + us[0] + "/" + identifier + "/" + filename + "/";
            String m3u8 = downHtml(client, mediaUrl + "playlist.m3u8");
            Matcher matcher = idPattern.matcher(m3u8);
            if (matcher.find()) {
                //media_w1259047207_9.ts
                ok = downFile(client, mediaUrl + "media_" + matcher.group(1) + "_9.ts", tsFile);
                if(ok) {
                    System.out.println(String.format("完成 " + identifier));
                }else {
                    System.out.println("出错 " + identifier);
                }
            } else {
                System.out.println("出错 " + identifier);
            }
        }
        if(ok)
            return tsFile;
        else
            return null;
    }

    private static String downHtml(OkHttpClient client, String url) {

        Request request = new Request.Builder().url(url).build();
        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            String html = new String(response.body().bytes());
            response.close();
            return html;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static boolean downFile(OkHttpClient client, String url, File tsFile){
        Request request = new Request.Builder().url(url).build();
        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            FileUtils.writeByteArrayToFile(tsFile, response.body().bytes());
            response.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
