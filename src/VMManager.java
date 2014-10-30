import java.io.*;
import java.util.*;


public class VMManager {
	private static final int ST_SIZE = 512;
	private static final int PT_SIZE = 1024;
	private static final int PAGE_SIZE = 512;
	private static final int FRMAE_SIZE = 512;
	private static final int READ_OPERATION = 0;
	private static final int WRITE_OPERATION = 1;
	
//	private static Integer[] ST = new Integer[512];
	private static Integer[] PM = new Integer[524288];
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
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void readVA(int va){
		
	}
	
	private static void writeVA(int va){
		
	}
	
	private static void executeTranslation(int operationIndicator,int va){
		if(operationIndicator == READ_OPERATION){
			
		}else if(operationIndicator == WRITE_OPERATION){
			
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
