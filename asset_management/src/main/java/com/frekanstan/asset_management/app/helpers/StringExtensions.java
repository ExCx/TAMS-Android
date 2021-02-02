package com.frekanstan.asset_management.app.helpers;

import com.google.common.base.Strings;

public class StringExtensions
{
    public static String latinize(String str) {
        if (str == null)
            return "";
        str = str.replace('İ', 'I');
        str = str.replace('Ğ', 'G');
        str = str.replace('Ş', 'S');
        str = str.replace('Ö', 'O');
        str = str.replace('Ç', 'C');
        str = str.replace('Ü', 'U');
        str = str.trim();
        return str;
    }

    public static String makePrintable(String str, Integer maxLength) {
        if (str == null || str.equals(""))
            return " ";
        str = str.replace('İ', 'I');
        str = str.replace('Ğ', 'G');
        str = str.replace('Ş', 'S');
        str = str.replace('Ö', 'O');
        str = str.replace('Ç', 'C');
        str = str.replace('Ü', 'U');
        str = str.trim();
        if (maxLength != null && str.length() > maxLength)
            str = str.substring(0, maxLength - 1);
        return str;
    }

    public static String shorten(String str, Integer maxLength) {
        if (str == null || str.equals(""))
            return "";
        str = str.trim();
        if (maxLength != null && str.length() > maxLength)
            str = str.substring(0, maxLength - 1);
        return str;
    }

    public static boolean sFilter(String string, String query) {
        if (Strings.isNullOrEmpty(string))
            return false;
        return latinize(string).startsWith(query) ||
                latinize(string).contains(" " + query);
    }
}