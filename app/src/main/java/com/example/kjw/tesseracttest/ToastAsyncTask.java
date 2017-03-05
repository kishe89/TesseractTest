package com.example.kjw.tesseracttest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by kjw on 2017. 3. 2..
 */

public class ToastAsyncTask extends AsyncTask<Void,Void,Void> {
    private String TAG = "ToastAsyncTask";
    private String text;
    private Context context;
    private Mat img_input;
    TessBaseAPI tessBaseAPI;
    public ToastAsyncTask(TessBaseAPI tessBaseAPI, Context context, Mat img_input) {
        this.context = context;
        this.img_input = img_input;
        this.tessBaseAPI = tessBaseAPI;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        File dir = new File(path+"/imageprocess");
        if( !dir.exists() ) {
            Log.e(TAG,"CREATE DIRECTORY : "+dir.getAbsolutePath());
            dir.mkdirs();
        }
        File imgfile;
        Date timestamp = Calendar.getInstance().getTime();

        int resize_ratio = 4;
        Mat gray = new Mat();
        Mat edgeImg = new Mat();
        Mat output = new Mat();
        Mat dst = new Mat();
        Mat element = new Mat(10,10,CvType.CV_8U, new Scalar(1));
        //입력영상 칼라(RGB -> Gray)변환
        img_input.copyTo(gray);
        Imgproc.cvtColor(gray,gray,Imgproc.COLOR_BGR2GRAY);

        //입력영상 처리전 리사이징
        //카메라영상 해상도 및 크기 너무 커서 속도가 안나옴
        Imgproc.resize(gray,dst,new Size(gray.cols()/resize_ratio,gray.rows()/resize_ratio),0,0,Imgproc.INTER_CUBIC);

        //리사이징 이미지 이진화 임계값 계산(OTSU)
        double thr = Imgproc.threshold(dst, output, 0, 255, Imgproc.THRESH_OTSU);

        //엣지 디텍션 전처리(2*2마스크) 블러처리(잡음 축소)
        Imgproc.blur(dst,edgeImg,new Size(2,2));
        //엣지 디텍션(Canny)
        Imgproc.Canny(edgeImg,edgeImg,0,thr);
        imgfile = new File(dir+"/output"+ timestamp+"cany.jpeg");
        Imgcodecs.imwrite(imgfile.getAbsolutePath(), edgeImg);
        dst.copyTo(dst,edgeImg);
        //엣지영상 morphlogy Close and Or 연산(엣지 대비 증가)
        Imgproc.morphologyEx(dst,dst,Imgproc.MORPH_CLOSE,element);
        imgfile = new File(dir+"/output"+ timestamp+"MORPHCLOSE.jpeg");
        Imgcodecs.imwrite(imgfile.getAbsolutePath(), edgeImg);
        Imgproc.morphologyEx(dst,dst,Imgproc.MORPH_OPEN,element);
        imgfile = new File(dir+"/output"+ timestamp+"MORPHOPEN.jpeg");
        Imgcodecs.imwrite(imgfile.getAbsolutePath(), edgeImg);

        //Teseract Api 호출(Bitmap)
        tessBaseAPI.setImage(captureBitmap(dst));
        //검출된 문자열 UTF8포맷 텍스트로 변환
        text = tessBaseAPI.getUTF8Text();
        Log.e(TAG,"Image Text :"+text);
        imgfile = new File(dir+"/output"+ timestamp+"DST.jpeg");
        Imgcodecs.imwrite(imgfile.getAbsolutePath(), dst);

        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Toast.makeText(context,text,Toast.LENGTH_SHORT).show();

    }
    private Bitmap captureBitmap(Mat img_input){
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(img_input.cols(), img_input.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img_input, bitmap);
        }catch(Exception ex){
            Log.e(TAG,ex.getMessage());
        }
        return bitmap;
    }
}
