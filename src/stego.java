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

			// Fix the bits in the original data for writing.
			for (int index = 0; index < originalData.length; index++) {
				originalData[index] = (byte) (originalData[index] & 0xFF);
			}

			// The first 54 bytes of the original contain image metadata, so we start at byte 55.
			for (; currentByte < 54; currentByte++) {
				steg.write(originalData[currentByte]);
			}

			// Let's write in the size.
			String sizeData = intToPaddedBinaryString(payload.length(), sizeBitsLength, '0');
			for (char bitChar : sizeData.toCharArray()) {
				int bit = (int) bitChar - 48;
				int newbyte = swapLsb(bit, originalData[currentByte++]);

				steg.write( newbyte );
			}

			byte[] payloadBytes = payload.getBytes();
			// Let's write our steganography stuff!
			for (int index = 0; index < payloadBytes.length; index++) {
				byte b = payloadBytes[index];
				String correspondingString = intToPaddedBinaryString(b, 8, '0');
				for (char bitChar : correspondingString.toCharArray()) {
					int bit = (int) bitChar - 48;  // Turn the char into a ascii 0 or 1 and reduce down to the int equivalent
					steg.write( swapLsb(bit, originalData[currentByte++]) );
				}
			}

			// Write the rest of the original file. 
			while (currentByte < originalData.length) {
				steg.write(originalData[currentByte++]);
			}

			steg.close();

		} catch (Exception e) {
			return "Fail";
		}

		return filenameToReturn;

	} 
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
				sizeBinaryString = sizeBinaryString.concat( Integer.toString(getLsb(originalData[currentByte])) );
			}
			Integer size = Integer.parseInt(sizeBinaryString, 2);

			// Read forward that many bytes and stop.
			int messageEnd = currentByte + (size*8);  // One bit encoded per byte in steg
			String encodedMessage = "";
			for (; currentByte < messageEnd; currentByte++) {
				encodedMessage = encodedMessage.concat( Integer.toString(getLsb(originalData[currentByte])) );
			}

			// Switch from a binary string of ascii to a string of chars the ascii represents
			message = asciiStringToString(encodedMessage);

		} catch (Exception e) {
			e.printStackTrace();
			return "Fail";
		}
		return message;
	}

	/**
	The hideFile method hides any file (so long as there's enough capacity in the image file) in a cover image

	@param file_payload - the name of the file to be hidden, you can assume it is in the same directory as the program
	@param cover_image - the name of the cover image file, you can assume it is in the same directory as the program
	@return String - either 'Fail' to indicate an error in the hiding process, or the name of the stego image written out as a
	result of the successful hiding process
	 */
	/*public String hideFile(String file_payload, String cover_image)
	{
		// Select an appropriate filename
		String filenameToReturn = "stegResult.bmp"; 
		Integer fileCounter = 1;
		while ( (new File(filenameToReturn).isFile() )) {
			filenameToReturn = filenameToReturn.split(".")[0].concat(fileCounter.toString()).concat(".bmp");
			fileCounter++;
		}

		try {
			//FileReader payload = new FileReader(file_payload);
			FileOutputStream steg = new FileOutputStream(new File(filenameToReturn));
			Path originalImage = Paths.get(cover_image);
			byte[] originalData = Files.readAllBytes(originalImage);
			int currentPosition = 0;
			Path payloadPath = Paths.get(file_payload);
			byte[] payloadData = Files.readAllBytes(payloadPath);

			// Write the first 54 bytes normally
			for (; currentPosition < 54; currentPosition++) {
				steg.write(originalData[currentPosition]);
			}


			//TODO: Write size bits
			String sizeBits = intToPaddedBinaryString(payloadData.length, sizeBitsLength, '0');
			for (char sizeBit : sizeBits.toCharArray()) {
				steg.write(swapLsb((int)sizeBit - 48, originalData[currentPosition++]));
			}
			//TODO: Write ext bits
			int extIndex = file_payload.split(".").length - 1;
			String ext = file_payload.split(".")[extIndex];
			// Now that we have the extension, let's cycle through it and work out what needs written.
			String extBinary = "";
			for (char extChar : ext.toCharArray()) {
				// Make sure each char of the extension we're writing is 8 bits long.
				extBinary = extBinary.concat( intToPaddedBinaryString( (int)extChar, 8, '0')); 
			}

			//TODO: Write steganographic data

			// There is no more steganographic data to write; finish writing the file as a regular file. 
			while(currentPosition < originalData.length) {
				steg.write(originalData[currentPosition++]);
			}

			steg.close();

		} catch (Exception e) {
			e.printStackTrace();
			return "Fail";
		}

		return filenameToReturn;
	}*/
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
			
			// The data of the original image, without a hidden file
			Path originalPath = Paths.get(cover_image);
			byte[] originalData = Files.readAllBytes(originalPath);

			// The data from the file we're hiding
			Path payloadPath = Paths.get(file_payload);
			byte[] payloadData = Files.readAllBytes(payloadPath);
			// Useful to have this as a bit string...
			String payloadBitString = "";
			for (byte b : payloadData) {
				payloadBitString = payloadBitString.concat( intToPaddedBinaryString(b, 8, '0') );
			}
			System.out.println("Got here");
			// Our steganographic output and our position in it
			FileOutputStream steg = new FileOutputStream(new File(filenameToReturn));
			int currentPosition = 0;

			// Write original 54 byte header
			while (currentPosition < 54) {
				steg.write(  originalData[currentPosition++] );
			}

			System.out.println("Got here");
			// Write size
			String size = intToPaddedBinaryString(payloadBitString.length(), 32, '0'); // Get the size as it should be written to steg, in a binary string
			for (char currentBitChar : size.toCharArray()) {
				int currentBit = (int)currentBitChar - 48; // Convert from ascii to 0 or 1
				steg.write( swapLsb(currentBit, originalData[currentPosition++]) );
			}

			System.out.println("Got here");
			// Write ext
			String extRaw = file_payload.substring(file_payload.lastIndexOf('.')); // Get the ext as chars
			String ext = "";
			for (char c : extRaw.toCharArray()) {
				ext = ext.concat( intToPaddedBinaryString((int)c, 8, '0') );
			}
			// We need to format the string one last time incase we have <8 chars. 
			// However, we can fill the rest with a stop char that we know is invalid; if we hit this in extraction we know we...
			// ...couldn't fill 64 bits of ext and had to make up the rest with stopchars, so we can stop reading the ext then and we know the ext precicely
			// This isn't an issue if we have 8 chars, because we won't use the stopchar at all and the problem won't arise. 
			// We use 00011111 as the stopchar, the ASCII unit seperator.
			while (ext.length() != 64) {
				ext = ext.concat("00011111");
			}
			// Now, we write the ext to steg!
			for (char c : ext.toCharArray()) {
				int currentBit  = (int) c - 48;
				steg.write( swapLsb(currentBit, originalData[currentPosition++]) );
			}

			System.out.println("Got here");
			// Write file data
			for (char c : payloadBitString.toCharArray()) {
				int currentBit = (int)c - 48;
				steg.write( swapLsb(currentBit, originalData[currentPosition++]) );
			}
			
			// Now let's write the rest of the original data and get out. 
			while (currentPosition < originalData.length) {
				steg.write(originalData[currentPosition++]);
			}

			System.out.println("Got here");
			// We're done, so let's clean up and get out. 
			steg.close();			System.out.println("Got here");

			return filenameToReturn;
			
		} catch (Exception e) {
			e.printStackTrace();
			return "Fail";
		}

	}



	/**
	The extractFile method hides any file (so long as there's enough capacity in the image file) in a cover image

	@param stego_image - the name of the file to be hidden, you can assume it is in the same directory as the program
	@return String - either 'Fail' to indicate an error in the extraction process, or the name of the file written out as a
	result of the successful extraction process
	 */
	public String extractFile(String stego_image)
	{

		try {
			// For reading bits from stegOriginal
			FileReader stegOriginal = new FileReader(stego_image);
			// Get the original data
			Path originalImage = Paths.get(stego_image);
			byte[] originalData = Files.readAllBytes(originalImage);

			// Make sure nothing crazy has happened so far...
			if (!stegOriginal.getSuccessBool()) {
				throw new Exception();
			}
			// To hold the information we've just extracted
			int currentBit = 0;
			int bitIndex = 0;
			String sizeString = "";
			String extString = "";
			// Cycle through and process what we've got
			while (bitIndex < 54 + sizeBitsLength + extBitsLength) {

				currentBit = stegOriginal.getNextBit();

				// Avoid the first 54 bits returned, it's Windows metadata. 
				if (bitIndex > 54) {
					// If we're currently at the size section
					if (bitIndex < (54 + sizeBitsLength)) {
						sizeString = sizeString.concat(Integer.toString(currentBit));
					}

					// If we're currently getting the extension
					if (bitIndex > 54+sizeBitsLength) {
						extString = extString.concat(Integer.toString(currentBit));
					}

				} 

				bitIndex++;

			}


			sizeString = asciiStringToString(sizeString);
			extString = asciiStringToString(extString);

			// Get filename using ext
			// Select an appropriate filename
			String filenameToReturn = "stegResult.".concat(extString); 
			Integer fileCounter = 1;
			while ( (new File(filenameToReturn).isFile() )) {
				filenameToReturn = filenameToReturn.split(".")[0].concat(fileCounter.toString()).concat(extString);
				fileCounter++;
			}			

			FileOutputStream stegOut = new FileOutputStream(filenameToReturn);

			// Extract actual file data and write to stegOut
			while (stegOriginal.hasNextBit()) {
				stegOut.write(stegOriginal.getNextBit());
			}
			stegOut.close();
			return filenameToReturn;

		} catch(Exception e) {
			return "Fail";
		}

	}

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

	// Retreive the least significant bit
	public int getLsb(int inByte) {
		return Math.abs((int)inByte % 2); // we take abs because -a%2 is negative.
	}

	// Convert a binary string into the string its ascii represents
	// Returns null if input is of invalid length (must divide by 8
	public String asciiStringToString(String uin) {
		if (uin.length() % 8 == 0) {
			String output = "";

			for (int currentIndex = 0; currentIndex < uin.length(); currentIndex += 8) {
				char currentChar = (char)Integer.parseInt(uin.substring(currentIndex, currentIndex + 8), 2);
				output = output.concat(Character.toString(currentChar)); 
			}

			return output;

		} else {
			return null;
		}
	}

	public String intToPaddedBinaryString(int original, int intendedLength, char padChar) {
		return padStrings(Integer.parseInt(Integer.toBinaryString(original)), intendedLength, padChar);
	}

	public String padStrings(int original, int intendedLength, char padChar) {
		String format = "%".concat(Integer.toString(intendedLength)).concat("d");
		return String.format(format, original).replace(' ', padChar);
	}

}
