package lint.util.sonatype.lift;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;

import org.fugerit.java.core.cfg.ConfigException;
import org.fugerit.java.core.io.FileIO;
import org.fugerit.java.core.io.StreamIO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MissingOverride {
	
	private final static Logger logger = LoggerFactory.getLogger( MissingOverride.class );
	
	public void apply( Reader reader, String baseLink, String srcPath ) throws ConfigException {
		try {
			String html = StreamIO.readString( reader );
			Document doc = Jsoup.parse( html );
			Elements aTag = doc.select("a[href]");
			logger.info( "total href check : {}", aTag.size() );
			int totalFound = 0;
			int totalAdded = 0;
			for (Element current : aTag) {
				String href = current.attr("abs:href");
				if ( href.startsWith( baseLink ) ) {
					totalFound++;
					String value = href.substring( baseLink.length() );
					String[] data = value.split( "#" );
					String filePath = data[0];
					int line = Integer.parseInt( data[1].substring( 1 ) );
					File currentFile = new File( srcPath, filePath );
					if ( currentFile.exists() ) {
						int count = 0;
						try ( StringWriter buffer = new StringWriter(); 
							PrintWriter writer = new PrintWriter( buffer );
							BufferedReader currentReader = new BufferedReader( new FileReader( currentFile ) ) ) {
							String currentLine = currentReader.readLine();
							while ( currentLine != null ) {
								count++;	
								if ( line == count && currentLine.contains( "public" ) && !currentLine.contains( "@Override" ) ) {
									int index = currentLine.indexOf( "public" );
									String pre = currentLine.substring( 0, index );
									writer.println( pre+"@Override" );
									logger.info( "Adding overrided to file {}, line {}", currentFile, line );
									totalAdded++;
								}
								writer.println( currentLine );
								currentLine = currentReader.readLine();
							}
							FileIO.writeString( buffer.toString() , currentFile );
						}
					} else {
						throw new ConfigException( "File does not exist : "+currentFile.getAbsolutePath() );
					}
				}
			}
			logger.info( "total added / found {} / {}", totalAdded, totalFound );
		} catch (IOException e) {
			throw new ConfigException( e );
		}
	}

}
