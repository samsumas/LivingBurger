/*
 * Licensed under GPL 3.0
 */

package org.sasehash.burgerwp;

import android.graphics.Bitmap;

/**
 * Created by sami on 08/10/17.
 */

public class ToDraw {
    private Bitmap texture;
    private int x, y;
    private long currentMovementTime;
    private long maxMovementTime;
    private boolean selfDestroy = false;
    private int bouncing;
    private Lambda xVec;
    private Lambda yVec;
    private int xMultiplier=1;
    private int yMultiplier=1;
    private float rotation=45;

    public ToDraw(Bitmap texture, int x, int y, long currentMovementTime, long maxMovementTime, boolean selfDestroy, int bouncing) {
        this.texture = texture;
        this.x = x;
        this.y = y;
        this.currentMovementTime = currentMovementTime;
        this.maxMovementTime = maxMovementTime;
        this.selfDestroy = selfDestroy;
        this.bouncing = bouncing;
    }
    public ToDraw(ToDraw td) {
        this(td.getTexture(),td.getX(),td.getY(),td.getCurrentMovementTime(),td.getMaxMovementTime(),td.getSelfDestroy(),td.getBouncing());
    }

    public float getRotation() {
        return rotation;
    }

    public int getWidth() {
        return texture.getWidth();
    }
    public int getHeight() {
        return texture.getHeight();
    }

    public void bounceX() {
        xMultiplier *=-1;
    }
    public boolean isVecNull() {
        return xVec == null || yVec == null;
    }
    public void resetMultipliers() {
        xMultiplier=1;
        yMultiplier=1;
    }
    public void bounceY() {
        yMultiplier *=-1;
    }
    public int getxVec(long t) {
        return xMultiplier*xVec.l(t);
    }
    public void setxVec(Lambda xVec) {
        this.xVec = xVec;
    }
    public int getyVec(long t) {
        return yMultiplier*yVec.l(t);
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
        this.x = x;
    }

    public void addToX(int x) {
        this.x += x;
    }

    public void addToY(int y) {
        this.y += y;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
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
}
