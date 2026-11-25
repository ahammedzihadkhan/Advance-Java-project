package cinema.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Animated window background: soft pools of red and amber light drifting over a dark base.
 * It is rendered small and stretched, which makes it smooth and cheap; Glass panels reuse the same image.
 */
final class Backdrop extends JPanel {
    private static final long serialVersionUID = 1L;
    static final int SCALE = 10;
    private static final Color BASE = new Color(0x0F0A0E);
    private static final long START = System.nanoTime();

    // 20 frames a second is plenty for light that drifts this slowly, and halves the CPU cost of 30.
    // Hover and transition effects request their own frames, so they stay smooth.
    private final Timer timer = new Timer(50, e -> repaint());
    private transient BufferedImage field;
    private long shownAt;

    Backdrop(LayoutManager layout) {
        super(layout);
        setOpaque(true);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    // Stops the animation when the window closes, so it doesn't keep running after logout.
    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    /** The current colour field, drawn at SCALE times its size; null before the first paint. */
    BufferedImage field() {
        return field;
    }

    @Override
    protected void paintComponent(Graphics g) {
        float t = (System.nanoTime() - START) / 1e9f;
        field = render(getWidth() / SCALE + 2, getHeight() / SCALE + 2, t);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(field, 0, 0, field.getWidth() * SCALE, field.getHeight() * SCALE, null);
        g2.dispose();
    }

    /** Window entrance: everything on top of the background fades in and rises into place. */
    @Override
    protected void paintChildren(Graphics g) {
        if (shownAt == 0) {
            shownAt = System.nanoTime();
        }
        float t = Motion.easeOut(Motion.progress(shownAt, 0.55f));
        if (t >= 1f) {
            super.paintChildren(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive(t));
        g2.translate(0, Math.round((1f - t) * 26));
        super.paintChildren(g2);
        g2.dispose();
    }

    private static BufferedImage render(int w, int h, float t) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(BASE);
        g.fillRect(0, 0, w, h);
        // Slow, out-of-step sine waves so the light never repeats in an obvious loop.
        blob(g, w, h, 0.22 + 0.12 * Math.sin(t * 0.13), 0.30 + 0.12 * Math.cos(t * 0.11), 0.55,
            new Color(229, 57, 70, 150));
        blob(g, w, h, 0.80 + 0.10 * Math.cos(t * 0.09), 0.22 + 0.14 * Math.sin(t * 0.12), 0.45,
            new Color(242, 160, 65, 105));
        blob(g, w, h, 0.62 + 0.16 * Math.sin(t * 0.07 + 2), 0.88 + 0.08 * Math.cos(t * 0.10), 0.60,
            new Color(130, 30, 60, 170));
        g.setPaint(new RadialGradientPaint(new Point2D.Float(w / 2f, h / 2f), Math.max(w, h) * 0.8f,
            new float[] {0.4f, 1f}, new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 150)}));
        g.fillRect(0, 0, w, h);
        g.dispose();
        return image;
    }

    private static void blob(Graphics2D g, int w, int h, double x, double y, double radius, Color color) {
        float r = (float) (Math.max(w, h) * radius);
        g.setPaint(new RadialGradientPaint(new Point2D.Float((float) (w * x), (float) (h * y)), r,
            new float[] {0f, 1f}, new Color[] {color, new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)}));
        g.fillRect(0, 0, w, h);
    }
}
