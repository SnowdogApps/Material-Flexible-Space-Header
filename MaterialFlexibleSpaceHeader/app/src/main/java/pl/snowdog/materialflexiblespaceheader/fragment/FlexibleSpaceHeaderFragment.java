package pl.snowdog.materialflexiblespaceheader.fragment;


import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.nineoldandroids.view.ViewHelper;

import butterknife.ButterKnife;
import butterknife.InjectView;
import pl.snowdog.materialflexiblespaceheader.R;
import pl.snowdog.materialflexiblespaceheader.view.ObservableScrollViewWithFling;


/**
 * Fragment with Flexible Space Header
 * Created by chomi3 on 09.12.14.
 */
public class FlexibleSpaceHeaderFragment extends Fragment implements ObservableScrollViewCallbacks {
    public static final String TAG = "FlexibleSpaceHeaderFragment";

    @InjectView(R.id.observable_sv)
    ObservableScrollViewWithFling mScrollView;

    @InjectView(R.id.title)
    TextView mTitle; //Title used instead of Toolbar.title

    @InjectView(R.id.toolbar_view)
    Toolbar mToolbarView;

    @InjectView(R.id.ll_above_photo)
    protected LinearLayout llTintLayer; //Layout that we're tinting when scrolling

    @InjectView(R.id.fl_image)
    protected FrameLayout flImage; //Layout that hosts the header image

    private int mParallaxImageHeight;
    private int mScrollY = 0; //Keeps track of our scroll.
    private boolean mIsToolbarShown = true;
    private int mToolbarHeight;
    private boolean goingUp = false;

    private int mToolbarBackgroundColor;

    public FlexibleSpaceHeaderFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flexible_space_header, container, false);
        ButterKnife.inject(this, view);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Store flexible space height
        mParallaxImageHeight = getResources().getDimensionPixelSize(R.dimen.parallax_image_height);

        configureToolbarView();
        configureScrollView();
    }


    private void configureScrollView() {
        mScrollView.setScrollViewCallbacks(this);
        mScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        mScrollView.setOnFlingListener(new ObservableScrollViewWithFling.OnFlingListener() {
            @Override
            public void onFlingStarted() {
                if (goingUp && !mIsToolbarShown) {
                    showFullToolbar(50);
                }
            }

            @Override
            public void onFlingStopped() {

            }
        });

        ViewTreeObserver vto = mTitle.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    mTitle.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    mTitle.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                updateFlexibleSpaceText(0);
            }
        });
    }

    private void configureToolbarView() {
        ((ActionBarActivity) getActivity()).setSupportActionBar(mToolbarView);
        mToolbarView.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        mToolbarView.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().onBackPressed();
            }
        });

        //Remove toolbars title, as we have our own title implementation
        mToolbarView.post(new Runnable() {
            @Override
            public void run() {
                mToolbarView.setTitle("");

            }
        });

        mToolbarBackgroundColor = getResources().getColor(R.color.colorPrimary);
        TypedValue tv = new TypedValue();
        if (getActivity().getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
            mToolbarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
        setBackgroundAlpha(mToolbarView, 0.0f, mToolbarBackgroundColor);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
        //Store actual scroll state:
        if (mScrollY > scrollY) {
            goingUp = true;
        } else if (mScrollY < scrollY) {
            goingUp = false;
        }

        //If we're close to edge, show toolbar faster
        if (mScrollY - scrollY > 50 && !mIsToolbarShown) {
            showFullToolbar(50); //speed up
        } else if (mScrollY - scrollY > 0 && scrollY <= mParallaxImageHeight && !mIsToolbarShown) {
            showFullToolbar(250);
        }

        //Show or hide full toolbar color, so it will become visible over scrollable content:
        if (scrollY >= mParallaxImageHeight - mToolbarHeight) {
            setBackgroundAlpha(mToolbarView, 1, mToolbarBackgroundColor);
        } else {
            setBackgroundAlpha(mToolbarView, 0, mToolbarBackgroundColor);
        }

        //Translate flexible image in Y axis
        ViewHelper.setTranslationY(flImage, scrollY / 2);

        //Calculate flexible space alpha based on scroll state
        float alpha = 1 - (float) Math.max(0, mParallaxImageHeight - (mToolbarHeight) - scrollY) / (mParallaxImageHeight - (mToolbarHeight * 1.5f));
        setBackgroundAlpha(llTintLayer, alpha, mToolbarBackgroundColor);

        //Store last scroll state
        mScrollY = scrollY;

        //Move the flexible text
        updateFlexibleSpaceText((scrollY));
    }

    @Override
    public void onDownMotionEvent() {

    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {

        //If we're scrolling up, and are too far away from toolbar, hide it:
        if (scrollState == ScrollState.UP) {
            if (mScrollY > mParallaxImageHeight) {
                if (mIsToolbarShown) {
                    hideFullToolbar();
                }
            } else {
                // Don't hide toolbar yet
            }
        } else if (scrollState == ScrollState.DOWN) {
            //Show toolbar as fast as we're starting to scroll down
            if (!mIsToolbarShown) {
                showFullToolbar(250);
            }
        }
    }

    private void setBackgroundAlpha(View view, float alpha, int baseColor) {
        int a = Math.min(255, Math.max(0, (int) (alpha * 255))) << 24;
        int rgb = 0x00ffffff & baseColor;
        view.setBackgroundColor(a + rgb);
    }


    public void showFullToolbar(int duration) {
        mIsToolbarShown = true;

        final AnimatorSet animatorSet = buildAnimationSet(duration,
                buildAnimation(mToolbarView, -mToolbarHeight, 0),
                buildAnimation(mTitle, -mToolbarHeight, 0));

        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                updateFlexibleSpaceText(mScrollY); //dirty update fling-fix
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                updateFlexibleSpaceText(mScrollY); //dirty update fling-fix
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        animatorSet.start();

    }

    private ObjectAnimator buildAnimation(View view, float from, float to) {
        return ObjectAnimator
                .ofFloat(view, View.TRANSLATION_Y, from, to);
    }

    public void hideFullToolbar() {
        mIsToolbarShown = false;
        final AnimatorSet animatorSet = buildAnimationSet(250,
                buildAnimation(mToolbarView, 0, -mToolbarHeight),
                buildAnimation(mTitle, 0, -mToolbarHeight));
        animatorSet.start();
    }

    private AnimatorSet buildAnimationSet(int duration, ObjectAnimator... objectAnimators) {

        AnimatorSet a = new AnimatorSet();
        a.playTogether(objectAnimators);
        a.setInterpolator(AnimationUtils.loadInterpolator(getActivity(), android.R.interpolator.accelerate_decelerate));
        a.setDuration(duration);

        return a;
    }

    /**
     * Scale title view and move it in Flexible space
     * @param scrollY
     */
    private void updateFlexibleSpaceText(final int scrollY) {
        if (!mIsToolbarShown) return;

        int adjustedScrollY = scrollY;
        if (scrollY < 0) {
            adjustedScrollY = 0;
        } else if (scrollY > mParallaxImageHeight) {
            adjustedScrollY = mParallaxImageHeight;
        }

        float maxScale = 1.6f;
        float scale = maxScale * ((float) (mParallaxImageHeight - mToolbarHeight) - adjustedScrollY) / (mParallaxImageHeight - mToolbarHeight);
        if (scale < 0) {
            scale = 0;
        }

        ViewHelper.setPivotX(mTitle, 0);
        ViewHelper.setPivotY(mTitle, 0);
        ViewHelper.setScaleX(mTitle, 1 + scale);
        ViewHelper.setScaleY(mTitle, 1 + scale);

        int maxTitleTranslation = (int) (mParallaxImageHeight * 0.4f);
        int titleTranslation = (int) (maxTitleTranslation * ((float) scale / maxScale));
        ViewHelper.setTranslationY(mTitle, titleTranslation);
    }
}
