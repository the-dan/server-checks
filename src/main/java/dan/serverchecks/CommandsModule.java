package dan.serverchecks;

import com.beust.jcommander.JCommander;

import dan.serverchecks.ServerChecks.ServerCheckCommand;

public abstract class CommandsModule {
	
	private JCommander jc;

	public void add(String name, ServerCheckCommand command) {
		jc.addCommand(name, command);
	}
	
	public abstract void configure();
	
	public void setCommander(JCommander jc) {
		this.jc = jc;
	}
	
}
