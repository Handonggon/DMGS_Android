package Koreatech.grad_project;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import org.opencv.core.Point;

class CustomView extends android.support.v7.widget.AppCompatImageView {
    Point[] points;
    private int col;
    private int row;
    private int corner = 4;
    Bitmap bitmap;

    public CustomView(Context context) {
        super(context);
        points = CameraActivity.rect;
        bitmap = CameraActivity.bitmap;
        col = bitmap.getWidth();
        row = bitmap.getHeight();
    }
    public CustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        points = CameraActivity.rect;
        bitmap = CameraActivity.bitmap;
        col = bitmap.getWidth();
        row = bitmap.getHeight();
    }

    @Override @SuppressLint("DrawAllocation")
    protected void onDraw(Canvas canvas) {
        Paint paint1 = new Paint();
        paint1.setColor(Color.GREEN);
        paint1.setStrokeWidth(10f);
        canvas.drawBitmap(bitmap, 0, 0, null);
        canvas.drawCircle((float)points[0].x, (float)points[0].y, 30, paint1);
        canvas.drawCircle((float)points[1].x, (float)points[1].y, 30, paint1);
        canvas.drawCircle((float)points[2].x, (float)points[2].y, 30, paint1);
        canvas.drawCircle((float)points[3].x, (float)points[3].y, 30, paint1);

        Paint paint2 = new Paint();
        paint2.setColor(Color.GREEN);
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setStrokeWidth(10f);

        Path path = new Path();
        path.moveTo((float)points[0].x, (float)points[0].y);
        path.lineTo((float)points[0].x, (float)points[0].y);
        path.lineTo((float)points[1].x, (float)points[1].y);
        path.lineTo((float)points[2].x, (float)points[2].y);
        path.lineTo((float)points[3].x, (float)points[3].y);
        path.close();
        canvas.drawPath(path, paint2);
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int)event.getX();
        int y = (int)event.getY();
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:{
                corner = getCorner(x, y);
                break;
            }
            case MotionEvent.ACTION_UP:{
                break;
            }
            case MotionEvent.ACTION_MOVE:{
                if(x > 0 && x < col && y > 0 && y < row) {
                    if (corner == 0) {
                        points[0].x = x;
                        points[0].y = y;
                    } else if (corner == 1) {
                        points[1].x = x;
                        points[1].y = y;
                    } else if (corner == 2) {
                        points[2].x = x;
                        points[2].y = y;
                    } else if (corner == 3) {
                        points[3].x = x;
                        points[3].y = y;
                    }
                    invalidate();
                }
                break;
            }
        }
        return true;
    }

    private int getCorner(int x, int y){
        for (int i = 0; i < 4; i++){
            int dx = Math.abs(x - (int)points[i].x);
            int dy = Math.abs(y - (int)points[i].y);
            if(dx <= 40 && dx >= 0 && dy <= 40 && dy >= 0){
                return i;
            }
        }
        return 4;
    }
}
