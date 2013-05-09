package com.jivesoftware.jcalibrary.objects;

import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.texture.TextureLoadException;
import net.venaglia.realms.common.util.debug.OutputGraph;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * User: ed
 * Date: 5/2/13
 * Time: 4:01 PM
 */
public class GlyphTextureImageSource implements Callable<BufferedImage> {

    private static final String FONT_AWESOME_RESOURCE = "fonts/fontawesome-webfont.ttf";

    public static final char[] FONT_AWESOME_CHARACTERS = new char[] {
        '\uf000','\uf001','\uf002','\uf003','\uf004','\uf005','\uf006','\uf007','\uf008','\uf009','\uf00a','\uf00b','\uf00c','\uf00d','\uf00e',
        '\uf010','\uf011','\uf012','\uf013','\uf014','\uf015','\uf016','\uf017','\uf018','\uf019','\uf01a','\uf01b','\uf01c','\uf01d','\uf01e',
        '\uf021','\uf022','\uf023','\uf024','\uf025','\uf026','\uf027','\uf028','\uf029','\uf02a','\uf02b','\uf02c','\uf02d','\uf02e','\uf02f',
        '\uf030','\uf031','\uf032','\uf033','\uf034','\uf035','\uf036','\uf037','\uf038','\uf039','\uf03a','\uf03b','\uf03c','\uf03d','\uf03e',
        '\uf040','\uf041','\uf042','\uf043','\uf044','\uf045','\uf046','\uf047','\uf048','\uf049','\uf04a','\uf04b','\uf04c','\uf04d','\uf04e',
        '\uf050','\uf051','\uf052','\uf053','\uf054','\uf055','\uf056','\uf057','\uf058','\uf059','\uf05a','\uf05b','\uf05c','\uf05d','\uf05e',
        '\uf060','\uf061','\uf062','\uf063','\uf064','\uf065','\uf066','\uf067','\uf068','\uf069','\uf06a','\uf06b','\uf06c','\uf06d','\uf06e',
        '\uf070','\uf071','\uf072','\uf073','\uf074','\uf075','\uf076','\uf077','\uf078','\uf079','\uf07a','\uf07b','\uf07c','\uf07d','\uf07e',
        '\uf080','\uf083','\uf084','\uf085','\uf086','\uf087','\uf088','\uf089','\uf08a','\uf08b','\uf08d','\uf08e',
        '\uf090','\uf091','\uf093','\uf094','\uf095','\uf096','\uf097','\uf098','\uf09c','\uf09d','\uf09e',
        '\uf0a0','\uf0a1','\uf0a2','\uf0a3','\uf0a4','\uf0a5','\uf0a6','\uf0a7','\uf0a8','\uf0a9','\uf0aa','\uf0ab','\uf0ac','\uf0ad','\uf0ae',
        '\uf0b0','\uf0b1','\uf0b2',
        '\uf0c0','\uf0c1','\uf0c2','\uf0c3','\uf0c4','\uf0c5','\uf0c6','\uf0c7','\uf0c8','\uf0c9','\uf0ca','\uf0cb','\uf0cc','\uf0cd','\uf0ce',
        '\uf0d0','\uf0d1','\uf0d6','\uf0d7','\uf0d8','\uf0d9','\uf0da','\uf0db','\uf0dc','\uf0dd','\uf0de',
        '\uf0e0','\uf0e2','\uf0e3','\uf0e4','\uf0e5','\uf0e6','\uf0e7','\uf0e8','\uf0e9','\uf0ea','\uf0eb','\uf0ec','\uf0ed','\uf0ee',
        '\uf0f0','\uf0f1','\uf0f2','\uf0f3','\uf0f4','\uf0f5','\uf0f6','\uf0f7','\uf0f8','\uf0f9','\uf0fa','\uf0fb','\uf0fc','\uf0fd','\uf0fe',
        '\uf100','\uf101','\uf102','\uf103','\uf104','\uf105','\uf106','\uf107','\uf108','\uf109','\uf10a','\uf10b','\uf10c','\uf10d','\uf10e',
        '\uf110','\uf111','\uf112','\uf114','\uf115'
    };

    private final DetailLevel detailLevel;

    public GlyphTextureImageSource(DetailLevel detailLevel) {
        this.detailLevel = detailLevel;
    }

    public BufferedImage call() throws Exception {
        final URL url = Thread.currentThread().getContextClassLoader().getResource(FONT_AWESOME_RESOURCE);
        if (url == null) {
            throw new TextureLoadException("unable to resolve classpath resource: " + FONT_AWESOME_RESOURCE);
        }

        int size = detailLevel.steps * 4;
        Font font = Font.createFont(Font.TRUETYPE_FONT, url.openStream()).deriveFont(Font.PLAIN, size * 0.75f * 3.0f);
        BufferedImage temp = new BufferedImage(3 * size, 4 * size, BufferedImage.TYPE_4BYTE_ABGR);
        BufferedImage image = new BufferedImage(16 * size, 16 * size, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics graphics = image.getGraphics();
        graphics.setColor(Color.BLACK);
        ((Graphics2D)graphics).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        int drawX = temp.getWidth() >> 1;
        int drawY = Math.round((size * 3) * 0.75f);
        int k = 0;
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++) {
                if (FONT_AWESOME_CHARACTERS.length <= k) {
                    break;
                }
                char c = FONT_AWESOME_CHARACTERS[k++];
                if (!font.canDisplay(c)) {
                    continue;
                }
                drawCharacter(temp, font, drawX, drawY, c);
                int x = Math.round(size * i);
                int y = Math.round(size * j);
                graphics.drawImage(temp, x, y, x + size, Math.round(size * j + size * 1.333f), 0, 0, size * 3, size * 4, null);
            }
        }
        return image;
    }

    private void drawCharacter(BufferedImage temp, Font font, int drawX, int drawY, char c) {
        String str = String.valueOf(c);
        Graphics graphics = temp.getGraphics();
//        ((Graphics2D)graphics).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        ((Graphics2D)graphics).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(font);
        FontMetrics fontMetrics = graphics.getFontMetrics();
        int w = fontMetrics.stringWidth(str) >> 1;
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, temp.getWidth(), temp.getHeight());
        graphics.setColor(Color.WHITE);
        graphics.drawString(str, drawX - w, drawY);
    }

    public static void main(String[] args) throws Exception {
        BufferedImage image = new GlyphTextureImageSource(DetailLevel.MEDIUM).call();
        new OutputGraph("Font-Awesome", 1100, 0, 0, 1).addImage(null, image, null, -512, -512, 1);
    }
}
