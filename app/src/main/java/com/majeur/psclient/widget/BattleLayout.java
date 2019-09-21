package com.majeur.psclient.widget;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.majeur.psclient.model.Player;
import com.majeur.psclient.model.PokemonId;
import com.majeur.psclient.util.Utils;

import java.util.LinkedList;
import java.util.List;

public class BattleLayout extends ViewGroup {

    public static final int MODE_PREVIEW = 0;
    public static final int MODE_BATTLE_SINGLE = 1;
    public static final int MODE_BATTLE_DOUBLE = 2;
    public static final int MODE_BATTLE_TRIPLE = 3;

    private static final float ASPECT_RATIO = 16f/9f;
    private static final int PLAYER_1 = 4;
    private static final int PLAYER_2 = 5;

    private static final PointF[] REL_TEAMPREV_P1_LINE =
            {new PointF(0.125f, 0.728f), new PointF(0.820f, 0.806f)};
    private static final PointF[] REL_TEAMPREV_P2_LINE =
            {new PointF(0.180f, 0.267f), new PointF(0.875f, 0.344f)};

    private static final PointF[][] REL_BATTLE_P1_POS = {
        {new PointF(0.225f, 0.748f)},
        {new PointF(0.225f, 0.738f), new PointF(0.425f, 0.758f)},
        {new PointF(0.125f, 0.728f), new PointF(0.325f, 0.748f), new PointF(0.525f, 0.768f)}};
    private static final PointF[][] REL_BATTLE_P2_POS = {
            {new PointF(0.775f, 0.397f)},
            {new PointF(0.775f, 0.297f), new PointF(0.575f, 0.267f)},
            {new PointF(0.475f, 0.267f), new PointF(0.675f, 0.287f), new PointF(0.875f, 0.307f)}};

    private int mCurrentMode = MODE_BATTLE_SINGLE;
    private int mStatusBarOffset;

    private int mP1PreviewTeamSize = 6, mP2PreviewTeamSize = 6;

    private List<ImageView> mImageViewCache;
    private SparseArray<ImageView> mP1ImageViews;
    private SparseArray<StatusView> mP1StatusViews;
    private SparseArray<ToasterView> mP1ToasterViews;
    private SparseArray<ImageView> mP2ImageViews;
    private SparseArray<StatusView> mP2StatusViews;
    private SparseArray<ToasterView> mP2ToasterViews;
    private SideView mP1SideView;
    private SideView mP2SideView;

    public BattleLayout(Context context) {
        this(context, null);
    }

    public BattleLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BattleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mImageViewCache = new LinkedList<>();
        mP1ImageViews = new SparseArray<>();
        mP1StatusViews = new SparseArray<>();
        mP1ToasterViews = new SparseArray<>();
        mP1SideView = new SideView(context);
        mP2ImageViews = new SparseArray<>();
        mP2StatusViews = new SparseArray<>();
        mP2ToasterViews = new SparseArray<>();
        mP2SideView = new SideView(context);

        mStatusBarOffset = Utils.dpToPx(18);

