package com.aetherquorion.cleansuperai.ads.banner;

import static com.aetherquorion.cleansuperai.ads.employment.EventKt.DATA_CONSTANT_NATIVE_CONTENT;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.aetherquorion.cleansuperai.R;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.tencent.mmkv.MMKV;

public class BannerView extends FrameLayout {
    private int templateType;
    private CommonViewStyle styles;
    private NativeAd nativeAd;
    private NativeAdView nativeAdView;
    private TextView primaryView;
    private TextView secondaryView;
    private RatingBar ratingBar;
    private TextView tertiaryView;
    private ImageView iconView;
    private MediaView mediaView;
    private AppCompatButton callToActionView;
    private ConstraintLayout background;

    public BannerView(Context context) {
        super(context);
        initView(context, null);
    }

    public BannerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public BannerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    public void setStyles(CommonViewStyle styles) {
        this.styles = styles;
        applyStyles();
    }

    public NativeAdView getNativeAdView() {
        return nativeAdView;
    }

    private void applyStyles() {
        if (styles == null) return;
        Drawable mainBackground = styles.getMainBackgroundColor();
        if (mainBackground != null && background != null) {
            background.setBackground(mainBackground);
        }
        Typeface primary = styles.getPrimaryTextTypeface();
        if (primary != null && primaryView != null) primaryView.setTypeface(primary);
        Typeface secondary = styles.getSecondaryTextTypeface();
        if (secondary != null && secondaryView != null) secondaryView.setTypeface(secondary);
        Typeface tertiary = styles.getTertiaryTextTypeface();
        if (tertiary != null && tertiaryView != null) tertiaryView.setTypeface(tertiary);
        Typeface ctaTypeface = styles.getCallToActionTextTypeface();
        if (ctaTypeface != null && callToActionView != null) callToActionView.setTypeface(ctaTypeface);
        if (styles.getPrimaryTextTypefaceColor() > 0 && primaryView != null) primaryView.setTextColor(styles.getPrimaryTextTypefaceColor());
        if (styles.getSecondaryTextTypefaceColor() > 0 && secondaryView != null) secondaryView.setTextColor(styles.getSecondaryTextTypefaceColor());
        if (styles.getTertiaryTextTypefaceColor() > 0 && tertiaryView != null) tertiaryView.setTextColor(styles.getTertiaryTextTypefaceColor());
        if (styles.getCallToActionTypefaceColor() > 0 && callToActionView != null) callToActionView.setTextColor(styles.getCallToActionTypefaceColor());
        if (styles.getCallToActionTextSize() > 0 && callToActionView != null) callToActionView.setTextSize(styles.getCallToActionTextSize());
        if (styles.getPrimaryTextSize() > 0 && primaryView != null) primaryView.setTextSize(styles.getPrimaryTextSize());
        if (styles.getSecondaryTextSize() > 0 && secondaryView != null) secondaryView.setTextSize(styles.getSecondaryTextSize());
        if (styles.getTertiaryTextSize() > 0 && tertiaryView != null) tertiaryView.setTextSize(styles.getTertiaryTextSize());
        Drawable ctaBackground = styles.getCallToActionBackgroundColor();
        if (ctaBackground != null && callToActionView != null) callToActionView.setBackground(ctaBackground);
        Drawable primaryBackground = styles.getPrimaryTextBackgroundColor();
        if (primaryBackground != null && primaryView != null) primaryView.setBackground(primaryBackground);
        Drawable secondaryBackground = styles.getSecondaryTextBackgroundColor();
        if (secondaryBackground != null && secondaryView != null) secondaryView.setBackground(secondaryBackground);
        Drawable tertiaryBackground = styles.getTertiaryTextBackgroundColor();
        if (tertiaryBackground != null && tertiaryView != null) tertiaryView.setBackground(tertiaryBackground);
        invalidate();
        requestLayout();
    }

