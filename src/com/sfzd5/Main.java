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
        if(args!=null){
            if(args.length>0){
                String parm = args[0];
                if(parm.toLowerCase().equals("createpic")){
                    createPic();
                } else if(parm.toLowerCase().equals("removeerrprogram")){
                    removeErrProgram();
                } else if(parm.toLowerCase().equals("updateids")){
                    updateIds();
                } else if(parm.toLowerCase().equals("printprograms")){
                    printPrograms();
                } else if(parm.toLowerCase().equals("checkprogrampic")){
                    checkProgramPic();
                }
            } else {
                exportWithUpdateAll();
            }
        }
    }

    static void exportWithUpdateAll(){
        ExportTVJsonFromWebApi exportTVJsonFromWebApi = new ExportTVJsonFromWebApi();
        exportTVJsonFromWebApi.checkNew();
    }

    static void createPic(){
        List<Program> programs = new ArrayList<>();
        File pfile = new File("json", "tvprogram.txt");
        try {
            String json = FileUtils.readFileToString(pfile, "utf-8");
            JsonResult jsonResult = new Gson().fromJson(json, JsonResult.class);
            for (Channel channel : jsonResult.channels) {
                programs.addAll(channel.programs);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        CreatePic createPic = new CreatePic(programs);
        createPic.start();
    }

    static void removeErrProgram(){
        List<String> errProgramIdentifiers = null;
        try {
            errProgramIdentifiers = FileUtils.readLines(new File("errProgram.txt"), "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        File pfile = new File("json", "tvprogram.txt");
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

    static void checkProgramPic(){
        File pfile = new File("json", "tvprogram.txt");
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

    static void updateIds(){
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

    static void printPrograms(){
        File pfile = new File("json", "tvprogram.txt");
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
