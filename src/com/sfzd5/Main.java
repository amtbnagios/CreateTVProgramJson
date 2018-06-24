package com.sfzd5;

import com.google.gson.Gson;
import com.sfzd5.tv.Channel;
import com.sfzd5.tv.JsonResult;
import com.sfzd5.tv.Program;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        exportWithUpdateAll();
    }

    static void exportUpdateIds(){
        File pfile = new File("updateIds.txt");
        List<String> ids = null;
        try {
            ids = FileUtils.readLines(pfile, "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(ids!=null && ids.size()>0) {
            ExportTVJsonFromWebApi exportTVJsonFromWebApi = new ExportTVJsonFromWebApi();
            exportTVJsonFromWebApi.updateProgramFiles(ids);
        }
    }

    static void downloadTs(){
        List<Program> programs = new ArrayList<>();
        File pfile = new File("json", "program.txt");
        try {
            String json = FileUtils.readFileToString(pfile, "utf-8");
            JsonResult jsonResult = new Gson().fromJson(json, JsonResult.class);
            for (Channel channel : jsonResult.channels) {
                programs.addAll(channel.programs);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        DownTs downTs = new DownTs(programs);
        downTs.start();
    }

    static void removeErrProgram(){
        List<String> errProgramIdentifiers = null;
        try {
            errProgramIdentifiers = FileUtils.readLines(new File("errProgram.txt"), "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        File pfile = new File("json", "program.txt");
        if (pfile.exists()) {
            try {
                String json = FileUtils.readFileToString(pfile, "utf-8");
                JsonResult jsonResult = new Gson().fromJson(json, JsonResult.class);
                for (Channel channel : jsonResult.channels) {
                    Iterator<Program> iterator = channel.programs.iterator();
                    while (iterator.hasNext()){
                        if(errProgramIdentifiers.contains(iterator.next().identifier))
                            iterator.remove();
                    }
                }
                FileUtils.write(pfile, new Gson().toJson(jsonResult), "utf-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static void initPicCreated(){
        File pfile = new File("json", "program.txt");
        if (pfile.exists()) {
            try {
                String json = FileUtils.readFileToString(pfile, "utf-8");
                JsonResult jsonResult = new Gson().fromJson(json, JsonResult.class);
                for (Channel channel : jsonResult.channels) {
                    for (Program item : channel.programs) {
                        File picBgFile = new File("pic", item.identifier+"_bg.jpg");
                        File picCardFile = new File("pic", item.identifier+"_card.jpg");
                        if(picBgFile.exists() && picCardFile.exists()){
                            item.picCreated = 1;
                        } else {
                            item.picCreated = 0;
                        }
                    }
                }
                FileUtils.write(pfile, new Gson().toJson(jsonResult), "utf-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static void createPic() {
        File pfile = new File("json", "program.txt");
        HashSet<String> programSet = new HashSet<>();
        if (pfile.exists()) {
            try {
                String json = FileUtils.readFileToString(pfile, "utf-8");
                JsonResult jsonResult = new Gson().fromJson(json, JsonResult.class);
                for (Channel channel : jsonResult.channels) {
                    for (Program item : channel.programs) {
                        String filename = item.identifier;
                        String[] us = item.identifier.split("-");
                        //ffmpeg -ss 00:00:06 -i m.ts -f image2 -y -vframes 1 img.jpeg

                        File tsFile = new File("ts", item.identifier + ".ts");
                        if (tsFile.exists()) {
                            //String mediaUrl = "http://amtbsg.cloudapp.net/redirect/vod/_definst_/mp4/" + us[0] + "/" + item.identifier + "/" + item.filename + "/playlist.m3u8";
                            String mediaUrl = tsFile.getAbsolutePath();

                            boolean t = CreateProgramPicByFFMpeg.makeScreenCut(mediaUrl, "00:00:06", item.identifier + "_bg.jpg", item.identifier + "_card.jpg", false);
                            if (t)
                                System.out.println("创建完成" + filename);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static void exportWithUpdateAll(){
        ExportTVJsonFromWebApi exportTVJsonFromWebApi = new ExportTVJsonFromWebApi();
        exportTVJsonFromWebApi.checkNew();
    }

    static void remove(){
        File pfile = new File("json", "program.txt");
        HashSet<String> programSet = new HashSet<>();
        if(pfile.exists()) {
            try {
                String json = FileUtils.readFileToString(pfile, "utf-8");
                JsonResult jsonResult = new Gson().fromJson(json, JsonResult.class);
                for(Channel channel : jsonResult.channels){
                    Iterator<Program> iterator = channel.programs.iterator();
                    while (iterator.hasNext()){
                        Program program = iterator.next();
                        if(programSet.contains(program.identifier)){
                            iterator.remove();
                        } else {
                            programSet.add(program.identifier);
                        }
                    }
                }

                try {
                    FileUtils.write(pfile, new Gson().toJson(jsonResult), "utf-8");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static void printProgramCount(){
        File pfile = new File("json", "program.txt");
        HashSet<String> programSet = new HashSet<>();
        if(pfile.exists()) {
            try {
                String json = FileUtils.readFileToString(pfile, "utf-8");
                JsonResult jsonResult = new Gson().fromJson(json, JsonResult.class);
                for(Channel channel : jsonResult.channels){

                    for(Program program : channel.programs){
                        System.out.print(program.identifier);
                        System.out.print(",");
                        System.out.print(channel.name);
                        System.out.print(",");
                        System.out.print(program.name);
                        System.out.print(",");
                        System.out.print(program.recDate);
                        System.out.print(",");
                        System.out.println(program.files.size());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