    private boolean adHasOnlyStore(NativeAd nativeAd) {
        String store = nativeAd.getStore();
        String advertiser = nativeAd.getAdvertiser();
        return !TextUtils.isEmpty(store) && TextUtils.isEmpty(advertiser);
    }

    public void setNativeAd(NativeAd nativeAd) {
        if (nativeAdView == null) {
            bindViews();
        }
        if (nativeAdView == null || nativeAd == null) return;

        this.nativeAd = nativeAd;
        String store = nativeAd.getStore();
        String advertiser = nativeAd.getAdvertiser();
        String headline = nativeAd.getHeadline();
        String body = nativeAd.getBody();
        String cta = nativeAd.getCallToAction();
        Double starRating = nativeAd.getStarRating();
        NativeAd.Image icon = nativeAd.getIcon();

        String secondaryText;
        if (callToActionView != null) nativeAdView.setCallToActionView(callToActionView);
        if (primaryView != null) nativeAdView.setHeadlineView(primaryView);
        if (mediaView != null) nativeAdView.setMediaView(mediaView);
        if (secondaryView != null) {
            secondaryView.setVisibility(VISIBLE);
            if (adHasOnlyStore(nativeAd)) {
                nativeAdView.setStoreView(secondaryView);
                secondaryText = store;
            } else if (!TextUtils.isEmpty(advertiser)) {
                nativeAdView.setAdvertiserView(secondaryView);
                secondaryText = advertiser;
            } else {
                secondaryText = "";
            }
        } else {
            secondaryText = "";
        }

        if (primaryView != null) primaryView.setText(headline);
        if (callToActionView != null) {
            String currentCta = MMKV.defaultMMKV().getString(DATA_CONSTANT_NATIVE_CONTENT, "");
            callToActionView.setText(TextUtils.isEmpty(currentCta) ? cta : currentCta);
        }
        if (starRating != null && starRating > 0 && ratingBar != null) {
            if (secondaryView != null) secondaryView.setVisibility(GONE);
            ratingBar.setRating(starRating.floatValue());
            ratingBar.setVisibility(INVISIBLE);
            nativeAdView.setStarRatingView(ratingBar);
        } else {
            if (secondaryView != null) {
                secondaryView.setText(secondaryText);
                secondaryView.setVisibility(VISIBLE);
            }
            if (ratingBar != null) ratingBar.setVisibility(INVISIBLE);
        }
        if (iconView != null && icon != null) {
            iconView.setVisibility(VISIBLE);
            iconView.setImageDrawable(icon.getDrawable());
        } else if (iconView != null) {
            iconView.setVisibility(GONE);
        }
        if (tertiaryView != null) {
            tertiaryView.setText(body);
            nativeAdView.setBodyView(tertiaryView);
        }
        nativeAdView.setNativeAd(nativeAd);
    }

    public void destroyNativeAd() {
        if (nativeAd != null) nativeAd.destroy();
    }

    private void initView(Context context, AttributeSet attributeSet) {
        TypedArray attributes = context.getTheme().obtainStyledAttributes(attributeSet, R.styleable.TemplateView, 0, 0);
        try {
            templateType = attributes.getResourceId(R.styleable.TemplateView_gnt_template_type, R.layout.view_native);
        } finally {
            attributes.recycle();
        }
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(templateType, this);
        bindViews();
        applyStyles();
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        bindViews();
    }

    private void bindViews() {
        nativeAdView = findViewById(R.id.native_ad_view);
        primaryView = findViewById(R.id.primary);
        secondaryView = findViewById(R.id.secondary);
        tertiaryView = findViewById(R.id.body);
        ratingBar = findViewById(R.id.rating_bar);
        if (ratingBar != null) {
            ratingBar.setEnabled(false);
            ratingBar.setVisibility(INVISIBLE);
        }
        iconView = findViewById(R.id.icon);
        callToActionView = findViewById(R.id.cta);
        mediaView = findViewById(R.id.media_view);
        background = findViewById(R.id.background);
    }
}
