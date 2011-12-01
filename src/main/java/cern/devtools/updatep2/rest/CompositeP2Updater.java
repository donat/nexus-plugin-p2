package cern.devtools.updatep2.rest;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * Class for building composite P2 update site descriptor based on the file
 * structure. The plugins should be the following hierarchy:
 * p2-root-dir
 *  |-->product1
 *  |    |--> version1.0
 *  |    |--> version2.0
 *  |-->product2
 *       |--> version1.0
 *       |--> version2.0
 *       |--> version3.0  
 * The public functions generate the descriptor and write it to the proper file
 * in the p2 root and in the project root folder too.
 * 
 * 
 * @author dcsikos
 * 
 */
public class CompositeP2Updater {
	/**
	 * Enumerate composite artifact descriptor types.
	 * 
	 * The {@link #value()} method returns the filename, where the descriptor should be stored.
	 * 
	 * For more information: http://www.vogella.de/blog/2010/04/06/eclipse-p2-composite-repository/
	 */
	public enum ArtifactType {
		/**
		 * Composite metadata repository.
		 */
		C_METADATA_REPO("compositeContent.xml"),
		
		/**
		 * Composite artifact repository.
		 */
		C_ARTIFACT_REPO("compositeArtifacts.xml");
		
		private final String fileName;
		
		ArtifactType(String fileName){
			this.fileName = fileName;
		}
		
		public String value() {
			return fileName;
		}
	}
	
	/**
	 * The path for the p2 update site root folder. E.g. /opt/updatesite
	 */
	private final String p2Root;
	
	/**
	 * Title of the update root update site.
	 */
	public static final String MAIN_P2_NAME = "BE-CO-AP";

	/**
	 * Logger instance.
	 */
	private static final Log LOG = LogFactory.getLog(CompositeP2Updater.class);

	/**
	 * Map for storing (products, list of versions) pairs.
	 */
	private Map<String, List<String>> plugins = new HashMap<String, List<String>>();
	
	/**
	 * Dull solution for mutual exclusion at file write operations.
	 * TODO: create per-file grained synchronization.
	 */
	private static Object mutex = new Object();

	
	/**
	 * Constructor.
	 * 
	 * @param p2Root
	 *            The root of the update site directory structure. Can be
	 *            <code>null</code>, if just one product's descriptor will be
	 *            analysed.
	 */
	public CompositeP2Updater(String p2Root) {
		this.p2Root = p2Root;
	}

	/**
	 * Gathers all project versions and build the descriptor for each one, and a
	 * grand update site for all project in the root folder. If IOException
	 * happens when parsing a single plugin, the generation will continue
	 * without that information.
	 * 
	 * @throws IOException
	 *             if the root folder is <code>null</code>, or does not exist.
	 */
	public void processRoot() throws IOException {
		// the root directory should be set, if this function is called
		if (p2Root == null) {
			LOG.error("When analyse the overall hierarchy, the root directory should be set.");
			throw new IOException("When analyse the overall hierarchy, the root directory should be set.");
		}

		// check if P2 root exists
		File rootDir = new File(p2Root);
		if (!rootDir.exists() || !rootDir.isDirectory()) {
			LOG.error(p2Root + " folder does not exist.");
			throw new IOException(p2Root + " folder does not exist.");
		}

		// generate descriptor for all the plugins
		for (File f : rootDir.listFiles()) {
			
			// folders not beginning with "." should be processed 
			if (f.isDirectory() && !f.getName().startsWith(".")) {
				try {
					processPlugin(f.getCanonicalPath());
				} catch (IOException e) {
					LOG.warn("Exception raised while reading " + f.getCanonicalPath() + ".");
				}
			}
		}

		// create descriptor and write it into the proper file	
		String artifact = buildDescriptorString(null, ArtifactType.C_ARTIFACT_REPO);
		String metadata = buildDescriptorString(null, ArtifactType.C_METADATA_REPO);
		
		writeOutput(p2Root + "/" + ArtifactType.C_ARTIFACT_REPO.value(), artifact);
		writeOutput(p2Root + "/" + ArtifactType.C_METADATA_REPO.value(), metadata);
		LOG.info("Root update site descriptor refresh finished: " + p2Root + ".");
	}
	
