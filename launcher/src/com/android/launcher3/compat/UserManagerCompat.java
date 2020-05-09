/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.compat;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.util.List;

public abstract class UserManagerCompat {
    protected UserManagerCompat() {
    }

    private static final Object sInstanceLock = new Object();
    private static UserManagerCompat sInstance;

    public static UserManagerCompat getInstance(Context context) {
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                sInstance = new UserManagerCompatVA();
            }
            return sInstance;
        }
    }

    public Drawable getUserBadgedIcon(Drawable icon, UserHandle user) {
        int vuserId = UserManagerCompat.toUserId(user);
        if (vuserId == 0) {
            return generatorNumIcon(icon, true, "1");
        } else {
            return generatorNumIcon(icon, true, String.valueOf(vuserId));
        }
    }

    /**
     * Creates a cache for users.
     */
    public abstract void enableAndResetCache();

    public abstract List<UserHandle> getUserProfiles();
    public abstract long getSerialNumberForUser(UserHandle user);
    public abstract UserHandle getUserForSerialNumber(long serialNumber);
    public abstract CharSequence getBadgedLabelForUser(CharSequence label, UserHandle user);
    public abstract long getUserCreationTime(UserHandle user);
    public abstract boolean isQuietModeEnabled(UserHandle user);
    public abstract boolean isUserUnlocked(UserHandle user);

    public abstract boolean isDemoUser();

    public static UserHandle fromUserId(int userId) {
        return mirror.android.os.UserHandle.ctor.newInstance(userId);
    }

    public static int toUserId(UserHandle user) {
        if (user == null) {
            return 0;
        }
        return mirror.android.os.UserHandle.getIdentifier.call(user);
    }

    /***
     *
     * 生成有数字的图片(没有边框)
     * @param isShowNum 是否要绘制数字
     * @param num 数字字符串：整型数字 超过99，显示为"99+"
     * @return
     */
    public static Drawable generatorNumIcon(Drawable icon, boolean isShowNum, String num) {

        DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
        //基准屏幕密度
        float baseDensity = 1.5f;//240dpi
        float factor = dm.density/baseDensity;

        // 初始化画布
        int width = icon.getIntrinsicWidth();
        int height = icon.getIntrinsicHeight();
        int iconSize = width;
        Bitmap numIcon = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(numIcon);

        // 拷贝图片
        Paint iconPaint = new Paint();
        iconPaint.setDither(true);// 防抖动
        iconPaint.setFilterBitmap(true);// 用来对Bitmap进行滤波处理，这样，当你选择Drawable时，会有抗锯齿的效果
        Rect src = new Rect(0, 0, width, height);
        Rect dst = new Rect(0, 0, width, height);

        // canvas.drawBitmap(icon, src, dst, iconPaint);

        if(isShowNum){

            if(TextUtils.isEmpty(num)){
                num = "0";
            }

            if(!TextUtils.isDigitsOnly(num)){
                //非数字
                num = "0";
            }

            int numInt = Integer.valueOf(num);

            if(numInt > 99){//超过99

                num = "99+";

                // 启用抗锯齿和使用设备的文本字体大小
                Paint numPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
                numPaint.setColor(Color.WHITE);
                numPaint.setTextSize(20f*factor);
                numPaint.setTypeface(Typeface.DEFAULT_BOLD);
                int textWidth=(int)numPaint.measureText(num, 0, num.length());

                int circleCenter = (int) (15*factor);//中心坐标
                int circleRadius = (int) (13*factor);//圆的半径

                //绘制左边的圆形
                Paint leftCirPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                leftCirPaint.setColor(Color.RED);
                canvas.drawCircle(iconSize-circleRadius-textWidth+(10*factor), circleCenter, circleRadius, leftCirPaint);

                //绘制右边的圆形
                Paint rightCirPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                rightCirPaint.setColor(Color.RED);
                canvas.drawCircle(iconSize-circleRadius, circleCenter, circleRadius, rightCirPaint);

                //绘制中间的距形
                Paint rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                rectPaint.setColor(Color.RED);
                RectF oval = new RectF(iconSize-circleRadius-textWidth+(10*factor), 2*factor, iconSize-circleRadius, circleRadius*2+2*factor);
                canvas.drawRect(oval, rectPaint);

                //绘制数字
                canvas.drawText(num, (float)(iconSize-textWidth/2-(24*factor)), 23*factor,        numPaint);

            }else{//<=99

                // 启用抗锯齿和使用设备的文本字体大小
                Paint numPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
                numPaint.setColor(Color.WHITE);
                numPaint.setTextSize(20f*factor);
                numPaint.setTypeface(Typeface.DEFAULT_BOLD);
                int textWidth=(int)numPaint.measureText(num, 0, num.length());

                //绘制外面的圆形
                //Paint outCirPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                //outCirPaint.setColor(Color.WHITE);
                //canvas.drawCircle(iconSize - 15, 15, 15, outCirPaint);

                //绘制内部的圆形
                Paint inCirPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                inCirPaint.setColor(Color.RED);
                canvas.drawCircle(iconSize-15*factor, 15*factor, 15*factor, inCirPaint);

                //绘制数字
                canvas.drawText(num, (float)(iconSize-textWidth/2-15*factor), 22*factor, numPaint);
            }
        }
        icon.draw(canvas);
        return icon;
    }
}
