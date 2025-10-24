package soplanificacion;

import java.io.*;

public class ConfigIO {
    private static final String FILE = "config.json";

    public static void save(int cycleMs){
        String json = "{ \"cycleMs\": "+cycleMs+" }";
        try (FileWriter fw = new FileWriter(FILE)){ fw.write(json); }
        catch(IOException ignored){}
    }

    public static int loadCycleMs(){
        File f = new File(FILE);
        if (!f.exists()) return 1000;
        try(BufferedReader br = new BufferedReader(new FileReader(f))){
            String s = br.readLine();
            int i = s.indexOf("\"cycleMs\"");
            if (i<0) return 1000;
            int c = s.indexOf(":", i);
            int e = s.indexOf("}", c);
            String num = s.substring(c+1, e).trim();
            return Integer.parseInt(num);
        } catch (Exception e){ return 1000; }
    }
}