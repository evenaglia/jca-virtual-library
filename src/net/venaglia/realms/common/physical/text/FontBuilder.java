package net.venaglia.realms.common.physical.text;

import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.texture.Texture;
import net.venaglia.realms.common.physical.texture.impl.TextureFactory;
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
public class FontBuilder {

    private final DetailLevel detailLevel;

    private Texture texture;
    private Font font;
    private float[] characterWidth;

    public FontBuilder(String fontResource) {
        this(fontResource, DetailLevel.MEDIUM);
    }

    public FontBuilder(String fontResource, DetailLevel detailLevel) {
        this.detailLevel = detailLevel;
        if (!fontResource.endsWith(".ttf")) {
            throw new IllegalArgumentException("Passed resource is not a True Type Font: " + fontResource);
        }
        URL fontURL = Thread.currentThread().getContextClassLoader().getResource(fontResource);
        if (fontURL == null) {
            throw new IllegalArgumentException("Unable to find classpath font resource: " + fontResource);
        }
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, fontURL.openStream());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Texture getTexture() {
        if (texture == null) {
            Font font = getFont().deriveFont(256.0f);
            texture = new TextureFactory().loadImageSource(font.getName(), getTextureSource(detailLevel)).build();
        }
        return texture;
    }

    private Callable<BufferedImage> getTextureSource(final DetailLevel detailLevel) {
        return new Callable<BufferedImage>() {
            public BufferedImage call() throws Exception {
                int size = detailLevel.steps * 4;
                Font font = getFont().deriveFont(Font.PLAIN, size * 0.75f * 3.0f);
                BufferedImage temp = new BufferedImage(3 * size, 4 * size, BufferedImage.TYPE_4BYTE_ABGR);
                BufferedImage image = new BufferedImage(16 * size, 16 * size, BufferedImage.TYPE_4BYTE_ABGR);
                Graphics graphics = image.getGraphics();
                graphics.setColor(Color.BLACK);
                ((Graphics2D)graphics).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
                int drawX = temp.getWidth() >> 1;
                int drawY = Math.round((size * 3) * 0.75f);
                char c = ' ';
                outer:
                for (int j = 0; j < 16; j++) {
                    for (int i = 0; i < 16; i++) {
                        drawCharacter(temp, font, drawX, drawY, c);
                        int x1 = Math.round(size * i);
                        int y1 = Math.round(size * j);
                        int x2 = Math.round(size * i + size);
                        int y2 = Math.round(size * j + size);
                        graphics.drawImage(temp, x1, y1, x2, y2, 0, 0, temp.getWidth(), temp.getHeight(), null);
                        c++;
                        if (c > '~') break outer;
                    }
                }
                return image;
            }

            private void drawCharacter(BufferedImage temp, Font font, int drawX, int drawY, char c) {
                String str = " " + c + " ";
                Graphics graphics = temp.getGraphics();
        //        ((Graphics2D)graphics).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                ((Graphics2D)graphics).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                graphics.setFont(font);
                FontMetrics fontMetrics = graphics.getFontMetrics();
                int w = fontMetrics.stringWidth(str) >> 1;
        //        graphics.setColor(Color.GRAY);
        //        graphics.setColor(Color.BLACK);
                graphics.clearRect(0, 0, temp.getWidth(), temp.getHeight());
                graphics.setColor(Color.WHITE);
                graphics.drawString(str, drawX - w, drawY);
            }
        };
    }

    public Font getFont() {
        return font;
    }

    public float getWidth(char c) {
        if (c < ' ' || c > '~') {
            return 0;
        }
        if (characterWidth == null) {
            BufferedImage img = new BufferedImage(16,16,BufferedImage.TYPE_4BYTE_ABGR);
            Font font = getFont().deriveFont(256.0f);
            FontMetrics fontMetrics = img.getGraphics().getFontMetrics(font);
            characterWidth = new float[128];
            for (char z = ' '; z <= '~'; z++) {
                characterWidth[z] = fontMetrics.charWidth(z) * 0.00390625f * 0.75f;
            }
        }
        return characterWidth[c];
    }

    public boolean getCharcaterBox(char c, float[] box) {
        float w = getWidth(c);
        if (w <= 0) {
            return false;
        }

        int i = c - ' ';
        float p = 0.0625f;
        float x = (i % 16) * p + 0.03125f;
        float v = 0.03125f * w;
        box[0] = x - v;
        box[2] = x + v;

        int y = i / 16;
        box[1] = y * p + (p / 16.0f);
        box[3] = y * p + 0.0625f - (3 * p / 16.0f);

        return true;
    }

    public static void main(String[] args) throws Exception {
        FontBuilder fontBuilder = new FontBuilder("fonts/DroidSansMono.ttf", DetailLevel.MEDIUM);
        BufferedImage image = fontBuilder.getTextureSource(DetailLevel.MEDIUM).call();
        new OutputGraph("DroidSansMono", 1100, 0, 0, 1).addImage(null, image, null, -512, -512, 1);
    }
}
