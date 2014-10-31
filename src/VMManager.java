import java.io.*;
import java.util.*;

public class VMManager {
	private static final int ST_SIZE = 512;
	private static final int PT_SIZE = 1024;
	private static final int PAGE_SIZE = 512;
	private static final int FRAME_SIZE = 512;
	private static final int READ_OPERATION = 0;
	private static final int WRITE_OPERATION = 1;
	private static final String OUTPUT_SEPARATOR = " ";

	private static int[] PM;
	private static Integer[] BitMap;
	private static Integer[] MASK;
	private static LinkedList<TLBEntry> TLB;
	private static String output;

	private static class TLBEntry {
		private Integer sp;
		private Integer frameIndex;

		TLBEntry(int sp, int frameIndex) {
			this.sp = sp;
			this.frameIndex = frameIndex;
		}
	}

	private static void updateTLB(int sp, int frameIndex) {

		TLBEntry entry = searchTLB(sp);
		if (entry != null) {
			removeTLBEntry(entry);
			putEntryAsMostRecentlyVisited(entry);
		} else {
			entry = new TLBEntry(sp, frameIndex);
			if (TLB.size() >= 4) {
				removeLRU();
			}
			putEntryAsMostRecentlyVisited(entry);
		}
	}

	private static boolean removeTLBEntry(TLBEntry entry) {
		return TLB.remove(entry);
	}

	private static void putEntryAsMostRecentlyVisited(TLBEntry entry) {
		TLB.add(entry);
	}

	private static void removeLRU() {
		TLB.removeFirst();
	}

	private static TLBEntry searchTLB(int sp) {
		ListIterator<TLBEntry> listIterator = TLB.listIterator();
		while (listIterator.hasNext()) {
			TLBEntry entry = listIterator.next();
			if (entry.sp == sp) {
				return entry;
			}
		}

		return null;
	}
	
	private static void resetVariables(){
		PM = new int[524288];
		BitMap = new Integer[32];
		MASK = new Integer[32];
		TLB = new LinkedList<TLBEntry>();
		output = "";
	}

