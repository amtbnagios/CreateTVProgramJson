package com.sfzd5;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

public class ImgHelper {
    /**
     * 根据尺寸图片居中裁剪
     * @param bufImg  图
     * @param w        宽
     * @param h        高
     * @return
     */
    public static BufferedImage cutCenterImage(BufferedImage bufImg, int w, int h){
        float sw = bufImg.getWidth();
        float sh = bufImg.getHeight();

        int zw, zh, x, y;
        if( sw/w < sh/h ){
            zw = w;
            zh = (int)(sh /(sw/w));
            x=0;
            y=(zh-h)/2;
        } else {
            zh = h;
            zw = (int)(sw /(sh/h));
            y=0;
            x=(zw-w)/2;
        }

        BufferedImage zimg = zoomImage(bufImg, zw, zh);
        BufferedImage img = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D)img.getGraphics();
        g.drawImage(zimg,0,0, w, h, x, y, x+w, y+h, null);

        return img;
    }

    /**
     * 根据图片规定尺寸缩放
     * @param bufImg  图
     * @param w        宽
     * @param h        高
     * @return
     */
    public static BufferedImage zoomImage(BufferedImage bufImg, int w,int h){

        double wr=0,hr=0;
        Image Itemp = bufImg.getScaledInstance(w, h, bufImg.SCALE_SMOOTH);
        wr=w*1.0/bufImg.getWidth();
        hr=h*1.0 / bufImg.getHeight();
        AffineTransformOp ato = new AffineTransformOp(AffineTransform.getScaleInstance(wr, hr), null);
        Itemp = ato.filter(bufImg, null);
        return (BufferedImage) Itemp;
    }
}
