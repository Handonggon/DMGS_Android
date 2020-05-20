package Koreatech.grad_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

public class ShowImageActivity extends AppCompatActivity {
    private static final String TAG = CompareActivity.class.getSimpleName();
    private SharedPreferences infoData;
    /*intent로 받아오는 값들*/
    private int imgNum;
    private int exhibitionNum;
    /*SharedPreferences로 받아오는 값들*/
    private String exhibitName;
    private Bitmap exhibitImg;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_img);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            throw new NullPointerException("Null ActionBar");
        } else {
            actionBar.hide();
        }

        infoData = getSharedPreferences("infoData", MODE_PRIVATE);

        ImageView imgView;
        TextView textView;

        Intent intent = getIntent();
        exhibitionNum = intent.getIntExtra("exhibitionNum", -1);     //전시관 번호(0~5)
        imgNum = intent.getIntExtra("imgNum", -1);                   //몇번째 이미지인지(0~max-1)

        exhibitName = infoData.getString("EXHIBIT_" + (exhibitionNum + 1) + "_" + (imgNum + 1) + "_1", "");
        File file = new File(getCacheDir().toString());
        File[] files = file.listFiles();
        String FileName = infoData.getString("EXHIBIT_" + (exhibitionNum + 1) + "_" + (imgNum + 1) + "_3", "")  + ".jpg";
        Log.d("FileName", FileName);
        for(File tempFile : files) {
            if(tempFile.getName().contains(FileName)) {
                Log.d("exhibitdate", tempFile.getName());
                exhibitImg = BitmapFactory.decodeFile(getCacheDir() + "/" + tempFile.getName());
                break;
            }
        }

        imgView = findViewById(R.id.correct_img);
        imgView.setImageBitmap(exhibitImg);
        textView = findViewById(R.id.title_text);
        textView.setText(exhibitName);
    }
    public void onClick(View v) {
        Intent intent = new Intent(ShowImageActivity.this, CompareActivity.class);
        intent.putExtra("exhibitionNum", exhibitionNum);      //전시관 번호(0~5)
        intent.putExtra("imgNum", imgNum);                  //몇번째 이미지인지(0~max-1)
        startActivityForResult(intent, 1000);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            switch(requestCode) {
                case 1000:  //NormalActivity로 보냄
                    Intent sendIntent = new Intent(ShowImageActivity.this, NormalActivity.class);
                    sendIntent.putExtra("isSuccess", data.getBooleanExtra("isSuccess", false));
                    setResult(RESULT_OK, sendIntent);
                    finish();
                    break;
            }
        }
    }
}
