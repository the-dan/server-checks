package dan.serverchecks.http;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WaitServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private final long DEFAULT_DELAY = 10000;
	public final String PARAM_DELAY = "delay";
	
	private long delay = DEFAULT_DELAY;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		
		String strDelay = config.getInitParameter(PARAM_DELAY);
		if (strDelay == null || strDelay.trim().equals("")) {
			return;
		}
		try {
			delay = Long.parseLong(strDelay);
		} catch (NumberFormatException e) {
			System.out.println("Can't init with given delay: " + strDelay);
		}
		
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
		this.doGet(req, resp);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
		SimpleDateFormat f = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		System.out.println("Request started " + f.format(new Date()));
		
		long d = delay;
		try {
			d = Long.parseLong(req.getParameter("delay"));
		} catch (NumberFormatException e) {
			// skip
		}
		
		try {
			Thread.sleep(d);
		} catch (InterruptedException e) {
			System.out.println("Wait interrupted");
			e.printStackTrace();
		}
		
		System.out.println("Thread woked up " + f.format(new Date()));
		
		try {
			resp.setContentType("text/plain");
			resp.setCharacterEncoding("utf-8");
			resp.getWriter().println("Waited for " + d + " ms");
		} catch (IOException e) {
			System.out.println("Write failed at " + f.format(new Date()) + " " + e.getMessage());
		}
		
		System.out.println("Request done " + f.format(new Date()));
	}
	
}
