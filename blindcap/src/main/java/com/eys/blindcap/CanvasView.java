package com.eys.blindcap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by nestorrubiogarcia on 09/04/2016.
 */
public class CanvasView extends View {

    ArrayList<Long> lapTimes = new ArrayList<Long>();
    int page;

    Context context;
    private Path mPath;
    private Paint paintGreyStroke;
    private Paint paintGreyText;
    private Paint paintBlueStroke;

    private final float heightMin = toPx(23);
    private final float heightMax = toPx(90);
    private final float heightRange = heightMax - heightMin;


    public CanvasView(Context c, AttributeSet attrs) {
        super(c, attrs);
        context = c;

        mPath = new Path();

        paintGreyStroke = new Paint();
        paintGreyStroke.setAntiAlias(true);
        paintGreyStroke.setColor(0xff5e5e5e);
        paintGreyStroke.setStyle(Paint.Style.STROKE);
        paintGreyStroke.setStrokeJoin(Paint.Join.ROUND);
        paintGreyStroke.setStrokeWidth(0f);

        paintBlueStroke = new Paint();
        paintBlueStroke.setAntiAlias(true);
        paintBlueStroke.setColor(0xff51E5DA);
        paintBlueStroke.setStyle(Paint.Style.STROKE);
        paintBlueStroke.setStrokeJoin(Paint.Join.ROUND);
        paintBlueStroke.setStrokeWidth(5f);

        paintGreyText = new Paint();
        paintGreyText.setAntiAlias(true);
        paintGreyText.setColor(0xff5e5e5e);
        paintGreyText.setStyle(Paint.Style.FILL);
        paintGreyText.setTextSize(toPx(8));
    }


    public void setData(LapDataList lapsData, int page) {
        lapTimes.clear();


        this.page = page;
        int numLaps = lapsData.size();
        int numItems = 8;

        for (int i = numItems * page ; i < numItems * (page + 1) ; i++) {
            if (i >= numLaps) { break; }
            this.lapTimes.add( lapsData.get(i).getMillis() );
        }

        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int numLaps = lapTimes.size();

        if (numLaps == 0) {
            return;
        }

        // get best/worst times
        long worstTime = 0;
        long bestTime = Integer.MAX_VALUE;
        for (long time : lapTimes) {
            bestTime = Math.min(time, bestTime);
            worstTime = Math.max(time, worstTime);
        }

        final float canvasWidth = getWidth();
        final float canvasHeight = getHeight();

        final float timeMin = bestTime;
        final float timeMax = worstTime;
        final float timeRange = timeMax - timeMin;

        final float stepWidth = canvasWidth / numLaps;

        final float textY = canvasHeight - toPx(8);
        final float textOffset = stepWidth / 2 + toPx(6);

        // start path
        mPath.moveTo(0, canvasHeight-heightMin);
        mPath.lineTo(0, canvasHeight-heightMin);

        for (int i = 0; i < numLaps; i++) {
            float time = (float) lapTimes.get(i);

            // map time to line height
            float lineHeight = heightMin + ((time - timeMin) / timeRange) * heightRange;

            float x = (i + 1) * stepWidth;
            float y0 = canvasHeight;
            float y1 = canvasHeight - lineHeight;

            // draw vertical line
            canvas.drawLine(x, y0, x, y1, paintGreyStroke);

            // draw text
            float textX = x - textOffset;
            canvas.drawText("L"+String.valueOf((8*page) + i + 1), textX, textY, paintGreyText);

            // add point to path
            mPath.lineTo(x, y1);
        }

        canvas.drawPath(mPath, paintBlueStroke);
    }

    public void clearCanvas() {
        mPath.reset();
//        invalidate();
    }


    private int toPx(int dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale);
    }
}