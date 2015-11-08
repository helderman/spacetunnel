// A.java - main application class

import com.nttdocomo.ui.*;

public class A
	extends IApplication
{
	public void start()
	{
		B b = new B();
		Display.setCurrent(b);	// this will call B.paint()
		b.run();
		terminate();
	}
}
