/*
 * Licensed under GPL 3.0
 */

/*
 * Licensed under GPL 3.0
 */

/*
 * Licensed under GPL 3.0
 */

/*
 * Licensed under GPL 3.0
 */

/*
 * Licensed under GPL 3.0
 */

/*
 * Licensed under GPL 3.0
 */

package org.sasehash.burgerwp;

import android.graphics.Canvas;
import android.view.MotionEvent;

public interface IDrawable {
    /**
     * Draws the thing to the canvas (the canvas has to be writable)
     *
     * @param canvas
     */
    void draw(Canvas canvas);

    /*
     * @return true => draw(canvas) will do nothing
     */
    boolean canBeReplaced();

    void event(MotionEvent event);
}
