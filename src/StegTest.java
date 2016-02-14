import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class StegTest {
	public Steg steg;
	
	@Before
	public void setup() {
		steg = new Steg();
	}
	
	//@Test
	public void testHidingString() {
		String filenameHidden = steg.hideString("I hope this is hidden!", "testImage.bmp");
		assertNotEquals(filenameHidden, "Fail");
	}
	
	//@Test
	public void testExtractingString() {
		String payload = "Hoping this is hidden too!";
		String filenameHidden = steg.hideString(payload, "testImage.bmp");
		String messageReceived = steg.extractString(filenameHidden);
		assertEquals(payload, messageReceived);
	}
	
	//@Test
	public void testHidingFile() {
		String filenameHidden = steg.hideFile("test.txt", "testImage.bmp");
		assertNotEquals(filenameHidden, "Fail");
	}
	
	@Test
	public void testExtractingFile() {
		String payloadFile = "test.txt";
		String filenameHidden = steg.hideFile(payloadFile,  "testImage.bmp");
		System.out.println(filenameHidden);
		String filenameExtracted = steg.extractFile(filenameHidden);
		System.out.println(filenameExtracted);
		assertNotEquals("Fail", filenameExtracted);
	}
	
}
