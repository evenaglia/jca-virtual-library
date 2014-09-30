package net.venaglia.common.util;

/**
 * User: ed
 * Date: 5/21/14
 * Time: 7:14 AM
 */
public class StringHelper {

    private StringHelper() {
        throw new UnsupportedOperationException("StringHelper is a pure static class");
    }

    public static char[] superscript(int count) {
        char[] chars = String.valueOf(count).toCharArray();
        return superscript(chars);
    }

    public static char[] superscript(char[] chars) {
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            switch (c) {
                case '-': c = '⁻'; break;
                case '0': c = '⁰'; break;
                case '1': c = '¹'; break;
                case '2': c = '²'; break;
                case '3': c = '³'; break;
                case '4': c = '⁴'; break;
                case '5': c = '⁵'; break;
                case '6': c = '⁶'; break;
                case '7': c = '⁷'; break;
                case '8': c = '⁸'; break;
                case '9': c = '⁹'; break;
            }
            chars[i] = c;
        }
        return chars;
    }

    public static char[] subscript(int count) {
        char[] chars = String.valueOf(count).toCharArray();
        return subscript(chars);
    }

    public static String subscript(String text) {
        return new String(subscript(text.toCharArray()));
    }

    public static char[] subscript(char[] chars) {
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            switch (c) {
                case '0': c = '₀'; break;
                case '1': c = '₁'; break;
                case '2': c = '₂'; break;
                case '3': c = '₃'; break;
                case '4': c = '₄'; break;
                case '5': c = '₅'; break;
                case '6': c = '₆'; break;
                case '7': c = '₇'; break;
                case '8': c = '₈'; break;
                case '9': c = '₉'; break;
            }
            chars[i] = c;
        }
        return chars;
    }
}
