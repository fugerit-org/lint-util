package lint.util.sonatype.lift;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
			int countCurrentFile = 0;
			String previousPath = null;
			List<String> hrefList = new ArrayList<String>();
			for (Element current : aTag) {
				hrefList.add( current.attr("abs:href") );
			}
			Collections.sort( hrefList, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					int res = o1.compareTo( o2 );
					String[] split1 = o1.split( "#L" );
					String[] split2 = o2.split( "#L" );
					if ( split1.length > 1 && split2.length > 1 ) {
						if ( split1[0].compareTo( split2[0] ) == 0 ) {
							res = Integer.parseInt( split1[1] ) - Integer.parseInt( split2[1] );
						}
					}
					return res;
				}
			} );
			for ( String href : hrefList ) {
				if ( href.startsWith( baseLink ) ) {
					StringBuilder note = new StringBuilder();
					totalFound++;
					String value = href.substring( baseLink.length() );
					String[] data = value.split( "#" );
					String filePath = data[0];
					if ( filePath.equals( previousPath ) ) {
						countCurrentFile++;
					} else {
						countCurrentFile = 0;
					}
					int line = Integer.parseInt( data[1].substring( 1 ) )+countCurrentFile;
					File currentFile = new File( srcPath, filePath );
					if ( currentFile.exists() ) {
						int count = 0;
						try ( StringWriter buffer = new StringWriter(); 
							PrintWriter writer = new PrintWriter( buffer );
							BufferedReader currentReader = new BufferedReader( new FileReader( currentFile ) ) ) {
							note.append( "[enter writer cycle]" );
							String currentLine = currentReader.readLine();
							while ( currentLine != null ) {
								count++;
								if ( line == count ) {
									if ( ( currentLine.contains( "public" ) || currentLine.contains( "protected" ) ) && !currentLine.contains( "@Override" ) ) {
										int index = currentLine.indexOf( "public" );
										if ( index == -1 ) {
											index = currentLine.indexOf( "protected" );
										}
										String pre = currentLine.substring( 0, index );
										writer.println( pre+"@Override" );
										//logger.info( "Adding overrided to file {}, line {}", currentFile, line );
										totalAdded++;	
									} else {
										note.append( "[check public or protected:" );
										note.append( currentLine.contains( "public" ) || currentLine.contains( "protected" )  );
										note.append( ", contains override:" );
										note.append( !currentLine.contains( "@Override" ) );
										note.append( "]" );
									}
								}
								writer.println( currentLine );
								currentLine = currentReader.readLine();
							}
							FileIO.writeString( buffer.toString() , currentFile );
						}
					} else {
						throw new ConfigException( "File does not exist : "+currentFile.getAbsolutePath() );
					}
					previousPath = filePath;
					logger.info( "href -> {} - {}", href, note.toString() );
				}
			}
			logger.info( "total added / found {} / {}", totalAdded, totalFound );
		} catch (IOException e) {
			throw new ConfigException( e );
		}
	}

}
