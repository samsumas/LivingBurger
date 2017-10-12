/*
 * Licensed under GPL 3.0
 */

/*
 * Licensed under GPL 3.0
 */

package org.sasehash.burgerwp;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class ToDraw {
    private Bitmap texture;
    private long currentMovementTime;
    private long maxMovementTime;
    private boolean selfDestroy = false;
    private int bouncing;
    private Lambda xVec;
    private Lambda yVec;
    private Lambda rVec = new Lambda() {
        @Override
        public int l(long x) {
            return 0;
        }
    };
    private int xMultiplier = 1;
    private int yMultiplier = 1;
    private int speed;
    //omg this looks nice
    private Matrix manipulation;
    private int x, y;
    private float rotation;
    private float scaler = 1;


    public ToDraw(Bitmap texture, int x, int y, long currentMovementTime, long maxMovementTime, boolean selfDestroy, int bouncing, int speed, float rotation, float scaler) {
        this.texture = texture;
        this.manipulation = new Matrix();
        this.rotation = rotation;
        this.addTo(rotation);
        this.x = x;
        this.y = y;
        manipulation.setTranslate(x, y);
        this.currentMovementTime = currentMovementTime;
        this.maxMovementTime = maxMovementTime;
        this.selfDestroy = selfDestroy;
        this.bouncing = bouncing;
        this.speed = speed;
        this.scaler = scaler;
        manipulation.preScale(scaler, scaler);
    }

    public ToDraw(ToDraw td) {
        this(td.getTexture(), td.getX(), td.getY(), td.getCurrentMovementTime(), td.getMaxMovementTime(), td.getSelfDestroy(), td.getBouncing(), td.getSpeed(), td.getRotation(), td.getScaler());
    }

    public Matrix getManipulation() {
        return manipulation;
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        manipulation.setRotate(rotation);
        this.rotation = rotation;
    }

    public float getScaler() {
        return scaler;
    }

    public void scale(float scaler) {
        manipulation.preScale(scaler, scaler);
        this.scaler = scaler;
    }

    private void recreate(int x, int y) {
        manipulation.reset();
        manipulation.setTranslate(x, y);
        manipulation.preRotate(rotation, getMiddleX(), getMiddleY());
        manipulation.preScale(scaler, scaler);
    }

    public void setTranslateX(int x) {
        //warning : when setting the translation, it could be that the rotation gets lost
        //jep, the matrix implementation is slightly bugged
        recreate(x, y);
        this.x = x;
    }

    public void setTranslateY(int y) {
        recreate(x, y);
        this.y = y;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getWidth() {
        return Math.round(scaler * texture.getWidth());
    }

    public int getHeight() {
        return Math.round(scaler * texture.getHeight());
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
        int relative = x - this.x;
        //setTranslateX(x);
        manipulation.postTranslate(relative, 0);
        this.x = x;
    }

    public void addTo(int x, int y) {
        manipulation.postTranslate(x, y);
        this.x += x;
        this.y += y;
    }

    public int getMiddleX() {
        return Math.round(texture.getWidth() * scaler / 2);
    }

    public int getMiddleY() {
        return Math.round(texture.getHeight() * scaler / 2);
    }

    public void addTo(float rotation) {
        manipulation.preRotate(rotation, getMiddleX(), getMiddleY());
        this.rotation += rotation;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        int relative = y - this.y;
        //setTranslateY(y);
        manipulation.postTranslate(0, relative);
        this.y = y;
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

    public void resetMult() {
        xMultiplier = 1;
        yMultiplier = 1;
    }
}
