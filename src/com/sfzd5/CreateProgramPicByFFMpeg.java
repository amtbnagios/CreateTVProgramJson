package com.sfzd5;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CreateProgramPicByFFMpeg {

    public static boolean makeScreenCut(String videoRealPath, String cutTime, String imageRealName, String cardImageName, boolean forceCardImage) {
        File bg = new File("pic", imageRealName);
        File card = new File("pic",cardImageName);

        String path = bg.getPath();
        if (!bg.exists()) {
            List<String> commend = new ArrayList<String>();
            //ffmpeg -ss 00:00:06 -i http://amtbsg.cloudapp.net/redirect/vod/_definst_/mp4/02/02-041/02-041-0001.mp4/playlist.m3u8 -f image2 -y -vframes 1 img.jpeg
            commend.add("ffmpeg");
            commend.add("-ss");
            commend.add(cutTime);
            commend.add("-i");
            commend.add(videoRealPath);
            commend.add("-y");
            commend.add("-f");
            commend.add("image2");
            commend.add("-vframes");
            commend.add("1");
            commend.add(path);

            try {
                ProcessBuilder builder = new ProcessBuilder();
                builder.command(commend);
                builder.redirectErrorStream(true);
                System.out.println("视频截图开始...");
                System.out.println(videoRealPath);
                Process process = builder.start();
                InputStream in = process.getInputStream();
                byte[] bytes = new byte[2048];
                System.out.print("正在进行截图，请稍候");

                process.waitFor(1, TimeUnit.MINUTES);

                if(process.isAlive()){
                    Thread.sleep(1000);
                    process.destroy();
                }

                System.out.println("");
                System.out.println("视频截取完成...");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("视频截图失败！");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(bg.exists()) {
            if (!card.exists() || forceCardImage) {
                try {
                    BufferedImage bgimg = ImageIO.read(bg);
                    BufferedImage cardimg = ImgHelper.cutCenterImage(bgimg, 240, 260);
                    ImageIO.write(cardimg, "jpg", card);
                    return  true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if(card.exists()){
                return  true;
            } else {
                System.out.println("error(filename=" + imageRealName + ")");
                return false;
            }
        }
        return false;
    }



}
