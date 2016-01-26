import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

class Steg
{
	
	
	
	/**
	 * A constant to hold the number of bits per byte
	 */
	private final int byteLength=8;
	
	/**
	 * A constant to hold the number of bits used to store the size of the file extracted
	 */
	protected final int sizeBitsLength=32;
	/**
	 * A constant to hold the number of bits used to store the extension of the file extracted
	 */
	protected final int extBitsLength=64;
	
	 
	 /**
	Default constructor to create a steg object, doesn't do anything - so we actually don't need to declare it explicitly. Oh well. 
	*/
	
	public Steg()
	{
	
	}
	
	/**
	A method for hiding a string in an uncompressed image file such as a .bmp or .png
	You can assume a .bmp will be used
	@param cover_filename - the filename of the cover image as a string 
	@param payload - the string which should be hidden in the cover image.
	@return a string which either contains 'Fail' or the name of the stego image which has been 
	written out as a result of the successful hiding operation. 
	You can assume that the images are all in the same directory as the java files
	*/
	//TODO you must write this method
	public String hideString(String payload, String cover_filename)
	{
		// Select an appropriate filename
		String filenameToReturn = "stegResult.bmp"; 
		Integer fileCounter = 1;
		while ( (new File(filenameToReturn).isFile() )) {
			filenameToReturn = filenameToReturn.split(".")[0].concat(fileCounter.toString()).concat(".bmp");
			fileCounter++;
		}
		
		try {
			Path originalPath = Paths.get(cover_filename);
			byte[] originalData = Files.readAllBytes(originalPath);
			FileOutputStream steg = new FileOutputStream(filenameToReturn);
			int currentByte = 0;  
			
			// The first 54 bytes of the original contain image metadata, so we start at byte 55.
			for (; currentByte < 54; currentByte++) {
				steg.write(originalData[currentByte]);
			}
			
			// Let's throw our string metadata in here.
			String sizeData = String.format("%32s", Integer.toBinaryString(payload.length())).replace(' ', '0'); // Pads to the left with 0s, makes it 32 chars
			for (char bitChar : sizeData.toCharArray()) {
				int bit = (int) bitChar - 48;
				steg.write( swapLsb(bit, originalData[currentByte++]) );
			}
			
			// We can write 0s for the file extension, because what we get out is a string
			// TODO: is this correct?
			for (int index = 0; index < 65; index++) {
				steg.write( swapLsb(0, originalData[currentByte++]) );
			}
			
			// Let's write our steganography stuff!
			for (byte b : payload.getBytes(payload)) {
				String correspondingString = Integer.toBinaryString(b & 0xFF).replace(' ', '0');
				for (char bitChar : correspondingString.toCharArray()) {
					int bit = (int) bitChar - 48;  // Turn the char into a ascii 0 or 1 and reduce down to the int equivalent
					steg.write( swapLsb(bit, originalData[currentByte++]) );
				}
			}
			
			
		} catch (Exception e) {
			return "Fail";
		}
		
		return filenameToReturn;
	
	} 
	//TODO you must write this method
	/**
	The extractString method should extract a string which has been hidden in the stegoimage
	@param the name of the stego image 
	@return a string which contains either the message which has been extracted or 'Fail' which indicates the extraction
	was unsuccessful
	*/
	public String extractString(String stego_image)
	{
		String message = "";
		try {
			int currentByte = 54;
			Path originalPath = Paths.get(stego_image);
			byte[] originalData = Files.readAllBytes(originalPath);
			String sizeBinaryString = "";
			
			// Get the size information
			for (; currentByte < 86; currentByte++) {
				sizeBinaryString.concat( Integer.toString(getLsb(originalData[currentByte])) );
			}
			Integer size = Integer.parseInt(sizeBinaryString, 2);
			
			// This is a string, so skip file extension.
			currentByte += 64;
			
			// Read forward that many bytes and stop.
			int messageEnd = currentByte + (size*8);  // One bit encoded per byte in steg
			String encodedMessage = "";
			for (; currentByte < messageEnd; currentByte++) {
				encodedMessage.concat( Integer.toString(getLsb(originalData[currentByte])) );
			}
			
			// Turn the encoded message into a string by changing each block of 8 digits into a char
			for (int index = 0; index < encodedMessage.length(); index += 8) {
				String currentChar = Integer.toString( (Integer.parseInt(encodedMessage.substring(index, index+8), 2) ) ); // Convert the next 8 bits into its equivalent ascii character
				message.concat(currentChar);
			}
			
		} catch (Exception e) {
			return "Fail";
		}
		return message;
	}
	
	//TODO you must write this method
	/**
	The hideFile method hides any file (so long as there's enough capacity in the image file) in a cover image
	
	@param file_payload - the name of the file to be hidden, you can assume it is in the same directory as the program
	@param cover_image - the name of the cover image file, you can assume it is in the same directory as the program
	@return String - either 'Fail' to indicate an error in the hiding process, or the name of the stego image written out as a
	result of the successful hiding process
	*/
	public String hideFile(String file_payload, String cover_image)
	{
		// Select an appropriate filename
		String filenameToReturn = "stegResult.bmp"; 
		Integer fileCounter = 1;
		while ( (new File(filenameToReturn).isFile() )) {
			filenameToReturn = filenameToReturn.split(".")[0].concat(fileCounter.toString()).concat(".bmp");
			fileCounter++;
		}

		try {
			FileReader payload = new FileReader(file_payload);
			
			FileOutputStream steg = new FileOutputStream(new File(filenameToReturn));
			Path originalImage = Paths.get(cover_image);
			byte[] originalData = Files.readAllBytes(originalImage);
			int currentPosition = 0;  // We can ignore the first 54 points.
			
			// Write the first 54 bytes normally
			for (; currentPosition < 54; currentPosition++) {
				steg.write(originalData[currentPosition]);
			}
			
			// Write the rest of the file as normal.
			for (;payload.hasNextBit();currentPosition++) {
				steg.write(swapLsb(payload.getNextBit(), originalData[currentPosition]));
			}
			
		} catch (Exception e) {
			return "Fail";
		}
		
		return filenameToReturn;
	}
	
	//TODO you must write this method
	/**
	The extractFile method hides any file (so long as there's enough capacity in the image file) in a cover image
	
	@param stego_image - the name of the file to be hidden, you can assume it is in the same directory as the program
	@return String - either 'Fail' to indicate an error in the extraction process, or the name of the file written out as a
	result of the successful extraction process
	*/
	public String extractFile(String stego_image)
	{
	
		return "";
	
	}
	
	//TODO you must write this method
	/**
	 * This method swaps the least significant bit with a bit from the filereader
	 * @param bitToHide - the bit which is to replace the lsb of the byte of the image
	 * @param byt - the current byte
	 * @return the altered byte
	 */
	public int swapLsb(int bitToHide,int byt)
	{		
		return (byt - byt%2) + bitToHide;
	}
	
	// Helper methods
	public int getLsb(int inByte) {
		return (int)inByte % 2;
	}
	
}
