package dan.serverchecks;

import dan.serverchecks.ServerChecks.DateCheck;
import dan.serverchecks.http.SlowpokeServer;

public class BasicCommandsModule extends CommandsModule {

	@Override
	public void configure() {
		add("black-hole", new ServerChecks.BlackHoleCommand());
		add("date", new DateCheck());
		add("locale", new ServerChecks.LocaleCheck());
		add("max-threads", new MaxThreadsCheck());
		add("nslookup", new NSLookupCheck());
		add("sleepy", new SleepyDaemon());
		add("max-fd", new MaxFD());
		add("env", new SystemProperties());
		add("slowpoke", new SlowpokeServer());
		add("so", new SocketCheck());
		add("chars", new CharDumper());
		add("sigterm", new SigtermCheck());
		add("deadlock", new DeadlockCheck());
		add("lock", new LockCheck());
		
	}
	
}
