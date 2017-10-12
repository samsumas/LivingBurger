/*
 * Licensed under GPL 3.0
 */

package org.sasehash.burgerwp;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * Created by sami on 08/10/17.
 */

public class ToDraw {
    private Bitmap texture;
    private long currentMovementTime;
    private long maxMovementTime;
    private boolean selfDestroy = false;
    private int bouncing;
    private Lambda xVec;
    private Lambda yVec;
    private Lambda rVec= new Lambda() {
        @Override
        public int l(long x) {
            return 0;
        }
    };
    private int xMultiplier = 1;
    private int yMultiplier = 1;
    private int speed;
    //omg this looks nice
    private Matrix m;
    private int x,y;
    private float rotation;
    private float scaler=1;


    public Matrix getM() {
        return m;
    }

    public void setM(Matrix m) {
        this.m = m;
    }

    public ToDraw(Bitmap texture, int x, int y, long currentMovementTime, long maxMovementTime, boolean selfDestroy, int bouncing, int speed, float rotation, float scaler) {
        this.texture = texture;
        this.m = new Matrix();
        this.scaler=scaler;
        this.rotation=rotation;
        this.addTo(rotation);
        m.setTranslate(x, y);
        this.x=x;
        this.y=y;
        this.currentMovementTime = currentMovementTime;
        this.maxMovementTime = maxMovementTime;
        this.selfDestroy = selfDestroy;
        this.bouncing = bouncing;
        this.speed = speed;
    }

    public ToDraw(ToDraw td) {
        this(td.getTexture(), td.getX(), td.getY(), td.getCurrentMovementTime(), td.getMaxMovementTime(), td.getSelfDestroy(), td.getBouncing(), td.getSpeed(), td.getRotation(),td.getScaler());
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public float getScaler() {
        return scaler;
    }

    public void setScaler(float scaler) {
        this.scaler = scaler;
    }

    public void setTranslateX(int x) {
        m.setTranslate(x, y);
        this.x=x;
    }

    public void setTranslateY(int y) {
        m.setTranslate(x, y);
        this.y=y;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getWidth() {
        return texture.getWidth();
    }

    public int getHeight() {
        return texture.getHeight();
    }

    public void bounceX() {
        xMultiplier *= -1;
    }

    public boolean isVecNull() {
        return xVec == null || yVec == null;
    }

    public void bounceY() {
        yMultiplier *= -1;
    }

    public int getxVec(long t) {
        return xMultiplier * xVec.l(t);
    }

    public float getrVec(long t) {
        return rVec.l(t);
    }

    public void setrVec(Lambda rVec) {
        this.rVec = rVec;
    }

    public void setxVec(Lambda xVec) {
        this.xVec = xVec;
    }

    public int getyVec(long t) {
        return yMultiplier * yVec.l(t);
    }

    public void setyVec(Lambda yVec) {
        this.yVec = yVec;
    }

    public int getBouncing() {
        return bouncing;
    }

    public void setBouncing(int bouncing) {
        this.bouncing = bouncing;
    }

    public boolean getSelfDestroy() {
        return selfDestroy;
    }

    public void setSelfDestroy(boolean selfdestroy) {
        this.selfDestroy = selfdestroy;
    }

    public boolean timeLeft() {
        return currentMovementTime < maxMovementTime;
    }

    public boolean survives() {
        return !selfDestroy || !timeLeft();
    }

    public boolean dies() {
        return (selfDestroy && !timeLeft());
    }

    public Bitmap getTexture() {
        return texture;
    }

    public void setTexture(Bitmap texture) {
        this.texture = texture;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        setTranslateX(x);
    }

    public void addTo(int x, int y) {
        m.postTranslate(x, y);
        this.x+=x;
        this.y+=y;
    }
    public int getMiddleX() {
        return (int) Math.round(texture.getWidth()*scaler/2);
    }
    public int getMiddleY() {
        return (int) Math.round(texture.getHeight()*scaler/2);
    }

    public void addTo(float rotation) {
        m.preRotate(rotation,getMiddleX(),getMiddleY());
        this.rotation+=rotation;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        setTranslateY(y);
    }

    public long getCurrentMovementTime() {
        return currentMovementTime;
    }

    public void setCurrentMovementTime(long currentMovementTime) {
        this.currentMovementTime = currentMovementTime;
    }

    public long getMaxMovementTime() {
        return maxMovementTime;
    }

    public void setMaxMovementTime(long maxMovementTime) {
        this.maxMovementTime = maxMovementTime;
    }
}
