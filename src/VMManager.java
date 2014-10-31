import java.io.*;
import java.util.*;


public class VMManager {
	private static final int ST_SIZE = 512;
	private static final int PT_SIZE = 1024;
	private static final int PAGE_SIZE = 512;
	private static final int FRAME_SIZE = 512;
	private static final int READ_OPERATION = 0;
	private static final int WRITE_OPERATION = 1;
	
//	private static Integer[] ST = new Integer[512];
	private static int[] PM = new int[524288];
	private static Integer[] BitMap = new Integer[32];
	private static Integer[] MASK = new Integer[32];
	
	public static void initialize(String filename){
		try {
			initalizeMask();

			initializeBitMap();
			
			Scanner sc = new Scanner(new File(filename));
			if(sc.hasNextLine()){
				String STEntriesString = sc.nextLine(); 
				System.out.println("STEntriesString is " + STEntriesString);
				initializeWithSTEntries(STEntriesString);
			}
			
			if(sc.hasNextLine()){
				String PTEntriesString = sc.nextLine();
				System.out.println("PTEntriesString is " + PTEntriesString);
				initializeWithPTEntries(PTEntriesString);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void initializeBitMap(){
		BitMap[0] = MASK[0];
	}
	
	private static void initalizeMask(){
		MASK[31] = 1;
		for(int i=30;i>=0;i--){
			MASK[i] = MASK[i+1] << 1;
			System.out.println(Integer.toBinaryString(MASK[i]));
		}
	}
	
	//Return the frame index in the Physical Memory
	private static int findNextEmptyFrame(){
		
		for(int i=0;i<32;i++){
			for(int j=0;j<32;j++){
				int test = BitMap[i] & MASK[j];
				if(test == 0){
					return i * 32 + j;
				}
			}
		}
		
		//TODO: what if we did not find an empty slots?
		return -1;
	}
	
	private static int findNextTwoEmptyFrames(){
		for(int i=0;i<32;i++){
			for(int j=0;j<32;j++){
				
				//Step 1: test whether the current bit is empty or not
				int test = BitMap[i] & MASK[j];
				if(test == 0){
					//Step 2: test whether the next bit (and of cource check
					//whether the next bit exists or not) is empty or not.
					if(j+1 <= 31) {
						int testNext = BitMap[i] & MASK[j+1];
						if(testNext == 0){
							return i * 32 + j;
						}
					}else if(i <= 30){
						int testNext = BitMap[i+1] & MASK[0];
						if(testNext == 0){
							return i * 32 + j;
						}
					}
				}
			}
		}
		
		
		//TODO: what if we did not find two consecutive empty slots?
		return -1;
	}
	
	private static void freeBitMap(int frameIndex){
		int row = frameIndex / 32;
		int col = frameIndex % 32;
		
		BitMap[row] = BitMap[row] & (~MASK[col]);
	}
	
	private static void occupyBitMap(int frameIndex){
		int row = frameIndex / 32;
		int col = frameIndex % 32;
		
		BitMap[row] = BitMap[row] | MASK[col];
	}
	
	private static void updateST(int STIndex,int PTAddress){
		PM[STIndex] = PTAddress;
		
		int frameIndex = PTAddress/512;
		occupyBitMap(frameIndex);
		occupyBitMap(frameIndex+1);
	}
	
	private static int getPTAddress(int STIndex){
		return PM[STIndex];
	}
	
	private static void updatePT(int PTAddress,int PTIndex, int PGAddress){
		PM[PTAddress+PTIndex] = PGAddress;
		
		int frameIndex = PGAddress/512;
		occupyBitMap(frameIndex);
	}
		
	private static void initializeWithSTEntries(String STEntriesString){
		String[] STEntries = STEntriesString.split("\\s+");
		
		//TODO: check whether the number of the ST is even
		
		for(int i=0;i<STEntries.length;i+=2){
			int STIndex = Integer.parseInt(STEntries[i]);
			int PTAddress = Integer.parseInt(STEntries[i+1]);
			updateST(STIndex,PTAddress);
		}
	}
	
	private static void initializeWithPTEntries(String PTEntriesString){
		String[] PTEntries = PTEntriesString.split("\\s+");
		
		//TODO: check whether the number of the ST is a multiple of 3
		
		for(int i=0;i<PTEntries.length;i+=3){
			int PTIndex = Integer.parseInt(PTEntries[i]);
			int STIndex = Integer.parseInt(PTEntries[i+1]);
			int PGAddress = Integer.parseInt(PTEntries[i+2]);
			
			int PTAddress = getPTAddress(STIndex);
			updatePT(PTAddress,PTIndex,PGAddress);
		}
	}
	
	private static void translate(String VAFilename){
		try {
			Scanner sc = new Scanner(new File(VAFilename));
			
			if(sc.hasNextLine()){
				String VAEntriesString = sc.nextLine();
				
				String[] VAEntries = VAEntriesString.split("\\s+");
				for(int i=0;i<VAEntries.length; i+=2){
					int operationIndicator = Integer.parseInt(VAEntries[i]);
					int va = Integer.parseInt(VAEntries[i+1]);
					executeTranslation(operationIndicator,va);
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void readVA(int va){
		System.out.println("Read VA" + va);
		System.out.println(Integer.toBinaryString(va));
		System.out.println(Integer.toBinaryString(parseSTIndex(va)));
		System.out.println(Integer.toBinaryString(parsePTIndex(va)));
		System.out.println(Integer.toBinaryString(parsePGIndex(va)));
		
		int STIndex = parseSTIndex(va);
		int PTIndex = parsePTIndex(va);
		int PGIndex = parsePGIndex(va);
		
		int PTAddress = accessST(STIndex);
		if(PTAddress == -1){
			System.out.println("pf");
		}else if(PTAddress == 0){
			System.out.println("err");
		}else if(PTAddress > 0){
			int PGAddress = accessPT(PTAddress,PTIndex);
			if(PGAddress == -1){
				System.out.println("pf");
			}else if(PGAddress == 0){
				System.out.println("err");
			}else if(PGAddress > 0){
				System.out.println(PGAddress + PGIndex);
			}
		}
		
	}
	
	private static void writeVA(int va){
		System.out.println("Write VA " + va);
		System.out.println(Integer.toBinaryString(va));
		System.out.println(Integer.toBinaryString(parseSTIndex(va)));
		System.out.println(Integer.toBinaryString(parsePTIndex(va)));
		System.out.println(Integer.toBinaryString(parsePGIndex(va)));
		
		int STIndex = parseSTIndex(va);
		int PTIndex = parsePTIndex(va);
		int PGIndex = parsePGIndex(va);
		
		int PTAddress = accessST(STIndex);
		if(PTAddress == -1){
			System.out.println("pf");
		}else if(PTAddress == 0){
			int PTFirstFrame = findNextTwoEmptyFrames();
			int newPTAddress = PTFirstFrame * FRAME_SIZE;
			updateST(STIndex,PTFirstFrame * FRAME_SIZE);
			
			int PGFrame = findNextEmptyFrame();
			int newPGAddress = PGFrame * FRAME_SIZE;
			updatePT(newPTAddress,PTIndex,newPGAddress);
			
			System.out.println(newPGAddress);
		}else if(PTAddress > 0){
			int PGAddress = accessPT(PTAddress,PTIndex);
			if(PGAddress == -1){
				System.out.println("pf");
			}else if(PGAddress == 0){
				int PGFrame = findNextEmptyFrame();
				int newPGAddress = PGFrame * FRAME_SIZE;
				updatePT(PTAddress,PTIndex,newPGAddress);
				
				System.out.println(newPGAddress);
			}else if(PGAddress > 0){
				System.out.println(PGAddress + PGIndex);
			}
		}
	}
	
	private static int accessST(int STIndex){
		return PM[STIndex];
	}
	
	private static int accessPT(int PTAddress,int PTIndex){
		return PM[PTAddress + PTIndex];
	}
	
	private static int parseSTIndex(int va){
		return (va & 0x0FF80000) >> 19;
	}
	
	private static int parsePTIndex(int va){
		return (va & 0x0007FE00) >> 9;
	}
	
	private static int parsePGIndex(int va){
		return va & 0x000001FF;
	}
	
	private static void executeTranslation(int operationIndicator,int va){
		if(operationIndicator == READ_OPERATION){
			readVA(va);
		}else if(operationIndicator == WRITE_OPERATION){
			writeVA(va);
		}
	}
	public static void main(String[] args){
		//TODO: check the validation of the arguments
		
		String initFilename = args[0];
		String VAFilename = args[1];
		
		initialize(initFilename);
		
		translate(VAFilename);
	}
}
