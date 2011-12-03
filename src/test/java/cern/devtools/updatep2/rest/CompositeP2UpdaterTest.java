/*
 * File: CompositeP2UpdaterTest.java.
 * Created by Donat Csikos<dcsikos@cern.ch> at 3 Dec 2011.
 *
 * Copyright CERN 2011, All Rights Reserved.
 */
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
