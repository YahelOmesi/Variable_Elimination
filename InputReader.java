import java.util.*;
import java.io.*;

public class InputReader{
    public static List<String> readLines(String fileName){

        List<String> lines = new ArrayList<>();//store all lines from input

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))){

            String line;

            //read each line from file
            while ((line = reader.readLine()) != null){
                if (!line.trim().isEmpty()) {
                    lines.add(line.trim());
                }
            }
        } catch (IOException e){
            System.out.println("error reading input file: " + e.getMessage());
        }

        return lines; //returns all valid lines
    }
}