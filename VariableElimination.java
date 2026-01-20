import java.util.*;

public class VariableElimination{

    //class to store the result of the query
    public static class Result {
        public double probability;
        public int addCount;
        public int mulCount;

        public Result(double probability, int addCount, int mulCount){
            this.probability = probability;
            this.addCount = addCount;
            this.mulCount = mulCount;
        }
    }

    public static Result run(Query q, BayesianNetwork bn){

        //System.out.println("starting Variable Elimination for query: " + q.originalQuery);

        int[] addCounter = new int[1]; // additions counter
        int[] mulCounter = new int[1]; // multiplications counter

        //keep only ancestors of query and evidence
        Set<String> relevantVariables = findRelevantVariables(q, bn);
//        System.out.println("-------------------------------------------------------------");
//        System.out.println("relevant variables for query " + q.queryVar + " and evidence " + q.evidence + ":");
        for(Variable varObj : bn.variables){ //loop over all variables in network
            String var = varObj.name;

//            if(relevantVariables.contains(var)){
//                System.out.println(var + " is relevant");
//            }else{
//                System.out.println(var + " is NOT relevant");
//            }
        }
        //System.out.println("-------------------------------------------------------------");

        //create factors only for relevant variables
        List<Factor> factors = new ArrayList<>();
        for(Variable var : bn.variables){ //loop over all variables in network
            if(relevantVariables.contains(var.name)) { //create only relevant
                //System.out.println("creating factor for variable: " + var.name);
                Factor factor = Factor.createFromVariable(var, q.evidence, bn);
                factors.add(factor);
            }
        }
        //System.out.println("initial factors created: " + factors.size());

        //remove fully assigned constant factors
        double constantMultiplier = 1.0;
        Iterator<Factor> iterator = factors.iterator();
        while (iterator.hasNext()){
            Factor f = iterator.next(); //take next factor
            boolean allInEvidence = true;
            for(String var : f.variables){
                if(!q.evidence.containsKey(var)){ //the factor is not completely defined.
                    allInEvidence = false;
                    break;
                }
            }
            if (allInEvidence) {
                Map.Entry<Map<String, String>, Double> entry = f.table.entrySet().iterator().next();
                constantMultiplier *= entry.getValue();
                iterator.remove();
                //System.out.println("removed constant factor (fully assigned by evidence), multiplied by: " + entry.getValue());
            }
        }

        //check elimination candidates
        Set<String> eliminationCandidates = new HashSet<>();

        for(String varName : relevantVariables){
            if(!varName.equals(q.queryVar) && !q.evidence.containsKey(varName)){
                eliminationCandidates.add(varName); //any variable that is not a query\evidence must be eliminated.
            }
        }

        //eliminate variables according to algorithm's number
        if(q.algoNum == 2){ //by alphabetical order
            List<String> eliminationOrder = new ArrayList<>(eliminationCandidates);
            Collections.sort(eliminationOrder); //sort by alphabetical order
            //System.out.println("elimination order (alphabetical): " + eliminationOrder);
            for (String varToEliminate : eliminationOrder){
                eliminateOneVariable(factors, varToEliminate, bn, addCounter, mulCounter, q); //doing elimination
            }
        } else if(q.algoNum == 3){ //by heuristic order
            //System.out.println("elimination order (heuristic by min factor size):");
            while (!eliminationCandidates.isEmpty()) {
                String varToEliminate = chooseNextVariable(factors, eliminationCandidates, bn); //current eliminated var
                //System.out.println("choosing to eliminate: " + varToEliminate);
                eliminationCandidates.remove(varToEliminate); //remove after treatment
                eliminateOneVariable(factors, varToEliminate, bn, addCounter, mulCounter, q); //doing elimination
            }
        }

        //multiply remaining factors
        Factor finalFactor = joinMultipleFactors(factors, bn, mulCounter, q.evidence);

//        System.out.println("final factor variables: " + finalFactor.variables);
//        System.out.println("final factor table: " + finalFactor.table);

        //initialization
        double numerator = 0.0;
        double denominator = 0.0;

        for(Map.Entry<Map<String, String>, Double> entry : finalFactor.table.entrySet()){//loop for each row in final factor
            if(entry.getKey().get(q.queryVar).equals(q.queryValue)){ //there is complete match
                numerator = entry.getValue();
            }
            denominator += entry.getValue();
        }

        double finalProbability;

        if(q.conditional && denominator != 0){ //conditions for normalization
            //System.out.println("normalizing result: numerator=" + numerator + ", denominator=" + denominator);
            addCounter[0]++; //1 + operation is required for normalization
            finalProbability = numerator / denominator;
        }else{ //not conditional
            finalProbability = numerator * constantMultiplier;
        }

        return new Result(finalProbability, addCounter[0], mulCounter[0]);
    }

