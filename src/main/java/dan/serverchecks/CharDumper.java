package dan.serverchecks;

import java.io.IOException;
import java.io.InputStream;

import dan.serverchecks.ServerChecks.ServerCheckCommand;

public class CharDumper implements ServerCheckCommand {

	public void execute() {
		InputStream in = System.in;
		try {
			int ch = in.read();
			while (ch > 0) {
				int av = in.available();
				for (int i = 0; i < av; i++) {
					String hex = Integer.toHexString(ch);
					System.out.print(hex);
					System.out.print(" ");
					ch = in.read();
				}
				System.out.println(Integer.toHexString(ch));
				ch = in.read();
			}
			System.out.println(Integer.toHexString(ch));
		} catch (IOException e) {
			System.out.println("Uh-oh, you've managed to damage stdin");
		}
	}

}
