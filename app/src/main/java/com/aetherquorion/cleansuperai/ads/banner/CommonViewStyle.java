package com.aetherquorion.cleansuperai.ads.banner;

import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;

public class CommonViewStyle {
    private Typeface callToActionTextTypeface;
    private float callToActionTextSize;
    private int callToActionTypefaceColor;
    private ColorDrawable callToActionBackgroundColor;
    private Typeface primaryTextTypeface;
    private float primaryTextSize;
    private int primaryTextTypefaceColor;
    private ColorDrawable primaryTextBackgroundColor;
    private Typeface secondaryTextTypeface;
    private float secondaryTextSize;
    private int secondaryTextTypefaceColor;
    private ColorDrawable secondaryTextBackgroundColor;
    private Typeface tertiaryTextTypeface;
    private float tertiaryTextSize;
    private int tertiaryTextTypefaceColor;
    private ColorDrawable tertiaryTextBackgroundColor;
    private ColorDrawable mainBackgroundColor;

    public Typeface getCallToActionTextTypeface() { return callToActionTextTypeface; }
    public float getCallToActionTextSize() { return callToActionTextSize; }
    public int getCallToActionTypefaceColor() { return callToActionTypefaceColor; }
    public ColorDrawable getCallToActionBackgroundColor() { return callToActionBackgroundColor; }
    public Typeface getPrimaryTextTypeface() { return primaryTextTypeface; }
    public float getPrimaryTextSize() { return primaryTextSize; }
    public int getPrimaryTextTypefaceColor() { return primaryTextTypefaceColor; }
    public ColorDrawable getPrimaryTextBackgroundColor() { return primaryTextBackgroundColor; }
    public Typeface getSecondaryTextTypeface() { return secondaryTextTypeface; }
    public float getSecondaryTextSize() { return secondaryTextSize; }
    public int getSecondaryTextTypefaceColor() { return secondaryTextTypefaceColor; }
    public ColorDrawable getSecondaryTextBackgroundColor() { return secondaryTextBackgroundColor; }
    public Typeface getTertiaryTextTypeface() { return tertiaryTextTypeface; }
    public float getTertiaryTextSize() { return tertiaryTextSize; }
    public int getTertiaryTextTypefaceColor() { return tertiaryTextTypefaceColor; }
    public ColorDrawable getTertiaryTextBackgroundColor() { return tertiaryTextBackgroundColor; }
    public ColorDrawable getMainBackgroundColor() { return mainBackgroundColor; }

    public static class Builder {
        private final CommonViewStyle styles = new CommonViewStyle();
        public Builder withCallToActionTextTypeface(Typeface value) { styles.callToActionTextTypeface = value; return this; }
        public Builder withCallToActionTextSize(float value) { styles.callToActionTextSize = value; return this; }
        public Builder withCallToActionTypefaceColor(int value) { styles.callToActionTypefaceColor = value; return this; }
        public Builder withCallToActionBackgroundColor(ColorDrawable value) { styles.callToActionBackgroundColor = value; return this; }
        public Builder withPrimaryTextTypeface(Typeface value) { styles.primaryTextTypeface = value; return this; }
        public Builder withPrimaryTextSize(float value) { styles.primaryTextSize = value; return this; }
        public Builder withPrimaryTextTypefaceColor(int value) { styles.primaryTextTypefaceColor = value; return this; }
        public Builder withPrimaryTextBackgroundColor(ColorDrawable value) { styles.primaryTextBackgroundColor = value; return this; }
        public Builder withSecondaryTextTypeface(Typeface value) { styles.secondaryTextTypeface = value; return this; }
        public Builder withSecondaryTextSize(float value) { styles.secondaryTextSize = value; return this; }
        public Builder withSecondaryTextTypefaceColor(int value) { styles.secondaryTextTypefaceColor = value; return this; }
        public Builder withSecondaryTextBackgroundColor(ColorDrawable value) { styles.secondaryTextBackgroundColor = value; return this; }
        public Builder withTertiaryTextTypeface(Typeface value) { styles.tertiaryTextTypeface = value; return this; }
        public Builder withTertiaryTextSize(float value) { styles.tertiaryTextSize = value; return this; }
        public Builder withTertiaryTextTypefaceColor(int value) { styles.tertiaryTextTypefaceColor = value; return this; }
        public Builder withTertiaryTextBackgroundColor(ColorDrawable value) { styles.tertiaryTextBackgroundColor = value; return this; }
        public Builder withMainBackgroundColor(ColorDrawable value) { styles.mainBackgroundColor = value; return this; }
        public CommonViewStyle build() { return styles; }
    }
}
