package dan.serverchecks.http;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.beust.jcommander.Parameter;

import dan.serverchecks.ServerChecks.ServerCheckCommand;

/**
 * Simple servlets to emulate common problems
 * like delays, error codes handling by proxies, request dumps
 * when setting up proxy servers
 */
public class SlowpokeServer implements ServerCheckCommand {
	
	@Parameter(names = {"-p", "--port"}, description = "Port to bind to. Default: 10000")
	public int port = 10000;
	@Parameter(names = {"-u", "--uri"}, description="URI to create info servlet at. Default: /info")
	public String uri = "/info";
	
	public void execute() {
		Server server = new Server(port);
		server.setSendServerVersion(false);
		ServletContextHandler contextHandler = new ServletContextHandler(
						ServletContextHandler.NO_SESSIONS);
		contextHandler.setContextPath("/");
		contextHandler.addServlet(	new ServletHolder(new WaitServlet()),
									"/wait");
		
		HandlerCollection handlers = new HandlerCollection();
		handlers.addHandler(contextHandler);
		
		server.setHandler(handlers);
		
		try {
			server.start();
			server.join();
		} catch (Exception e) {
			System.out.println("Failed to start server or interrupted");
			e.printStackTrace();
		}
	}
	
}