    private static void eliminateOneVariable(List<Factor> factors, String varToEliminate, BayesianNetwork bn, int[] addCounter, int[] mulCounter, Query q){

        List<Factor> involvedFactors = new ArrayList<>(); //all factors that contains eliminated var

        Iterator<Factor> it = factors.iterator();
        while(it.hasNext()){
            Factor f = it.next();
            if(f.variables.contains(varToEliminate)){
                involvedFactors.add(f);
                it.remove();
            }
        }

        if(!involvedFactors.isEmpty()){ //we found factors that contains eliminated var
            Factor joined = joinMultipleFactors(involvedFactors, bn, mulCounter, q.evidence); //join on those factors
            Factor eliminated = eliminate(joined, varToEliminate, bn, addCounter, q.evidence); //new factor that doesn't contain the eliminated var
            factors.add(eliminated); //add the new one to remaining factors
        }
    }

    private static String chooseNextVariable(List<Factor> factors, Set<String> candidates, BayesianNetwork bn){

        String bestVar = null;
        int minSize = Integer.MAX_VALUE;

        for(String var : candidates){ //loop over all candidate variables
            Set<String> varsInvolved = new HashSet<>();
            for(Factor f : factors){
                if(f.variables.contains(var)){
                    varsInvolved.addAll(f.variables);
                }
            }
            varsInvolved.remove(var); //remove the variable we're going to eliminate

            //calculate the size of the factor after elimination
            int size = 1;
            for(String v : varsInvolved){
                Variable realVar = bn.getVariableByName(v);
                size *= realVar.outcomes.size();
            }

            //f the size we found now is better
            if(size < minSize){
                //update
                minSize = size;
                bestVar = var;
            }
        }
        return bestVar; //return the one that most worth eliminating
    }


    private static Set<String> findRelevantVariables(Query q, BayesianNetwork bn){

        Set<String> relevant = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        //query var is always relevant
        relevant.add(q.queryVar);
        queue.add(q.queryVar);

        //evidence var is always relevant
        for(String ev : q.evidence.keySet()){
            relevant.add(ev);
            queue.add(ev);
        }

        //finding previous ancestors of a query\evidence variable
        while(!queue.isEmpty()){
            String current = queue.poll(); //query\evidence var
            Variable var = bn.getVariableByName(current);
            for(String parent : var.parents){ //loop over all his parents
                if(!relevant.contains(parent)){
                    relevant.add(parent); //we will check also "Grandparents"
                    queue.add(parent);
                }
            }
        }

        return relevant;
    }


    //joining multiple factors (first. order by table size from small to large, then ASCII sum small to large)
    private static Factor joinMultipleFactors(List<Factor> factors, BayesianNetwork bn, int[] mulCounter, Map<String, String> evidence){

        while(factors.size() > 1){
            factors.sort((f1, f2) ->{
                if(f1.table.size() != f2.table.size()){
                    return Integer.compare(f1.table.size(), f2.table.size()); //by table size
                }else{
                    return asciiSum(f1.variables) - asciiSum(f2.variables); //tie brake - by ASCII sum
                }
            }
            );

            //remove the first two factors
            Factor f1 = factors.remove(0);
            Factor f2 = factors.remove(0);

            //join those 2 factors
            Factor joined = join(f1, f2, bn, mulCounter, evidence); //call helper function

            //add the new joined factor to factors list
            factors.add(joined);
        }
        return factors.get(0); //return the factor after all multiplication
    }