	/**
	 * Process product version and create descriptor for it.  
	 * 
	 * @param pluginPath the main folder for the product which contains all the versions. 
	 * @throws IOException if file access failed.
	 */
	public void processPlugin(String pluginPath) throws IOException {
		// parse plugin information and check availability
		File pluginFolder = new File(pluginPath);
		if (!pluginFolder.exists()) {
			LOG.warn("Plugin does not exist: " + pluginPath + ".");
		}
		String pluginName = pluginFolder.getName();
		
		// save all the versions for the plugin
		plugins.put(pluginName, new LinkedList<String>());
		File[] versions = pluginFolder.listFiles();
		for (File v : versions) {
			if (v.isDirectory()) {
				String versionNumber = v.getName();
				plugins.get(pluginName).add(versionNumber);
			}
		}

		// write the descriptors to a file
		String artifact = buildDescriptorString(pluginName, ArtifactType.C_ARTIFACT_REPO);
		String metadata = buildDescriptorString(pluginName, ArtifactType.C_METADATA_REPO);
		
		writeOutput(pluginFolder.getAbsolutePath() + "/" + ArtifactType.C_ARTIFACT_REPO.value(), artifact);
		writeOutput(pluginFolder.getAbsolutePath() + "/" + ArtifactType.C_METADATA_REPO.value(), metadata);
		LOG.info("Update site descriptor refresh finished: " + pluginPath + ".");
	}

	/**
	 * Assemble the descriptor from the plugin data (stored in the <code>plugins</code> variable).
	 * 
	 * @param productName the name of the plugin to create the descriptor for. If <code>null</code>, all descriptors will be created.
	 * @param type the descriptor type to generate.
	 * @return the content of the descriptor in raw text format.
	 */
	private String buildDescriptorString(String productName, ArtifactType type) {
		// store the descriptor in stringbuilder
		StringBuilder output = new StringBuilder();
		
		// add header information
		addHeader(type,output);
		
		// start repository tag
		beginRepository(productName,type, output);
		
		// add timestamp properties
		addProperties(output);
		
		// add projects into the descriptor
		addChildren(productName, output);
		
		// close repository tag
		endRepository(output);

		// return result
		return output.toString();
	}

	private void addChildren(String productName, StringBuilder output) {
		output.append("  <children size='");
		int size = 0;
		if (productName == null) {
			for (List<String> pl : plugins.values()) {
				size += pl.size();
			}
		} else {
			size = plugins.get(productName).size();
		}
		output.append(size);
		output.append("'>\n");

		// add the plugins relative path to the descriptor.
		if (productName == null) {
			for (String p : plugins.keySet()) {
				for (String v : plugins.get(p)) {
					output.append("    <child location='");
					output.append(p + "/" + v);
					output.append("'/>\n");
				}
			}
		} else {
			for (String v : plugins.get(productName)) {
				output.append("    <child location='");
				output.append(v);
				output.append("'/>\n");
			}
		}

		// close remaining xml tags.
		output.append("  </children>\n");
	}

	private static void addHeader(ArtifactType type, StringBuilder output) {
		output.append("<?xml version='1.0' encoding='UTF-8'?>\n");
		
		if(type == ArtifactType.C_ARTIFACT_REPO) {
			output.append("<?compositeArtifactRepository version='1.0.0'?>\n");
		}
		else if (type == ArtifactType.C_METADATA_REPO) {
			output.append("<?compositeMetadataRepository version='1.0.0'?>\n");
		}
	}
	
	private static void addProperties(StringBuilder output) {
		output.append("  <properties size='1'>\n    <property name='p2.timestamp' value='");
		output.append(System.currentTimeMillis());
		output.append("'/>\n");
		output.append("  </properties>\n");
	}

	private static void beginRepository(String productName, ArtifactType type, StringBuilder output) {
		output.append("<repository name='");
		if (productName == null) {
			output.append(MAIN_P2_NAME);
		} else {
			output.append(productName);
		}
		
		if (type == ArtifactType.C_ARTIFACT_REPO) {
			output.append(" - Update Site' type='org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository' version='1.0.0'>\n");
		}
		else if (type == ArtifactType.C_METADATA_REPO) {
			output.append(" - Update Site' type='org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository' version='1.0.0'>\n");
		}
	}
	
	private static void endRepository(StringBuilder output) {
		output.append("</repository>\n");
	}

	/**
	 * Write descriptor into the specified file.
	 * 
	 * @param path the path for writing.
	 * @param content the content to write.
	 * @throws IOException if writing fails.
	 */
	private static void writeOutput(String path, String content) throws IOException {
		synchronized (mutex) {
			File outputFile = new File(path);
			FileWriter writer = new FileWriter(outputFile);
			writer.append(content);
			writer.flush();
			writer.close();
		}
	}

	/**
	 * Demo.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		CompositeP2Updater cp2u = new CompositeP2Updater("/c:/wamp/www");
		cp2u.processRoot();
	}
}