	public static void initialize(String filename) {
		try {
			resetVariables();
			
			initalizeMask();

			initializeBitMap();
			
			Scanner sc = new Scanner(new File(filename));
			if (sc.hasNextLine()) {
				String STEntriesString = sc.nextLine();
				System.out.println("STEntriesString is " + STEntriesString);
				initializeWithSTEntries(STEntriesString);
			}

			if (sc.hasNextLine()) {
				String PTEntriesString = sc.nextLine();
				System.out.println("PTEntriesString is " + PTEntriesString);
				initializeWithPTEntries(PTEntriesString);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static void initializeBitMap() {
		BitMap[0] = MASK[0];
	}

	private static void initalizeMask() {
		MASK[31] = 1;
		for (int i = 30; i >= 0; i--) {
			MASK[i] = MASK[i + 1] << 1;
		}
	}

	// Return the frame index in the Physical Memory
	private static int findNextEmptyFrame() {

		for (int i = 0; i < 32; i++) {
			for (int j = 0; j < 32; j++) {
				int test = BitMap[i] & MASK[j];
				if (test == 0) {
					return i * 32 + j;
				}
			}
		}

		return -1;
	}

	private static int findNextTwoEmptyFrames() {
		for (int i = 0; i < 32; i++) {
			for (int j = 0; j < 32; j++) {

				// Step 1: test whether the current bit is empty or not
				int test = BitMap[i] & MASK[j];
				if (test == 0) {
					// Step 2: test whether the next bit (and of cource check
					// whether the next bit exists or not) is empty or not.
					if (j + 1 <= 31) {
						int testNext = BitMap[i] & MASK[j + 1];
						if (testNext == 0) {
							return i * 32 + j;
						}
					} else if (i <= 30) {
						int testNext = BitMap[i + 1] & MASK[0];
						if (testNext == 0) {
							return i * 32 + j;
						}
					}
				}
			}
		}

		// TODO: what if we did not find two consecutive empty slots?
		return -1;
	}

	private static void freeBitMap(int frameIndex) {
		int row = frameIndex / 32;
		int col = frameIndex % 32;

		BitMap[row] = BitMap[row] & (~MASK[col]);
	}

	private static void occupyBitMap(int frameIndex) {
		int row = frameIndex / 32;
		int col = frameIndex % 32;

		BitMap[row] = BitMap[row] | MASK[col];
	}

	private static void updateST(int STIndex, int PTAddress) {
		PM[STIndex] = PTAddress;

		int frameIndex = PTAddress / 512;
		occupyBitMap(frameIndex);
		occupyBitMap(frameIndex + 1);
	}

	private static int getPTAddress(int STIndex) {
		return PM[STIndex];
	}

	private static void updatePT(int PTAddress, int PTIndex, int PGAddress) {
		PM[PTAddress + PTIndex] = PGAddress;

		int frameIndex = PGAddress / 512;
		occupyBitMap(frameIndex);
	}

	private static void initializeWithSTEntries(String STEntriesString) {
		String[] STEntries = STEntriesString.split("\\s+");

		// TODO: check whether the number of the ST is even

		for (int i = 0; i < STEntries.length; i += 2) {
			int STIndex = Integer.parseInt(STEntries[i]);
			int PTAddress = Integer.parseInt(STEntries[i + 1]);
			updateST(STIndex, PTAddress);
		}
	}

	private static void initializeWithPTEntries(String PTEntriesString) {
		String[] PTEntries = PTEntriesString.split("\\s+");

		// TODO: check whether the number of the ST is a multiple of 3

		for (int i = 0; i < PTEntries.length; i += 3) {
			int PTIndex = Integer.parseInt(PTEntries[i]);
			int STIndex = Integer.parseInt(PTEntries[i + 1]);
			int PGAddress = Integer.parseInt(PTEntries[i + 2]);

			int PTAddress = getPTAddress(STIndex);
			updatePT(PTAddress, PTIndex, PGAddress);
		}
	}

	private static void translate(String VAFilename, boolean withTLB) {
		try {
			output = "";
			Scanner sc = new Scanner(new File(VAFilename));

			if (sc.hasNextLine()) {
				String VAEntriesString = sc.nextLine();

				String[] VAEntries = VAEntriesString.split("\\s+");
				for (int i = 0; i < VAEntries.length; i += 2) {
					int operationIndicator = Integer.parseInt(VAEntries[i]);
					int va = Integer.parseInt(VAEntries[i + 1]);
					executeTranslation(operationIndicator, va, withTLB);
				}
			}
			
			output = output.trim();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void writeToFile(String filename){
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			writer.write(output);
	    	writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void readVA(int va, boolean withTLB) {
		
		String TLBPrefix;
		if (withTLB == true) {
			TLBPrefix = "m" + OUTPUT_SEPARATOR;
		} else {
			TLBPrefix = "";
		}

		int STIndex = parseSTIndex(va);
		int PTIndex = parsePTIndex(va);
		int PGIndex = parsePGIndex(va);

		int PTAddress = accessST(STIndex);
		if (PTAddress == -1) {
			output += "pf" + OUTPUT_SEPARATOR;
		} else if (PTAddress == 0) {
			output += "err" + OUTPUT_SEPARATOR;
		} else if (PTAddress > 0) {
			int PGAddress = accessPT(PTAddress, PTIndex);
			if (PGAddress == -1) {
				output += "pf" + OUTPUT_SEPARATOR;
			} else if (PGAddress == 0) {
				output += "err" + OUTPUT_SEPARATOR;
			} else if (PGAddress > 0) {
				int sp = STIndex << 9 + PTIndex;
				int frameIndex = PGAddress / FRAME_SIZE;
				updateTLB(sp, frameIndex);
				output += TLBPrefix + Integer.toString(PGAddress + PGIndex) + OUTPUT_SEPARATOR;
			}
		}

	}

	private static void writeVA(int va, boolean withTLB) {
		System.out.println("Write VA " + va);

		String TLBPrefix;
		if (withTLB == true) {
			TLBPrefix = "m" + OUTPUT_SEPARATOR;
		} else {
			TLBPrefix = "";
		}

		int STIndex = parseSTIndex(va);
		int PTIndex = parsePTIndex(va);
		int PGIndex = parsePGIndex(va);
		int sp = STIndex << 9 + PTIndex;

		int PTAddress = accessST(STIndex);
		if (PTAddress == -1) {
			output += "pf" + OUTPUT_SEPARATOR;
		} else if (PTAddress == 0) {
			int PTFirstFrame = findNextTwoEmptyFrames();
			int newPTAddress = PTFirstFrame * FRAME_SIZE;
			updateST(STIndex, PTFirstFrame * FRAME_SIZE);

			int PGFrame = findNextEmptyFrame();
			int newPGAddress = PGFrame * FRAME_SIZE;
			updatePT(newPTAddress, PTIndex, newPGAddress);

			updateTLB(sp, PGFrame);

			output += TLBPrefix + Integer.toString(newPGAddress + PGIndex) + OUTPUT_SEPARATOR;
		} else if (PTAddress > 0) {
			int PGAddress = accessPT(PTAddress, PTIndex);
			if (PGAddress == -1) {
				output += "pf" + OUTPUT_SEPARATOR;
			} else if (PGAddress == 0) {
				int PGFrame = findNextEmptyFrame();
				int newPGAddress = PGFrame * FRAME_SIZE;
				updatePT(PTAddress, PTIndex, newPGAddress);

				updateTLB(sp, PGFrame);

				output += TLBPrefix + Integer.toString(newPGAddress + PGIndex)
						+ OUTPUT_SEPARATOR;
			} else if (PGAddress > 0) {
				int frameIndex = PGAddress / FRAME_SIZE;
				updateTLB(sp, frameIndex);
				output += TLBPrefix + Integer.toString(PGAddress + PGIndex) + OUTPUT_SEPARATOR;
			}
		}
	}

	private static int accessST(int STIndex) {
		return PM[STIndex];
	}

	private static int accessPT(int PTAddress, int PTIndex) {
		return PM[PTAddress + PTIndex];
	}

	private static int parseSTIndex(int va) {
		return (va & 0x0FF80000) >> 19;
	}

	private static int parsePTIndex(int va) {
		return (va & 0x0007FE00) >> 9;
	}

	private static int parsePGIndex(int va) {
		return va & 0x000001FF;
	}

	private static void executeTranslation(int operationIndicator, int va,
			boolean withTLB) {
		int STIndex = parseSTIndex(va);
		int PTIndex = parsePTIndex(va);
		int PGIndex = parsePGIndex(va);

		int sp = STIndex << 9 + PTIndex;
		TLBEntry entry = searchTLB(sp);

		if (withTLB == true) {
			if (entry != null) {
				int frameIndex = entry.frameIndex;
				int PA = frameIndex * FRAME_SIZE + PGIndex;
				output += "h" + OUTPUT_SEPARATOR + Integer.toString(PA) + OUTPUT_SEPARATOR;
				updateTLB(sp, frameIndex);
			} else {
				if (operationIndicator == READ_OPERATION) {
					readVA(va, withTLB);
				} else if (operationIndicator == WRITE_OPERATION) {
					writeVA(va, withTLB);
				}
			}
		} else {
			if (operationIndicator == READ_OPERATION) {
				readVA(va, withTLB);
			} else if (operationIndicator == WRITE_OPERATION) {
				writeVA(va, withTLB);
			}
		}

	}

	public static void main(String[] args){
		//TODO: check the validation of the arguments
		String initFilename = "";
		String VAFilename = "";
		String outputFilenameWithoutTLB = "";
		String outputFilenameWithTLB = "";
		
		if(args.length != 4){
			System.out.println("You should specify all four args: initFilename, " +
					"VAFilename, outputFilenameWithoutTLB, outputFilenameWithTLB." +
					"otherwise, by default we will have input1.txt, input2.txt, nnn1.txt, nnn2.txt");
			
			initFilename = "input1.txt";
			VAFilename = "input2.txt";
			outputFilenameWithoutTLB = "nnn1.txt";
			outputFilenameWithTLB = "nnn2.txt";
		}else{
			initFilename = args[0];
			VAFilename = args[1];
			outputFilenameWithoutTLB = args[2];
			outputFilenameWithTLB = args[3];
		}
		
		initialize(initFilename);
		translate(VAFilename,false);
		writeToFile(outputFilenameWithoutTLB);
		
		initialize(initFilename);
		translate(VAFilename,true);
		writeToFile(outputFilenameWithTLB);		
	}
}
