package Koreatech.grad_project;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;


public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = CameraActivity.class.getSimpleName();

    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat matInput;
    private Mat matResult;
    private int[] points;
    private long mLastClickTime = 0;

    public static Bitmap bitmap;
    public static Point[] rect = new Point[4];

    private final ReentrantLock locker = new ReentrantLock();

    public native int[] ROI(long input_mat);

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            throw new NullPointerException("Null ActionBar");
        } else {
            actionBar.hide();
        }

        mOpenCvCameraView = findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(0); // froremoveVient-camera(1),  back-camera(0)
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        ImageButton shutter = findViewById(R.id.button_capture);
        shutter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 10000) {
                    return;
                }
                if(matInput != null && matInput.width() > 0) {
                    mLastClickTime = SystemClock.elapsedRealtime();

                    Intent intent = new Intent(CameraActivity.this, OCRActivity.class);

                    Bitmap lotate = Bitmap.createBitmap(matInput.width(), matInput.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(matInput, lotate);

                    /*90도 회전*/
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    bitmap = Bitmap.createBitmap(lotate, 0, 0, lotate.getWidth(), lotate.getHeight(), matrix, true);
                    List<Point> pointList = new ArrayList<Point>();
                    pointList.add(new Point(lotate.getHeight()-points[1], points[0]));
                    pointList.add(new Point(lotate.getHeight()-points[3], points[2]));
                    pointList.add(new Point(lotate.getHeight()-points[5], points[4]));
                    pointList.add(new Point(lotate.getHeight()-points[7], points[6]));
                    Collections.sort(pointList, new Comparator<Point>() {
                        public int compare(Point o1, Point o2) {
                            return ((Double)o1.y).compareTo(((Double)o2.y));
                        }
                    });

                    if(pointList.get(0).x > pointList.get(1).x) {
                        rect[0] = pointList.get(1);
                        rect[1] = pointList.get(0);
                    }
                    else {
                        rect[0] = pointList.get(0);
                        rect[1] = pointList.get(1);
                    }
                    if(pointList.get(2).x > pointList.get(3).x) {
                        rect[2] = pointList.get(2);
                        rect[3] = pointList.get(3);
                    }
                    else {
                        rect[2] = pointList.get(3);
                        rect[3] = pointList.get(2);
                    }
                    startActivityForResult(intent, 0);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0) {
            // 인식 완료되면
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent(CameraActivity.this, PopupOcrActivity.class);
                setResult(RESULT_OK, intent);
                intent.putExtra("result", data.getStringExtra("result"));
                finish();
            }
            // 사용자가 인식 기능 사용 취소하면
            else if (resultCode == RESULT_CANCELED) {
                mLastClickTime = 0;
            }
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) { }

    @Override
    public void onCameraViewStopped() { }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        matResult = inputFrame.rgba();
        locker.lock();
        if( matResult != null) {
            Mat tempMat = matResult.clone();
            int[] temp = ROI(matResult.getNativeObjAddr());
            int sum = 0;
            for(int i = 0; i < 8; i++) sum += temp[i];
            if(sum != 0) {
                points = temp;
                matInput = tempMat.clone();
            }
        }
        locker.unlock();
        return matResult;
    }
}