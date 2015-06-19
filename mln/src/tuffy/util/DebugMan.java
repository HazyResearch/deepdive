package tuffy.util;
import java.util.Scanner;

/**
 * Container of methods for debugging purposes.
 */
public class DebugMan {
	
	private static StringBuilder log = new StringBuilder();

	public static void log(String s){
		log.append(s);
	}
	
	public static String getLog(){
		return log.toString();
	}
	
	public static void pause() {
		System.out.println("\nPress enter to continue...");
		Scanner in = new Scanner(System.in);
		in.nextLine();
	}
	
	public static boolean runningInWindows(){
		String os = System.getProperty("os.name").toLowerCase();
		return os.contains("win");
	}
	
	private static final Runtime s_runtime = Runtime.getRuntime ();
    public static void runGC() throws Exception
    {
        // It helps to call Runtime.gc()
        // using several method calls:
        for (int r = 0; r < 4; ++ r) _runGC ();
    }
    private static void _runGC () throws Exception
    {
        long usedMem1 = usedMemoryp (), usedMem2 = Long.MAX_VALUE;
        for (int i = 0; (usedMem1 < usedMem2) && (i < 500); ++ i)
        {
            s_runtime.runFinalization ();
            s_runtime.gc ();
            Thread.yield();
            
            usedMem2 = usedMem1;
            usedMem1 = usedMemoryp ();
        }
    }
    
    private static long usedMemoryp ()
    {
        return s_runtime.totalMemory () - s_runtime.freeMemory ();
    }
	
	public static long usedMemory(){
		try{
			runGC();
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
	    long mem0 = Runtime.getRuntime().totalMemory() -
	      Runtime.getRuntime().freeMemory();
	    return mem0;
	}
	
	static private long baseMem = 0;
	public static void checkBaseMem(){
		baseMem = usedMemory();
	}
	
	public static long getBaseMem(){
		return baseMem;
	}
	
	
	static private long peakMem = 0;
	public static void checkPeakMem(){
		long mem = usedMemory();
		if(mem > peakMem){
			peakMem = mem;
		}
	}
	
	public static long getPeakMem(){
		return peakMem;
	}

}