        addView(mP1SideView);
        addView(mP2SideView);
        mP2SideView.setGravity(Gravity.END);
    }

    public void setMode(int currentMode) {
        mCurrentMode = currentMode;
        requestLayout();
    }

    public void setPreviewTeamSize(Player player, int teamSize) {
        if (player == Player.TRAINER) mP1PreviewTeamSize = teamSize;
        if (player == Player.FOE) mP2PreviewTeamSize = teamSize;
    }

    public ToasterView getToasterView(PokemonId id) {
        if (id.position < 0)
            return null;
        SparseArray<ToasterView> toasterViews = id.player == Player.TRAINER ? mP1ToasterViews : mP2ToasterViews;
        return toasterViews.get(id.position);
    }

    public StatusView getStatusView(PokemonId id) {
        if (id.position < 0)
            return null;
        SparseArray<StatusView> statusViews = id.player == Player.TRAINER ? mP1StatusViews : mP2StatusViews;
        return statusViews.get(id.position);
    }

    public ImageView getPokemonView(PokemonId id) {
        if (id.position < 0)
            return null;
        SparseArray<ImageView> imageViews = id.player == Player.TRAINER ? mP1ImageViews : mP2ImageViews;
        return imageViews.get(id.position);
    }

    public SideView getSideView(Player player) {
        SideView sideView = player == Player.TRAINER ? mP1SideView : mP2SideView;
        sideView.bringToFront();
        return sideView;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth, measuredHeight;

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode == MeasureSpec.UNSPECIFIED)
            throw new IllegalStateException("Fixed parent width required");
        measuredWidth = widthSize;

        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.EXACTLY)
            measuredHeight = heightSize;
        else if (heightMode == MeasureSpec.AT_MOST)
            measuredHeight = (int) Math.min(measuredWidth/ASPECT_RATIO, heightSize);
        else
            // Here heightMode is equal to MeasureSpec.UNSPECIFIED, we take all the space we want.
            measuredHeight = (int) (measuredWidth/ASPECT_RATIO);

        measureChildren(MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.AT_MOST));
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    private void measureChildInLayout(View child, int parentWidth, int parentHeight) {
        LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
        int widthMeasureSpec = getChildMeasureSpec(layoutParams.width, parentWidth, layoutParams.relWidth);
        int heightMeasureSpec = getChildMeasureSpec(layoutParams.height, parentHeight, layoutParams.relHeight);

        child.measure(widthMeasureSpec, heightMeasureSpec);
    }

    private int getChildMeasureSpec(int dim, int parentDim, float relDim) {
        int spec;
        if (dim == LayoutParams.WRAP_CONTENT)
            spec = MeasureSpec.makeMeasureSpec(parentDim, MeasureSpec.AT_MOST);
        else if (dim == LayoutParams.MATCH_PARENT)
            spec = MeasureSpec.makeMeasureSpec(parentDim, MeasureSpec.EXACTLY);
        else if (dim == LayoutParams.SIZE_RELATIVE)
            spec = MeasureSpec.makeMeasureSpec((int) (parentDim*relDim), MeasureSpec.EXACTLY);
        else
            spec = MeasureSpec.makeMeasureSpec(dim, MeasureSpec.EXACTLY);
        return spec;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;

        switch (mCurrentMode) {
            case MODE_PREVIEW:
                layoutPreviewMode(width, height);
                break;
            case MODE_BATTLE_SINGLE:
                layoutBattleMode(1, width, height);
                break;
            case MODE_BATTLE_DOUBLE:
                layoutBattleMode(2, width, height);
                break;
            case MODE_BATTLE_TRIPLE:
                layoutBattleMode(3, width, height);
                break;
        }
    }

    private void layoutPreviewMode(int width, int height) {
        int p1Count = mP1PreviewTeamSize;
        fillNeededViews(mP1ImageViews, p1Count, width, height);
        fillNeededStatusViews(mP1StatusViews, 0, width, height);
        fillNeededToasterViews(mP1ToasterViews, 0, width, height);
        Point startPoint = new Point((int) (REL_TEAMPREV_P1_LINE[0].x * width),
                (int) (REL_TEAMPREV_P1_LINE[0].y * height));
        Point endPoint = new Point((int) (REL_TEAMPREV_P1_LINE[1].x * width),
                (int) (REL_TEAMPREV_P1_LINE[1].y * height));
        int xStep = (endPoint.x - startPoint.x) / p1Count;
        int yStep = (endPoint.y - startPoint.y) / p1Count;
        for (int i = 0; i < p1Count; i++)
            layoutChild(mP1ImageViews.get(i), startPoint.x + i*xStep,
                    startPoint.y + i*yStep, Gravity.CENTER, false);

        int p2Count = mP2PreviewTeamSize;
        fillNeededViews(mP2ImageViews, p2Count, width, height);
        fillNeededStatusViews(mP2StatusViews, 0, width, height);
        fillNeededToasterViews(mP2ToasterViews, 0, width, height);
        startPoint = new Point((int) (REL_TEAMPREV_P2_LINE[0].x * width),
                (int) (REL_TEAMPREV_P2_LINE[0].y * height));
        endPoint = new Point((int) (REL_TEAMPREV_P2_LINE[1].x * width),
                (int) (REL_TEAMPREV_P2_LINE[1].y * height));
        xStep = (endPoint.x - startPoint.x) / p2Count;
        yStep = (endPoint.y - startPoint.y) / p2Count;
        for (int i = 0; i < p2Count; i++)
            layoutChild(mP2ImageViews.get(i), endPoint.x - i*xStep,
                    endPoint.y - i*yStep, Gravity.CENTER, false);
    }

    private void layoutChild(View child, int xIn, int yIn, int gravity, boolean fitInParent) {
        layoutChild(child, xIn, yIn, gravity, fitInParent, null);
    }

    private void layoutChild(View child, int xIn, int yIn, int gravity, boolean fitInParent, Point out) {
        int w = child.getMeasuredWidth();
        int h = child.getMeasuredHeight();
        int x = xIn;
        int y = yIn;

        if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.CENTER_HORIZONTAL) {
            x = xIn - w/2;
        }
        if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.CENTER_VERTICAL) {
            y = yIn - h/2;
        }
        if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
            y = yIn - h;
        }

        if (fitInParent) {
            if (x < 0)
                x = 0;
            if (y < 0)
                y = 0;
            if (x + w > getWidth())
                x = getWidth() - w;
            if (y + h > getHeight())
                y = getHeight() - h;
        }

        if (out != null) out.set(x, y);
        child.layout(x, y, x + w, y + h);
    }

    private ImageView obtainImageView() {
        if (mImageViewCache.size() > 0)
            return mImageViewCache.get(0);
        ImageView imageView = new ImageView(getContext());
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageView.setLayoutParams(layoutParams);
        return imageView;
    }

    private void layoutBattleMode(int count, int width, int height) {
        fillNeededViews(mP1ImageViews, count, width, height);
        fillNeededStatusViews(mP1StatusViews, count, width, height);
        fillNeededToasterViews(mP1ToasterViews, count, width, height);
        fillNeededViews(mP2ImageViews, count, width, height);
        fillNeededStatusViews(mP2StatusViews, count, width, height);
        fillNeededToasterViews(mP2ToasterViews, count, width, height);

        Point point = new Point();
        int j;
        for (int i = 0; i < count; i++) {
            point.set((int) (REL_BATTLE_P1_POS[count-1][i].x * width),
                    (int) (REL_BATTLE_P1_POS[count-1][i].y * height));
            int cX = point.x, cY = point.y;
            layoutChild(mP1ImageViews.get(i), cX, cY, Gravity.CENTER, false, point);
            layoutChild(mP1StatusViews.get(i), cX, point.y - mStatusBarOffset, Gravity.CENTER_HORIZONTAL, true);
            layoutChild(mP1ToasterViews.get(i), cX, cY, Gravity.CENTER, false);

            j = count - i - 1;
            point.set((int) (REL_BATTLE_P2_POS[count-1][j].x * width),
                    (int) (REL_BATTLE_P2_POS[count-1][j].y * height));
            cX = point.x;
            cY = point.y;
            layoutChild(mP2ImageViews.get(i), cX, cY, Gravity.CENTER, false, point);
            layoutChild(mP2StatusViews.get(i), cX, point.y - mStatusBarOffset, Gravity.CENTER_HORIZONTAL, true);
            layoutChild(mP2ToasterViews.get(i), cX, cY, Gravity.CENTER, false);
        }

        layoutChild(mP1SideView, 0, 4 * height / 5, Gravity.CENTER_VERTICAL, true);
        layoutChild(mP2SideView, width, 3 * height / 5, Gravity.CENTER_VERTICAL, true);
    }

    private void fillNeededViews(SparseArray<ImageView> pXImageViews, int needed, int width, int height) {
        final int current = pXImageViews.size();
        if (current < needed) {
            for (int i = current; i < needed; i++) {
                ImageView child = obtainImageView();
                child.setImageDrawable(null);
                pXImageViews.append(i, child);
                addViewInLayout(child, -1, child.getLayoutParams());
                measureChildInLayout(child, width, height);
            }
        } else if (current > needed) {
            for (int i = needed; i < current; i++) {
                ImageView child = pXImageViews.get(i);
                pXImageViews.remove(i);
                removeViewInLayout(child);
                mImageViewCache.add(child);
            }
        }
    }

    private void fillNeededStatusViews(SparseArray<StatusView> pXStatusViews, int needed, int width, int height) {
        final int current = pXStatusViews.size();
        if (current < needed) {
            for (int i = current; i < needed; i++) {
                StatusView child = new StatusView(getContext());
                child.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                pXStatusViews.append(i, child);
                addViewInLayout(child, -1, child.getLayoutParams());
                measureChildInLayout(child, width, height);
            }
        } else if (current > needed) {
            for (int i = needed; i < current; i++) {
                StatusView child = pXStatusViews.get(i);
                pXStatusViews.remove(i);
                removeViewInLayout(child);
            }
        }
    }

    private void fillNeededToasterViews(SparseArray<ToasterView> pXToasterViews, int needed, int width, int height) {
        final int current = pXToasterViews.size();
        if (current < needed) {
            for (int i = current; i < needed; i++) {
                ToasterView child = new ToasterView(getContext());
                child.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                pXToasterViews.append(i, child);
                addViewInLayout(child, -1, child.getLayoutParams());
                measureChildInLayout(child, width, height);
            }
        } else if (current > needed) {
            for (int i = needed; i < current; i++) {
                ToasterView child = pXToasterViews.get(i);
                pXToasterViews.remove(i);
                removeViewInLayout(child);
            }
        }
    }

    private static class LayoutParams extends ViewGroup.LayoutParams {

        public static final int SIZE_RELATIVE = -3;

        private float relWidth;
        private float relHeight;

        public LayoutParams(float relWidth, float relHeight) {
            super(SIZE_RELATIVE, SIZE_RELATIVE);
            this.relWidth = relWidth;
            this.relHeight = relHeight;
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }
    }
}
