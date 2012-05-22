package jpstrack.android;

public class ThreadUtils {
	
	static void execute(final Runnable r) {
		new Thread(r).start();
	}
	
	static void executeAndWait(final Runnable r) {
		Thread t = new Thread(r);
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted, eh? DOH!" + e);
		}
	}
}
