package Koreatech.grad_project;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class LodingActivity  extends AppCompatActivity {
    private SharedPreferences infoData;
    int ImgNumByExhibit;
    List<URL> urls = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loding);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            throw new NullPointerException("Null ActionBar");
        } else {
            actionBar.hide();
        }

        ImageView rabbit = (ImageView) findViewById(R.id.lodingImg);
        GlideDrawableImageViewTarget gifImage = new GlideDrawableImageViewTarget(rabbit);
        Glide.with(this).load(R.drawable.loding).into(gifImage);

        infoData = getSharedPreferences("infoData", MODE_PRIVATE);

        NormalActivity.NormalClass normalClass = new NormalActivity.NormalClass();
        ImgNumByExhibit = normalClass.getImgNumByExhibit();

        if (!getNetworkInfo()) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("앱 사용을 위해서 네트워크 연결이 필요합니다.");
            builder.setNegativeButton("종료",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            setResult(Activity.RESULT_CANCELED);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                finishAffinity();
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        finishAffinity();
                                    }
                                });
                            }
                        }
                    });
            builder.setCancelable(false);
            builder.show();
        }
        else {
            getExhibitImg();
        }
    }

    @Override
    public void onBackPressed() {

    }

    public class ImageLoadTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() { }
        @Override
        protected Void doInBackground(Void... voids) {
            if(urls.size() == 0) {
                return null;
            }
            for(int i = 0; i < urls.size(); i++) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(urls.get(i).openConnection().getInputStream());
                    saveBitmapToJpeg(bitmap, String.valueOf(urls.get(i)));
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "사진 다운로드 오류", Toast.LENGTH_SHORT).show();
                }
            }
            return null;
        }
        @Override
        protected void onProgressUpdate(Void... voids) { }
        @Override
        protected void onPostExecute(Void result) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    finish();
                }
            }, 2000);
        }
    }
    public void getExhibitImg() {
        File file = new File(getCacheDir().toString());
        File[] files = file.listFiles();

        DbConnect dbConnect = new DbConnect(this);
        try {
            String result = dbConnect.execute(DbConnect.GET_EXHIBIT_IMG).get();
            Log.d("GET_EXHIBIT_IMG", result);
            JSONObject jResult = new JSONObject(result);
            JSONArray jArray = jResult.getJSONArray("result");
            for (int i = 0; i < jArray.length(); i++) {
                //캐시에 파일이 존재하는지 확인
                boolean isFile = false;
                String imgURL = jArray.getString(i);
                String fileName = imgURL.split("/")[imgURL.split("/").length-1] + ".jpg";
                for(File tmep : files) {
                    if (tmep.getName().contains(fileName)) {
                        isFile = true;
                        break;
                    }
                }
                if(!isFile) {
                    urls.add(new URL("http://" + imgURL));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "네트워크 통신 오류", Toast.LENGTH_SHORT).show();
        }
        if(!infoData.getBoolean("IS_EXHIBITDATA", false)) {      //한번만 실행
            inExhibitdata(); //전시물 목록 가져오기
        }
        else {
            ImageLoadTask imageLoadTask = new ImageLoadTask();
            imageLoadTask.execute();
        }
    }

    private boolean getNetworkInfo() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.isConnected();
        } else {
            return false;
        }
    }

    private void saveBitmapToJpeg(Bitmap bitmap, String name) {
        //내부저장소 캐시 경로를 받아옵니다.
        File storage = getCacheDir();
        //저장할 파일 이름
        String fileName = name.split("/")[name.split("/").length-1] + ".jpg";

        //storage 에 파일 인스턴스를 생성합니다.
        File tempFile = new File(storage, fileName);
        try {
            Log.d("fileName", fileName);
            // 자동으로 빈 파일을 생성합니다.
            tempFile.createNewFile();
            // 파일을 쓸 수 있는 스트림을 준비합니다.
            FileOutputStream out = new FileOutputStream(tempFile);
            // compress 함수를 사용해 스트림에 비트맵을 저장합니다.
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            // 스트림 사용후 닫아줍니다.
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "사진 다운로드 오류", Toast.LENGTH_SHORT).show();
        }
    }

    public void inExhibitdata() {
        SharedPreferences.Editor editor = infoData.edit();
        String result = getExhibitData();
        if(!result.equals("-1")) {
            try {
                for(int i = 0; i < 6; i++) {                                                //i:각 전시관 번호
                    JSONObject jResult = new JSONObject(result);
                    JSONArray jArray = jResult.getJSONArray(String.valueOf(i + 1));
                    for (int j = 0; j < jArray.length(); j++) {                             //j:각 전시물 번호
                        JSONObject exhibitArray = jArray.getJSONObject(j);                  //0:name, 1:MAC, 2:space, 3:img
                        Log.d("EXHIBIT_" + (i + 1) + "_" + (j + 1), String.valueOf(exhibitArray));
                        editor.putString("EXHIBIT_" + (i + 1) + "_" + (j + 1) + "_1", exhibitArray.getString("name"));
                        editor.putString("EXHIBIT_" + (i + 1) + "_" + (j + 1) + "_2", exhibitArray.getString("MAC"));
                        editor.putString("EXHIBIT_" + (i + 1) + "_" + (j + 1) + "_3", exhibitArray.getString("img"));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "네트워크 통신 오류", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "네트워크 통신 오류", Toast.LENGTH_SHORT).show();
        }

        editor.apply();

        ImageLoadTask imageLoadTask = new ImageLoadTask();
        imageLoadTask.execute();
    }

    public String getExhibitData() {
        DbConnect dbConnect = new DbConnect(this);
        String result = "-1";
        try {
            result = dbConnect.execute(DbConnect.GET_EXHIBIT, "", "", "", "", "", "", "", "", Integer.toString(ImgNumByExhibit)).get();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "네트워크 통신 오류", Toast.LENGTH_SHORT).show();
        }
        Log.d("GET_EXHIBIT", result);
        return result;
    }
}
