import java.io.*;
import java.util.*;


public class VMManager {
	private static final int ST_SIZE = 512;
	private static final int PT_SIZE = 1024;
	private static final int PAGE_SIZE = 512;
	private static final int FRMAE_SIZE = 512;
	
	private static Integer[] ST = new Integer[512];
	private static Integer[] PM = new Integer[524288];
	private static Integer[] BitMap = new Integer[32];
	private static Integer[] MASK = new Integer[32];
	
	public static void initialize(String filename){
		try {
			initializeBitMap();
			initalizeMask();
			
			Scanner sc = new Scanner(new File(filename));
			if(sc.hasNext()){
				String STEntriesString = sc.next(); 
				System.out.println("STEntriesString is " + STEntriesString);
				initalizeWithSTEntries(STEntriesString);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void initializeBitMap(){
		BitMap[0] = 1;
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
	
	private static void updateST(int STIndex,int PTAddress){
		ST[STIndex] = PTAddress;
	}
	
	private static void initalizeWithSTEntries(String STEntriesString){
		String[] STEntriesPairs = STEntriesString.split("\\s+");
		
		//TODO: check whether the number of the ST is even
		
		for(int i=0;i<STEntriesPairs.length;i+=2){
			int STIndex = Integer.parseInt(STEntriesPairs[i]);
			int PTAddress = Integer.parseInt(STEntriesPairs[i+1]);
			updateST(STIndex,PTAddress);
		}
		
	}
	public static void main(String[] args){
		//TODO: check the validation of the arguments
		
		String initFilename = args[0];
		
		initialize(initFilename);
		
		
	}
}
