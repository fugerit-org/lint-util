package test.lint.util.sonatype.lift;

import static org.junit.Assert.fail;

import java.io.FileReader;
import java.io.Reader;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lint.util.sonatype.lift.MissingOverride;

public class TestMissingOverride {

	private final static Logger logger = LoggerFactory.getLogger( TestMissingOverride.class );
	
	@Test
	public void test() {
		String file = "src/test/resources/missing_override/sonatype_lift_test.html";
		String baseLink = "https://github.com/fugerit-org/fj-lib/blob/9ff3f1372362ba393fa3bbc662847739bf5129da/";
		String srcPath = "../fj-lib";
		try ( Reader reader = new FileReader( file ) ) {
			MissingOverride fun = new MissingOverride();
			fun.apply( reader, baseLink, srcPath );
		} catch (Exception e) {
			String message = "error : "+e;
			logger.error( message, e );
			fail( message );
		}
	}

}
