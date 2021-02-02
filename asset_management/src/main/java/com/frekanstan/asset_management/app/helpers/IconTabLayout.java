package com.frekanstan.asset_management.app.helpers;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Objects;

import lombok.val;

public class IconTabLayout extends TabLayout {
    public IconTabLayout(@NonNull Context context) {
        super(context);
    }

    public IconTabLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public IconTabLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setupWithViewPagerAndKeepIcons(ViewPager viewPager) {
        val icons = new ArrayList<Drawable>();
        for (int i = 0; i < getTabCount(); i++)
            icons.add(Objects.requireNonNull(getTabAt(i)).getIcon());
        val titles = new ArrayList<CharSequence>();
        for (int i = 0; i < getTabCount(); i++)
            titles.add(Objects.requireNonNull(getTabAt(i)).getText());
        setupWithViewPager(viewPager);

        for (int i = 0; i < getTabCount(); i++) {
            Objects.requireNonNull(getTabAt(i)).setText(titles.get(i));
            Objects.requireNonNull(getTabAt(i)).setIcon(icons.get(i));
        }
    }
}