    //joining 2 factors
    public static Factor join(Factor f1, Factor f2, BayesianNetwork bn, int[] mulCounter, Map<String, String> evidence){
        List<String> allVars = new ArrayList<>();

        //the new factor will include a union of all the variables
        for(String var : f1.variables){
            if(!allVars.contains(var)){
                allVars.add(var);
            }
        }

        for(String var : f2.variables){
            if(!allVars.contains(var)){
                allVars.add(var);
            }
        }

//        System.out.println("joining factors:");
//        System.out.println("  factor 1 variables: " + f1.variables);
//        System.out.println("  factor 2 variables: " + f2.variables);
//        System.out.println("  result factor will have variables: " + allVars);

        Factor result = new Factor(allVars);

        List<Map<String, String>> allAssignments = generateAllAssignments(allVars, bn, evidence); //creating all possible assignment

        for(Map<String, String> assignment : allAssignments){ //loop over all assignment
            double p1 = f1.getProbabilityForAssignment(assignment);
            double p2 = f2.getProbabilityForAssignment(assignment);
            double product = p1 * p2;

            result.table.put(new LinkedHashMap<>(assignment), product); //save the multiplication result in the new factor

            mulCounter[0]++; //update multiplication counter
            //System.out.println("  assignment: " + assignment + ", p1=" + p1 + ", p2=" + p2 + ", product=" + product);
        }

//        System.out.println("finished joining. Total multiplications done: " + mulCounter[0]);
//        System.out.println("-------------------------------------------------------------");

        return result;
    }

    public static Factor eliminate(Factor f, String varToRemove, BayesianNetwork bn, int[] addCounter, Map<String, String> evidence){
        //System.out.println("eliminating variable: " + varToRemove + " from factor with variables: " + f.variables);

        //add all variables except the one we want to remove
        List<String> newVariables = new ArrayList<>();
        for(String var : f.variables){
            if(!var.equals(varToRemove)){
                newVariables.add(var);
            }
        }

        Factor result = new Factor(newVariables);

        List<Map<String, String>> allNewAssignments = generateAllAssignments(newVariables, bn, evidence); //create all possible assignment

        Variable var = bn.getVariableByName(varToRemove);

        for(Map<String, String> partialAssignment : allNewAssignments){

            double sum = 0.0;
            boolean first = true;

            for(String value : var.outcomes){ //loop over all outcomes of eliminated var
                Map<String, String> fullAssignment = new LinkedHashMap<>(partialAssignment);
                fullAssignment.put(varToRemove, value);

                double prob = f.getProbabilityForAssignment(fullAssignment);
                sum += prob;

                if(!first){
                    addCounter[0]++; //count + operations only from the second addition
                }

                first = false;

                //System.out.println("  summing assignment: " + fullAssignment + " -> prob=" + prob);
            }

            result.table.put(new LinkedHashMap<>(partialAssignment), sum); //put the result of the addition in the new factor table
            //System.out.println("result assignment after eliminating " + varToRemove + ": " + partialAssignment + " -> summed prob=" + sum);
        }

//        System.out.println("finished eliminating " + varToRemove + ". Total additions so far: " + addCounter[0]);
//        System.out.println("-------------------------------------------------------------");

        return result; //return the new factor without var to eliminate
    }

    //generate all possible assignment
    private static List<Map<String, String>> generateAllAssignments(List<String> variables, BayesianNetwork bn, Map<String, String> evidence) {
        List<Map<String, String>> result = new ArrayList<>();
        generateAllAssignmentsHelper(variables, 0, new LinkedHashMap<>(), bn, evidence, result);
        return result;
    }

    //recursive helper
    private static void generateAllAssignmentsHelper(List<String> variables, int index, Map<String, String> current, BayesianNetwork bn, Map<String, String> evidence, List<Map<String, String>> result){

        if(index == variables.size()){ //we went through all the variables

            if(isConsistent(current, evidence)){ //the assignment we created is consistent with the evidence
                result.add(new LinkedHashMap<>(current));
            }
            return;
        }

        String varName = variables.get(index); //current var
        Variable var = bn.getVariableByName(varName); //as a variable

        for(String outcome : var.outcomes){ //loop over all possible outcome
            current.put(varName, outcome); //fix possible value to current assignment
            generateAllAssignmentsHelper(variables, index + 1, current, bn, evidence, result); //recursive call to the next var
            current.remove(varName);
        }
    }

    public static boolean isConsistent(Map<String, String> assignment, Map<String, String> evidence){
        for(String key : evidence.keySet()){ //each var in evidence
            if(assignment.containsKey(key)){ //belongs to current factor
                if(!assignment.get(key).equals(evidence.get(key))){ //if there is a contradiction
                    return false;
                }
            }
        }
        return true;
    }

    //calculate ASCII sum of variables
    private static int asciiSum(List<String> vars) {
        int sum = 0;
        for (String var : vars) {
            for (char c : var.toCharArray()) {
                sum += (int) c;
            }
        }
        return sum;
    }
}
