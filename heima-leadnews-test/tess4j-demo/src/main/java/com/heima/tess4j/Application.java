package com.heima.tess4j;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.io.File;

/**
 * 图片识别出文字
 */
public class Application {
    public static void main(String[] args) throws TesseractException {
        //获取本地图片
        File file = new File("D:\\Develop\\zitiku\\ocr.png");
        //创建tesseract对象
        ITesseract iTesseract = new Tesseract();
        //设置字体库路径
        iTesseract.setDatapath("D:\\Develop\\zitiku\\tessdata");
        //设置为中文识别库
        iTesseract.setLanguage("chi_sim");
        //执行orc识别
        String doOCR = iTesseract.doOCR(file);
        doOCR=  doOCR.replaceAll("\\r|\\n","-").replaceAll(" ","");
        System.out.println("识别的结果为："+doOCR);


    }
}
