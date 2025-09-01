package dev.nipafx.scia.observe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ThreadDumper {

	private static final Logger LOG = LoggerFactory.getLogger(ThreadDumper.class);

	public static void createDumpAfter(int delayInMs) {
		Thread
				.ofVirtual()
				.name("thread-dumper")
				.start(() -> {
					try {
						Thread.sleep(delayInMs);
						var jcmd = new ProcessBuilder(
								"jcmd",
								Long.toString(ProcessHandle.current().pid()),
								"Thread.dump_to_file",
								"-format=json",
								"-overwrite",
								"threads.json"
						).start();

						jcmd.waitFor();
						LOG.info("Thread dump created");
					} catch (InterruptedException ex) {
						LOG.error("Thread dumper interrupted - no dump created", ex);
					} catch (IOException ex) {
						LOG.error("Thread dumper failed - no dump created (probably)", ex);
					}
				});
	}

}

