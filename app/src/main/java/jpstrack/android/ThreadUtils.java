package jpstrack.android;

import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadUtils {
	
	private final static ExecutorService threadpool = Executors.newFixedThreadPool(3);
	
	public static void execute(final Runnable r) {
		threadpool.execute(r);
	}

	static void executeAndWait(final Runnable r) {
		try {
			Future<?> submission = threadpool.submit(r);
			submission.get();
		} catch (ExecutionException | InterruptedException e) {
			String message = "Background thread failure: " + e;
			Log.w("ThreadUtils", message);
			throw new RuntimeException(message);
		}
	}
}
