package org.lby123165.scroll_whell_command.client.exec;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

public final class CommandScheduler {
    private static final Queue<ScheduledTask> queue = new ArrayDeque<>();

    private CommandScheduler() {}

    public static void init() {
        queue.clear();
    }

    public static int msToTicks(int ms) {
        if (ms <= 0) return 0;
        // 20 ticks per second => 50ms per tick; round up
        return (ms + 49) / 50;
    }

    public static void schedule(Runnable run, int delayTicks) {
        queue.add(new ScheduledTask(run, Math.max(0, delayTicks)));
    }

    public static void tick(MinecraftClient client) {
        if (queue.isEmpty()) return;
        for (Iterator<ScheduledTask> it = queue.iterator(); it.hasNext();) {
            ScheduledTask t = it.next();
            if (t.ticks > 0) {
                t.ticks--;
            }
            if (t.ticks == 0) {
                try { t.run.run(); } catch (Throwable ignored) {}
                it.remove();
            }
        }
    }

    private static class ScheduledTask {
        final Runnable run;
        int ticks;
        ScheduledTask(Runnable run, int ticks) { this.run = run; this.ticks = ticks; }
    }
}
