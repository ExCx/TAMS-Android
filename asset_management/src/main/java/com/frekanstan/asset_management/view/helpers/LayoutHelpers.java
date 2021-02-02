package com.frekanstan.asset_management.view.helpers;

import android.content.Context;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.frekanstan.asset_management.R;
import com.google.common.base.Strings;

import lombok.val;
import lombok.var;

public class LayoutHelpers {
    public static void setTextViewDrawableTint(TextView tw, int color) {
        for (val d : tw.getCompoundDrawablesRelative())
            if (d != null) {
                var wrappedDrawable = DrawableCompat.wrap(d);
                DrawableCompat.setTint(wrappedDrawable, color);
                tw.setCompoundDrawablesRelative(wrappedDrawable, null, null, null);
            }
    }

    public static void setTextWithDrawable(Context context, TextView tw, String text) {
        if (Strings.isNullOrEmpty(text)) {
            tw.setText("");
            setTextViewDrawableTint(tw, ContextCompat.getColor(context, R.color.red_700));
        }
        else {
            tw.setText(text);
            setTextViewDrawableTint(tw, ContextCompat.getColor(context, R.color.blue_700));
        }
    }
}
