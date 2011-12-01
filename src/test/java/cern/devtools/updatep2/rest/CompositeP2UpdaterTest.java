package cern.devtools.updatep2.rest;

import org.junit.Test;

public class CompositeP2UpdaterTest {
	
	public void before(){
		
	}
	
	public void after() {
		
	}
	
	@Test
	public void testGenerate() throws Exception {
		CompositeP2Updater p2 = new CompositeP2Updater("./src/test/resources/p2root");
		p2.processRoot();
	}
	
	

}
