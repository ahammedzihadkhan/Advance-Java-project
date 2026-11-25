package cinema.ui;

import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Time-based easing. Animations advance each time their component paints, and ask for the next frame. */
final class Motion {
    private Motion() { }

    /** 0 at startNanos, 1 once the given number of seconds has passed. */
    static float progress(long startNanos, float seconds) {
        float t = (System.nanoTime() - startNanos) / 1e9f / seconds;
        return Math.max(0f, Math.min(1f, t));
    }

    static float easeOut(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    /** Eases a 0..1 "hover" level towards whether the pointer is over a component (or any of its children). */
    static final class Hover extends MouseAdapter {
        private static final float SECONDS = 0.18f;
        private final Container owner;
        private float level;
        private long last = System.nanoTime();

        private Hover(Container owner) {
            this.owner = owner;
        }

        static Hover attach(Container owner) {
            Hover hover = new Hover(owner);
            owner.addMouseListener(hover);
            return hover;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            owner.repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            owner.repaint();
        }

        float level() {
            long now = System.nanoTime();
            float step = (now - last) / 1e9f / SECONDS;
            last = now;
            // Asking the pointer position (not just enter/exit events) keeps cards lit while over their buttons.
            boolean inside = owner.isShowing() && owner.getMousePosition(true) != null;
            float target = inside ? 1f : 0f;
            level = target > level ? Math.min(target, level + step) : Math.max(target, level - step);
            if (level != target) {
                owner.repaint();
            }
            return easeOut(level);
        }
    }
}
