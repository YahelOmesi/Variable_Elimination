import java.util.*;

public class Query{
    public String originalQuery;
    public int algoNum; //1- simple inference, 2 - variable elimination(by abc), 3 - variable elimination(by heuristic)
    public boolean conditional;
    public String queryVar;
    public String queryValue;
    public Map<String, String> evidence = new LinkedHashMap<>();

    public Query(String line){ //constructor
        this.originalQuery = line; //keep the original query

        //every query must contain ()
        if (!line.contains("(") || !line.contains(")")){
            throw new IllegalArgumentException("invalid query format: missing parentheses");
        }

        //extracting content from ()
        String inside = line.substring(line.indexOf('(') + 1, line.indexOf(')'));

        //check if there is an algorithm number after the ()
        int afterParen = line.indexOf(')') + 1;
        if (afterParen < line.length() && line.charAt(afterParen) == ','){
            String algoStr = line.substring(afterParen + 1).trim();

            if (algoStr.matches("[123]")){
                this.algoNum = Integer.parseInt(algoStr);
            } else {
                throw new IllegalArgumentException("invalid algorithm number: " + algoStr);
            }
        } else {
            this.algoNum = 1; //there is no , after (). means joint probability
        }

        if (inside.contains("|")){ //conditional query
            conditional = true;

            //split query to 2 parts, left: query var, right: evidences
            String[] condParts = inside.split("\\|");
            if (condParts.length != 2){
                throw new IllegalArgumentException("invalid conditional query format.");
            }

            //left - query var
            String[] q = condParts[0].trim().split("=");

            if (q.length != 2){ //must be 2 parts
                throw new IllegalArgumentException("invalid query variable format.");
            }

            queryVar = q[0].trim(); //query name var
            queryValue = q[1].trim(); //var's value

            //right - evidences
            String[] evidences = condParts[1].split(",");
            for (String evid : evidences){ //each evidence is split into a variable and a value
                String[] pair = evid.trim().split("=");

                if (pair.length != 2) continue; //must be 2 parts
                evidence.put(pair[0].trim(), pair[1].trim());
            }

        } else{//if it is not conditional this is joint probability (1)
            conditional = false;

            String[] allAssignments = inside.split(","); //store all assignment
            for (String part : allAssignments){
                String[] pair = part.trim().split("=");
                if (pair.length != 2) continue;
                evidence.put(pair[0].trim(), pair[1].trim());
            }
        }
    }

}
