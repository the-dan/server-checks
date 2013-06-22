package dan.serverchecks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import dan.serverchecks.ServerChecks.ServerCheckCommand;


/**
 * 
 * This little socket server designed to
 * allow you to tinker with tcp/ip stack, emulate different network 
 * problems.
 * 
 * E.g.:
 * 1) emulating connect timeouts
 * 2) emulating read timeouts
 * 3) emulating firewall killing a connection
 * 4) emulating write timeout
 * 5) emulating tcp keepalive
 * 
 * In all of these cases you can also see specific exceptions
 * raised by Java standard library.
 * 
 * Even better to watch how changing linux tcp/ip stack
 * settings influence behavior. Sure in this case it's better
 * to run tcpdump and analyze network traffic.
 */
@Parameters(commandDescription = "Starts TCP server or connects to TCP server." +
		"If file not specified, reads from stdin")
public class SocketCheck implements ServerCheckCommand {

	
	private void teeCopy(InputStream is, OutputStream os) throws IOException {
		byte[] buffer = new byte[4096];
        int n = 0;
        while (-1 != (n = is.read(buffer))) {
            os.write(buffer, 0, n);
            log(new String(buffer));
        }
	}
	
	private final class SocketReader implements Runnable {
		private Socket s = null;
		
		public SocketReader(Socket s) {
			this.s = s;
		}

		public void run() {
			InputStream is = null;
			try {
				is = s.getInputStream();
				IOUtils.copy(is, System.out);
			} catch (IOException e) {
				log("Error reading from socket");
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(is);
			}
		}
	}

	@Parameter(names = {"-k", "--keep-alive"}, required=false, description = "Enable SO_KEEPALIVE")
	public boolean doKeepAlive = false;
	
	@Parameter(names = {"-n", "--nagle"}, required = false, description = "Enabled Nagle's algorithm TCP_NODELAY")
	public boolean doNagle = false;
	
	@Parameter(names = {"-r", "--read-timeout"}, required = false, description = "Read timeout, secs")
	public int readTimeout = 0;
	
	@Parameter(names = {"-c", "--connect-timeout"}, required = false, description = "Connect timeout, secs")
	public long connectTimeout = 0;
	
	
	@Parameter(names = {"-f", "--file"}, required = false, description = "File to read data from. Data read will be written to socket output stream")
	public File file;
	
	@Parameter(names = {"-o", "--log-file"}, required = false, description = "File to log input and output to")
	public File log;
	
	@Parameter(description = "listen <port> | connect <host> <port>")
	public List<String> params = new ArrayList<String>();
	
	private PrintWriter osw;
	
	private void log(String message) {
		SimpleDateFormat sdf = new SimpleDateFormat("dd.mm.yyyy HH:MM:SS");
		String date = sdf.format(new Date());
		if (osw != null) {
			osw.println(date + " " + message);
		}
		System.out.println(date + " " + message);
	}
	
	public void execute() {
		
		
		if (params.size() == 0) {
			System.out.println("Operation mode isn't specified");
			System.exit(1);
		}
		
		String mode = params.get(0);
		String hostname = "";
		int port = 0;
		if ("listen".equals(mode)) {
			if (params.size() < 2) {
				System.out.println("Wrong number of required parameters");
				System.exit(1);
			}
			port = NumberUtils.toInt(params.get(1));
			if (params.size() > 2) {
				hostname = params.get(2);
			} else {
					hostname = "0.0.0.0";
			}
		} else if ("connect".equals(mode)) {
			if (params.size() < 3) {
				System.out.println("Wrong number of required parameters");
				System.exit(1);
			}
			hostname = params.get(1);
			port = NumberUtils.toInt(params.get(2));
		} else {
			System.out.println("Unknown operation mode " + mode);
			System.exit(1);
		}
		
		if (file != null && !file.exists()) {
			System.out.println("File " + file.getName() + " doesn't exist");
			System.exit(1);
		}
		
		FileInputStream fis = null;
		if (file != null) {
			try {
				fis = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				System.out.println("File exists but not found");
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		try {
			if (log != null) {
				FileOutputStream fos = new FileOutputStream(log);
				osw = new PrintWriter(fos);
			}
		} catch (FileNotFoundException e1) {
			System.err.println("File " + log + " not found");
			System.exit(1);
		}
	
		if ("connect".equals(mode)) {
			Socket s = new Socket();
			try {
				s.setKeepAlive(doKeepAlive);
				s.setTcpNoDelay(doNagle);
				s.setSoTimeout((int) TimeUnit.SECONDS.toMillis(readTimeout));
			} catch(SocketException e) {
				System.out.println("Failed to set required parameters");
				e.printStackTrace();
			}
			connect(s, hostname, port, fis);
		} else {
			ServerSocket ss = null;
			try {
				ss = new ServerSocket();
				ss.setReuseAddress(true);
				log("Binding on " + hostname + ":" + port + " ...");
				ss.bind(new InetSocketAddress(hostname, port));
				log("done");
			} catch (IOException e) {
				System.out.println("Bind failed");
				e.printStackTrace();
				System.exit(2);
			}
			
			Socket cs = null;
			try {
				log("Accepting");
				cs = ss.accept();
				log("done");
			} catch (IOException e) {
				log("Accept failed");
				e.printStackTrace();
				System.exit(3);
			}
			
			Thread socketReader = new Thread(new SocketReader(cs));
			socketReader.start();
			
			OutputStream os = null;
			
			try {
				os = cs.getOutputStream();
								
				if (fis != null) {
					IOUtils.copy(fis, os);
				} else {
					int ch = System.in.read();
					while (ch > 0) {
						os.write(ch);
						ch = System.in.read();
					}
				}
				
			} catch (IOException e) {
				log("IO Error while reading or writing");
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(os);
			}
			
			try {
				socketReader.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
						
		}
	}

	private void connect(Socket s, String hostname, int port,
			FileInputStream fis) {
		SocketAddress sa = new InetSocketAddress(hostname, port);
		try {
			log("Connecting...");
			
			s.connect(sa, (int)TimeUnit.SECONDS.toMillis(connectTimeout));
			
			log("done");
		} catch (SocketTimeoutException e) {
			log("Timed out on connect");
			e.printStackTrace();
		} catch (IOException e) {
			log("IO error occured");
			e.printStackTrace();
		}

		// TODO: dump socket info
		// TODO: accept some commands like 'ICLOSE', 'OCLOSE', 'CLOSE', 'DUMP'
		// TODO: make it possible to change the sequence of read & write
		// TODO: closing os/is closes underlying socket. so what to do?
		// TODO: dump os/is available data length
		
		// the architecture of reader and writer is a little bit different
		// compared with nc, for example. nc uses epoll,kqueue,poll,select
		// I believe it's because they use nmap library to do low level stuff
		// and want to keep their implementation single-threaded (possible because using pthreads
		// is too much for such a small program).
		// here I'll try to use threaded implementation first
		
		Thread socketReader = new Thread(new SocketReader(s));
		socketReader.start();
		
		OutputStream os = null;
		
		try {
			os = s.getOutputStream();
			
			
			if (fis != null) {
				IOUtils.copy(fis, os);
			} else {
				int ch = System.in.read();
				while (ch > 0) {
					os.write(ch);
					ch = System.in.read();
				}
			}
			
		} catch (IOException e) {
			log("IO Error while reading or writing");
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(os);
		}
		
		try {
			socketReader.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
