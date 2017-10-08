package org.sasehash.burgerwp;

import android.graphics.Bitmap;

/**
 * Created by sami on 08/10/17.
 */

public class ToDraw {
    private Bitmap texture;
    private int x, y;
    private Movement mov;
    private long currentMovementTime= 0;
    private long maxMovementTime = 50;

    public ToDraw(Bitmap texture, int x, int y, Movement mov, long currentMovementTime, long maxMovementTime) {
        this.texture = texture;
        this.x = x;
        this.y = y;
        this.mov = mov;
        this.currentMovementTime = currentMovementTime;
        this.maxMovementTime = maxMovementTime;
    }

    public void move(long t) {
        long dt = t+currentMovementTime;
        if (dt > maxMovementTime || mov==null) {
            mov=null;
            return;
        }
        x+=mov.moveX(t);
        y+=mov.moveY(t);
        currentMovementTime = dt;
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

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Movement getMov() {
        return mov;
    }

    public void setMov(Movement mov) {
        this.mov = mov;
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
