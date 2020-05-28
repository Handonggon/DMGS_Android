#include <jni.h>
#include <opencv2/opencv.hpp>
#include "opencv2/core.hpp"
#include "opencv2/core/utility.hpp"
#include "opencv2/core/ocl.hpp"
#include "opencv2/imgcodecs.hpp"
#include "opencv2/highgui.hpp"
#include "opencv2/features2d.hpp"
#include "opencv2/calib3d.hpp"
#include "opencv2/imgproc.hpp"
#include <iostream>
#include <android/log.h>

using namespace cv;
using namespace std;

const float RATIO = 0.65f;

struct ORBDetector {
    Ptr<Feature2D> orb;
    ORBDetector(double hessian = 2000)
    {
        orb = ORB::create(hessian);
    }
    template<class T>
    void operator()(const T& in, const T& mask, std::vector<KeyPoint>& pts, T& descriptors, bool useProvided = false)
    {
        orb->detectAndCompute(in, mask, pts, descriptors, useProvided);
    }
};
struct ORBMatcher {
    Ptr<BFMatcher> matcher;
    ORBMatcher() {
        matcher = BFMatcher::create(NORM_HAMMING);
    }
    template<class T>
    void operator()(const T &in1, const T &in2, std::vector<std::vector<DMatch>>& pts, int k) {
        matcher->knnMatch(in1, in2, pts, k);
    }
};

float resize(UMat img_src, UMat &img_resize, int resize_width){

    float scale = resize_width / (float)img_src.cols ;

    if (img_src.cols > resize_width) {
        int new_height = cvRound(img_src.rows * scale);
        resize(img_src, img_resize, Size(resize_width, new_height));
    }
    else {
        img_resize = img_src;
    }
    return scale;
}

void sortCorners(std::vector<Point2f>& corners)         //Ecken ausrichten von Karten
{
    std::vector<Point2f> top, bot;
    Point2f center;
    // Get mass center
    for (int i = 0; i < corners.size(); i++)
        center += corners[i];
    center *= (1. / corners.size());

    for (int i = 0; i < corners.size(); i++)
    {
        if (corners[i].y < center.y)
            top.push_back(corners[i]);
        else
            bot.push_back(corners[i]);
    }
    corners.clear();

    if (top.size() == 2 && bot.size() == 2) {
        Point2f tl = top[0].x > top[1].x ? top[1] : top[0];
        Point2f tr = top[0].x > top[1].x ? top[0] : top[1];
        Point2f bl = bot[0].x > bot[1].x ? bot[1] : bot[0];
        Point2f br = bot[0].x > bot[1].x ? bot[0] : bot[1];

        corners.push_back(tl);
        corners.push_back(tr);
        corners.push_back(br);
        corners.push_back(bl);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_Koreatech_grad_1project_OCRActivity_warp(JNIEnv *env, jobject thiz, jlong input_mat, jintArray rect) {

    Mat& img = *(Mat *) input_mat;
    Mat reImg = img.clone();

    jint *value = env->GetIntArrayElements(rect, NULL);

    vector<Point2f> selected_points = {Point2f((float)value[0], (float)value[1]),
                                       Point2f((float)value[2], (float)value[3]),
                                       Point2f((float)value[4], (float)value[5]),
                                       Point2f((float)value[6], (float)value[7])};

    vector<Point2f> quad_pts;
    quad_pts.push_back(Point2f(0, 0));
    quad_pts.push_back(Point2f(0, reImg.rows));
    quad_pts.push_back(Point2f(reImg.cols, 0));
    quad_pts.push_back(Point2f(reImg.cols, reImg.rows));

    Mat transmtx = getPerspectiveTransform(selected_points, quad_pts);
    warpPerspective(reImg, img, transmtx, reImg.size());
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_Koreatech_grad_1project_CameraActivity_ROI(JNIEnv *env, jobject thiz, jlong input_mat) {
    ocl::setUseOpenCL(true);

    Mat& img = *(Mat *) input_mat;
    double imgArea = img.cols * img.rows;
    Mat gray = img.clone();

    cvtColor( gray, gray, COLOR_BGR2GRAY);

    GaussianBlur(gray, gray, Size(5, 5), 1.5, 1.5);
    Canny(gray, gray, 50, 150, 3);

    vector<vector<Point>> contours;

    findContours(gray, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

    sort(contours.begin(), contours.end(), [](const vector<Point>& c1, const vector<Point>& c2) {
        return contourArea(c1, false) > contourArea(c2, false);
    });

    int rect[8] = {0,};
    vector<Point> approx;
    for (size_t i = 0; i < contours.size(); i++) {
        approxPolyDP(contours[i], approx, 0.02 * arcLength(contours[i], true), true);
        double area = contourArea(approx);
        if (approx.size() == 4 && isContourConvex(Mat(approx)) && area > imgArea/5 ) {
            line(img, approx[0], approx[1], Scalar(0, 255, 0), 3);
            line(img, approx[1], approx[2], Scalar(0, 255, 0), 3);
            line(img, approx[2], approx[3], Scalar(0, 255, 0), 3);
            line(img, approx[3], approx[0], Scalar(0, 255, 0), 3);
            rect[0] = approx[0].x;
            rect[1] = approx[0].y;
            rect[2] = approx[1].x;
            rect[3] = approx[1].y;
            rect[4] = approx[2].x;
            rect[5] = approx[2].y;
            rect[6] = approx[3].x;
            rect[7] = approx[3].y;
            break;
        }
    }
    jintArray outer = env->NewIntArray(8);
    env->SetIntArrayRegion(outer, 0, 8, rect);
    return outer;
}

extern "C"
JNIEXPORT jint JNICALL
Java_Koreatech_grad_1project_CompareActivity_imageprocessing(JNIEnv *env, jobject thiz,
                                                              jlong object_image,
                                                              jlong scene_image) {
    ocl::setUseOpenCL(true);

    UMat img1, img2;

    Mat &img_object = *(Mat *) object_image;
    Mat &img_scene = *(Mat *) scene_image;

    img_object.copyTo(img1);
    img_scene.copyTo(img2);

    resize(img1, img1, 800);
    resize(img2, img2, 800);

    cvtColor( img1, img1, COLOR_RGBA2GRAY);
    cvtColor( img2, img2, COLOR_RGBA2GRAY);

    //declare input/output
    std::vector<KeyPoint> keypoints1, keypoints2;
    std::vector<std::vector<DMatch>> matches;

    UMat _descriptors1, _descriptors2;
    Mat descriptors1 = _descriptors1.getMat(ACCESS_RW),
            descriptors2 = _descriptors2.getMat(ACCESS_RW);

    //instantiate detectors/matchers
    ORBDetector orb;
    ORBMatcher matcher;

    orb(img2.getMat(ACCESS_READ), Mat(), keypoints1, descriptors1);
    orb(img1.getMat(ACCESS_READ), Mat(), keypoints2, descriptors2);
    if(keypoints1.size() == 0 || keypoints2.size() == 0) {
        return -1;
    }
    matcher(descriptors1, descriptors2, matches, 2);

    if(matches.size() == 0) {
        return -1;
    }
    __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                        "%d keypoints on object image", keypoints1.size());
    __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                        "%d keypoints on scene image", keypoints2.size());
    __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                        "%d matches on scene image", matches.size());

    std::vector<DMatch> good_matches;
    for (int i = 0; i < matches.size(); i++) {
        if(matches[i].size() == 2 && matches[i][0].distance <= matches[i][1].distance * RATIO) {
            good_matches.push_back(matches[i][0]);
        }
    }

    return good_matches.size();
}