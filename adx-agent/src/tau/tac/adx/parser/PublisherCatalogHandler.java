package tau.tac.adx.parser;

import se.sics.isl.util.IllegalConfigurationException;
import se.sics.tasim.logtool.LogHandler;
import se.sics.tasim.logtool.LogReader;

import java.io.IOException;
import java.text.ParseException;

/**
 * <code>BankStatusHandler</code> is a simple example of a log handler that uses
 * a specific parser to extract information from log files.
 * 
 * @author - Lee Callender
 */
public class PublisherCatalogHandler extends LogHandler {

	public PublisherCatalogHandler() {
	}

	/**
	 * Invoked when a new log file should be processed.
	 * 
	 * @param reader
	 *            the log reader for the log file.
	 */
	@Override
	protected void start(LogReader reader)
			throws IllegalConfigurationException, IOException, ParseException {
		PublisherCatalogParser parser = new PublisherCatalogParser(reader);
		parser.start();
		parser.stop();
	}
}
