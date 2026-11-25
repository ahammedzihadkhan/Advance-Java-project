package cinema.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/** A translucent glass panel: the moving backdrop shows through, with a frosted tint and a bright top edge. */
class Glass extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int ARC = 22;

    private transient Motion.Hover hover;
    private long enterAt;

    Glass(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(new EmptyBorder(20, 22, 20, 22));
    }

    /** Makes the panel glow softly while the pointer is over it. */
    Glass hoverable() {
        hover = Motion.Hover.attach(this);
        return this;
    }

    /** Fades and slides the panel in, starting at the given time (which may be slightly in the future). */
    Glass entering(long startNanos) {
        enterAt = startNanos;
        return this;
    }

    @Override
    public void paint(Graphics g) {
        float t = enterAt == 0 ? 1f : Motion.easeOut(Motion.progress(enterAt, 0.45f));
        if (t >= 1f) {
            super.paint(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive(t));
        g2.translate(0, Math.round((1f - t) * 20));
        super.paint(g2);
        g2.dispose();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Ui.smooth(g);
        int w = getWidth();
        int h = getHeight();
        float lit = hover == null ? 0f : hover.level();
        RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, ARC, ARC);

        Backdrop backdrop = (Backdrop) SwingUtilities.getAncestorOfClass(Backdrop.class, this);
        BufferedImage field = backdrop == null ? null : backdrop.field();
        if (field != null) {
            Point at = SwingUtilities.convertPoint(this, 0, 0, backdrop);
            Graphics2D frost = (Graphics2D) g2.create();
            frost.clip(shape);
            frost.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            frost.drawImage(field, -at.x, -at.y, field.getWidth() * Backdrop.SCALE,
                field.getHeight() * Backdrop.SCALE, null);
            frost.dispose();
        }
        g2.setColor(new Color(255, 255, 255, (int) (16 + 14 * lit)));
        g2.fill(shape);
        g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, (int) (30 + 16 * lit)),
            0, h * 0.5f, new Color(255, 255, 255, 0)));
        g2.fill(shape);
        g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, (int) (75 + 70 * lit)),
            0, h, new Color(255, 255, 255, (int) (16 + 24 * lit))));
        g2.draw(shape);
        g2.dispose();
    }
}
