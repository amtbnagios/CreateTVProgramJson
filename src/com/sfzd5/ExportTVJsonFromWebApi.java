package com.sfzd5;

import com.google.gson.Gson;
import com.sfzd5.tv.Channel;
import com.sfzd5.tv.JsonResult;
import com.sfzd5.tv.Program;
import com.sfzd5.xmlbean.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ExportTVJsonFromWebApi {
    //需要更新的文件
    File programsFile;
    File newFile;
    File emptyMp4UrlsFile;
    File allIdFile;

    //不需要更新的文件
    File errProgramFile;
    File noUpdateIdsFile;

    JsonResult jsonResult;
    List<String> emptyMp4ProgramUrls;
    HashMap<String, String> allIdMap;
    List<String> errProgramIdentifiers;
    List<String> noUpdateProgramIdentifiers;
    List<String> newFiles;

    long dtime;
    int newFileCount;

    HashSet<String> urls;


    public ExportTVJsonFromWebApi()
    {
        dtime = new Date().getTime();
        AmtbQuery.dtime = dtime;
        {
            File dirJson = new File("json");
            if (!dirJson.exists()) dirJson.mkdir();

            File dirCache = new File("cache");
            if (!dirCache.exists()) dirCache.mkdir();

            File dirPic = new File("pic");
            if (!dirPic.exists()) dirPic.mkdir();

            File dirTs = new File("ts");
            if (!dirTs.exists()) dirTs.mkdir();

            File dirVol = new File("vol");
            if (!dirVol.exists()) dirVol.mkdir();
        }

        programsFile = new File("json", "program.txt");
        if(programsFile.exists()) {
            try {
                String json = FileUtils.readFileToString(programsFile, "utf-8");
                jsonResult = new Gson().fromJson(json, JsonResult.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(jsonResult==null){
            jsonResult = new JsonResult();
            jsonResult.channels = new ArrayList<>();
        }
        newFileCount = 0;
        urls = new HashSet<>();
        allIdMap = new HashMap<>();

        emptyMp4UrlsFile = new File("update","emptyMp4UrlsFile.txt");
        allIdFile = new File("update","allid.txt");
        errProgramFile = new File("update", "errProgram.txt");
        newFile = new File("update", "new_"+dtime+".txt");
        noUpdateIdsFile = new File("update", "noUpdateIds.txt");

        if(emptyMp4UrlsFile.exists()) {
            try {
                emptyMp4ProgramUrls = FileUtils.readLines(emptyMp4UrlsFile, "utf-8");
                errProgramIdentifiers = FileUtils.readLines(errProgramFile, "utf-8");
                noUpdateProgramIdentifiers = FileUtils.readLines(noUpdateIdsFile, "utf-8");
                List<String> ids = FileUtils.readLines(allIdFile, "utf-8");
                for(String s : ids){
                    String[] sp = s.split(",");
                    if(sp.length==4){
                        allIdMap.put(sp[0], s);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        newFiles = new ArrayList<>();

        if(emptyMp4ProgramUrls==null)
            emptyMp4ProgramUrls = new ArrayList<>();
        if(errProgramIdentifiers==null)
            errProgramIdentifiers = new ArrayList<>();

    }

    public void checkNew(){
        checkCategory();
        cleanSave();
    }

    private void cleanSave(){

        //删除files为0的program
        Iterator<Channel> channelIterator = jsonResult.channels.iterator();
        while (channelIterator.hasNext()){
            Channel channel = channelIterator.next();
            Iterator<Program> programIterator = channel.programs.iterator();
            while (programIterator.hasNext()){
                Program program = programIterator.next();
                if(program.files==null || program.files.size()==0){
                    programIterator.remove();
                }
            }
            if(channel.programs.size()==0){
                channelIterator.remove();
            }
        }

        try {
            FileUtils.write(programsFile, new Gson().toJson(jsonResult), "utf-8");
            FileUtils.writeLines(emptyMp4UrlsFile, emptyMp4ProgramUrls);
            FileUtils.writeLines(allIdFile, allIdMap.values());
            FileUtils.writeLines(newFile, newFiles);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * 检测是否有新的内容
     * */
    public void checkCategory() {
        HashMap<String, Channel> ids = new HashMap<>();
        for (Channel c : jsonResult.channels)
            ids.put(c.name, c);
        CategoryListResult categoryListResult = AmtbQuery.queryCategoryListResult();
        for (CategoryListItem c : categoryListResult.getList().getItem()) {
            if (ids.containsKey(c.getName())) {
                checkMovie(c, ids.get(c.getName()));
            } else {
                Channel channel = new Channel();
                channel.name = c.getName();
                channel.programs = new ArrayList<>();
                jsonResult.channels.add(channel);
                insertMovie(c, channel);
            }
        }
    }

    public void checkMovie(CategoryListItem category, Channel channel){
        HashMap<String, Program> ids = new HashMap<>();
        for(Program program :channel.programs){
            ids.put(program.identifier, program);
        }
        List<ProgramListResult> results = (List<ProgramListResult>) AmtbQuery.queryProgramListResult(category.getAmtbid());
        for(ProgramListResult result : results){
            for(ProgramListItem c : result.getList().getItem()){
                if(!allIdMap.containsKey(c.getLectureno())) {
                    StringBuilder sb = new StringBuilder();
                    allIdMap.put(c.getLectureno(), sb.append(c.getLectureno()).append(",").append(c.getAmtbid()).append(",").append(c.getSubamtbid()).append(c.getLectureid()).toString());
                }
                if(!noUpdateProgramIdentifiers.contains(c.getLectureno())) {
                    if (ids.containsKey(c.getLectureno())) {
                        checkFile(c, ids.get(c.getLectureno()));
                        Program p = ids.get(c.getLectureno());
                        p.files.sort(compareTo);
                    } else {
                        if (!errProgramIdentifiers.contains(c.getLectureno())) {
                            Program program = insertProgram(channel, c);
                            if (program != null) {
                                ids.put(program.identifier, program);
                            }
                        }
                    }
                }
            }
        }
    }

    public void updateProgramFiles(List<String> identifierList){
        for(String identifier : identifierList) {
            //lectureno,amtbid,subamtbid,lectureid
            if (allIdMap.containsKey(identifier)) {
                String f = allIdMap.get(identifier);

                Program p = null;
                for (Channel channel : jsonResult.channels) {
                    for (Program program : channel.programs) {
                        if (program.identifier.equals(identifier)) {
                            p = program;
                            break;
                        }
                    }
                    if (p != null)
                        break;
                }

                if (p != null) {
                    ProgramListItem movie = new ProgramListItem();
                    String[] sp = f.split(",");
                    movie.setAmtbid(Integer.parseInt(sp[1]));
                    movie.setSubamtbid(Integer.parseInt(sp[2]));
                    movie.setLectureid(Integer.parseInt(sp[3]));
                    movie.setLectureno(identifier);
                    checkFile(movie, p);
                    p.files.sort(compareTo);
                } else {
                    System.out.println("未找到" + identifier);
                }
            } else {
                System.out.println("未找到" + identifier);
            }
        }
        cleanSave();
    }

    private Program movie2Program(Channel channel, ProgramListItem movie){
        Program program = new Program();
        program.files = new ArrayList<>();

        File picBgFile = new File("pic", movie.getLectureno()+"_bg.jpg");
        File picCardFile = new File("pic", movie.getLectureno()+"_card.jpg");
        program.picCreated = (picBgFile.exists() && picCardFile.exists())?1:0;

        program.channel = channel.name;
        program.identifier = movie.getLectureno();
        program.name = movie.getLecturename();
        program.recAddress = movie.getLectureaddr();
        program.recDate = movie.getLecturedate();
        return program;
    }

    //添加节目并添加文件
    private Program insertProgram(Channel channel, ProgramListItem movie){
        String url = "http://www.amtb.tw/app/unicast2xml.asp?act=4&amtbid="+String.valueOf(movie.getAmtbid())+"&subamtbid="+String.valueOf(movie.getSubamtbid())+"&lectureid="+String.valueOf(movie.getLectureid());
        if(!emptyMp4ProgramUrls.contains(url)) {
            Program program = movie2Program(channel, movie);
            insertFile(movie, program);
            if (program.files.size() > 0) {
                channel.programs.add(program);
                program.files.sort(compareTo);

                if(program.picCreated==0){
                    createPic(movie.getLectureno(), program.files.get(0));
                }

                return program;
            } else {
                emptyMp4ProgramUrls.add(url);
            }
        }
        return null;
    }

    private void createPic(String identifier, String filename){
        File tsFile = DownTs.downTs(identifier, filename);
        if (tsFile!=null && tsFile.exists()) {
            //String mediaUrl = "http://amtbsg.cloudapp.net/redirect/vod/_definst_/mp4/" + us[0] + "/" + item.identifier + "/" + item.filename + "/playlist.m3u8";
            String mediaUrl = tsFile.getAbsolutePath();
            boolean t = CreateProgramPicByFFMpeg.makeScreenCut(mediaUrl, "00:00:06", identifier + "_bg.jpg", identifier + "_card.jpg", false);
            if (t)
                System.out.println("创建缩图完成 " + identifier);
            else
                System.out.println("创建缩图出错 " + identifier);
        } else {
            System.out.println("创建缩图出错 " + identifier);
        }
    }

    public void insertMovie(CategoryListItem category, Channel channel){
        List<ProgramListResult> results = (List<ProgramListResult>) AmtbQuery.queryProgramListResult(category.getAmtbid());
        for(ProgramListResult result : results){
            for(ProgramListItem c : result.getList().getItem()){
                if(!allIdMap.containsKey(c.getLectureno())) {
                    StringBuilder sb = new StringBuilder();
                    allIdMap.put(c.getLectureno(), sb.append(c.getLectureno()).append(",").append(c.getAmtbid()).append(",").append(c.getSubamtbid()).append(c.getLectureid()).toString());
                }
                if(!noUpdateProgramIdentifiers.contains(c.getLectureno()))
                    insertProgram(channel, c);
            }
        }
    }

    Comparator<String> compareTo = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.compareTo(o2);
        }
    };

    private void insertFile(ProgramListItem movie, Program program) {
        MediaListResult mediaListResultLast = null;
        MediaListResult mediaListResultLastP = null;
        File l = new File("vol", program.identifier+".json");
        if(l.exists()){
            String json = null;
            try {
                json = FileUtils.readFileToString(l, "utf-8");
                mediaListResultLast = new Gson().fromJson(json, MediaListResult.class);
                mediaListResultLastP = mediaListResultLast;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        MediaListResult mediaListResult;
        int lsize=0, lvolsize=0, idx=0;
        if(mediaListResultLast!=null){
            mediaListResult = AmtbQuery.queryMediaListResult(mediaListResultLast.getAmtbid(), mediaListResultLast.getSubamtbid(), mediaListResultLast.getLectureid(), mediaListResultLast.getThisVol());

            lsize = mediaListResultLast.getList().getItem().size();
            lvolsize = mediaListResultLast.getVoltotal();
            for(int i=0; i<mediaListResultLast.getVollist().size(); i++){
                if(mediaListResultLast.getVollist().get(i).getItem().getVolno().equals(mediaListResultLast.getThisvolno())){
                    idx=i;
                    break;
                }
            }
        } else {
            mediaListResult = AmtbQuery.queryMediaListResult(movie.getAmtbid(), movie.getSubamtbid(), movie.getLectureid());
        }

        if(mediaListResult!=null) {
            if (mediaListResult.getList().getItem().size() != lsize) {
                mediaListResultLast = mediaListResult;
                int c = 0;
                for (MediaListItem media : mediaListResult.getList().getItem()) {
                    if (media.getFiletype().equals("flv")) {
                        String sp[] = media.getFileurl().split("/");
                        String filename = sp[2].replace("flv", "mp4");
                        c++;
                        if (!urls.contains(filename)) {
                            insertFileName(filename, program);
                            urls.add(filename);
                        }
                    }
                }

                if (c > 0 && lvolsize < mediaListResult.getVollist().size()) {
                    for (int i = idx + 1; i < mediaListResult.getVollist().size(); i++) {
                        VolListItem item = mediaListResult.getVollist().get(i);
                        if (!item.getItem().getVolno().equals(mediaListResult.getThisvolno())) {
                            MediaListResult mediaListResult2 = AmtbQuery.queryMediaListResult(movie.getAmtbid(), movie.getSubamtbid(), movie.getLectureid(), item.getItem().getVolid());
                            mediaListResultLast = mediaListResult2;
                            if (mediaListResult2 != null) {
                                for (MediaListItem media : mediaListResult2.getList().getItem()) {
                                    if (media.getFiletype().equals("flv")) {
                                        String sp[] = media.getFileurl().split("/");
                                        String filename = sp[2].replace("flv", "mp4");
                                        if (!urls.contains(filename)) {
                                            insertFileName(filename, program);
                                            urls.add(filename);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if(mediaListResultLast!=null && mediaListResultLast!=mediaListResultLastP){
            String json = new Gson().toJson(mediaListResultLast);
            try {
                FileUtils.writeStringToFile(l, json, "utf-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void insertFileName(String filename, Program program){
        program.files.add(filename);
        newFileCount++;
        StringBuilder sb = new StringBuilder();
        sb.append(program.identifier).append(",");
        sb.append(program.name).append(",");
        sb.append(program.channel).append(",");
        sb.append(program.files.size()).append(",");
        newFiles.add(sb.toString());
        System.out.println(String.valueOf(newFileCount)+ ":" + filename);
    }

    public void checkFile(ProgramListItem movie, Program program){
        for(String url : program.files)
            if(!urls.contains(url))
                urls.add(url);

        insertFile(movie, program);
    }
}
