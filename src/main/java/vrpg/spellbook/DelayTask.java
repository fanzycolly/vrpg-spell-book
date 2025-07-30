package vrpg.spellbook;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class DelayTask {
    private static final List<DelayTask> tasks = new ArrayList<>();

    private final Runnable callback;
    private int remainingTicks;

    private DelayTask(Runnable callback, double delaySeconds) {
        this.remainingTicks = (int) Math.ceil(delaySeconds * 20);
        this.callback = callback;
    }

    public static void add(Runnable callback, double delaySeconds) {
        DelayTask task = new DelayTask(callback, delaySeconds);
        tasks.add(task);
    }

    public static void tick() {
        Iterator<DelayTask> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            DelayTask task = iterator.next();
            task.remainingTicks--;
            if (task.remainingTicks <= 0) {
                try {
                    task.callback.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                iterator.remove();
            }
        }
    }
}
