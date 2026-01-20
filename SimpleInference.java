import java.util.*;

public class SimpleInference{

    public static class Result{
        public double probability;
        public int addCount;// + operation count
        public int mulCount;// * operation count

        public Result(double probability, int addCount, int mulCount){ //constructor
            this.probability = probability;
            this.addCount = addCount;
            this.mulCount = mulCount;
        }
    }

    public static Result run(Query q, BayesianNetwork bn){

        //initial counters
        int addCount = 0;
        int mulCount = 0;


        Map<String, String> baseAssignment = new LinkedHashMap<>(q.evidence); //assignment of known variables
        baseAssignment.put(q.queryVar, q.queryValue); //query variable

        //unassigned variables
        List<String> freeVars = findFreeVariables(baseAssignment, bn);
        List<Assignment> allAssignments = generateAllAssignments(freeVars, bn); //all possible combinations

        Map<Assignment, ComputeResult> computedNumeratorTerms = new HashMap<>(); //keep track of computed terms

        double numeratorSum = 0.0;
        boolean firstNumeratorTerm = true; //check to track # additions

        //calculating numerator
        for (Assignment assign : allAssignments){
            Map<String, String> fullAssign = new LinkedHashMap<>(baseAssignment);
            fullAssign.putAll(assign.map); //create full assignment

            ComputeResult res = computeProbability(fullAssign, bn);
            computedNumeratorTerms.put(assign, res); //insert computed term

            //update counters
            numeratorSum += res.probability;
            mulCount += res.mulCount;

            if (!firstNumeratorTerm){ //+ is not considered in first term
                addCount++;
            }
            firstNumeratorTerm = false;

            //System.out.println("[numerator] Term: " + assign.map + ", prob=" + res.probability + ", mul=" + res.mulCount);
        }
        //System.out.println("numerator sum = " + numeratorSum);

        //if the query variable is already in evidence, directly return the result.
        if (q.evidence.containsKey(q.queryVar)){
            //System.out.println("query variable already in evidence. No normalization needed.");
            return new Result(numeratorSum, addCount, mulCount);
        }

        //denominator
        Variable queryVariable = bn.getVariableByName(q.queryVar);

        double denominatorExtraSum = 0.0;
        boolean firstDenominatorNewTerm = true; //whether this is the first new term in denominator

        for (String outcome : queryVariable.outcomes){
            if (outcome.equals(q.queryValue)){
                continue; //skip value that's already being calculated in the numerator
            }

            Map<String, String> evidencePlusOutcome = new LinkedHashMap<>(q.evidence);
            evidencePlusOutcome.put(q.queryVar, outcome);

            List<String> freeVarsDenominator = findFreeVariables(evidencePlusOutcome, bn); //finding unassigned variables
            List<Assignment> assignmentsDenominator = generateAllAssignments(freeVarsDenominator, bn);

            //calculating each new term in denominator
            for (Assignment assign : assignmentsDenominator){
                Map<String, String> fullAssign = new LinkedHashMap<>(evidencePlusOutcome);
                fullAssign.putAll(assign.map); //creating full assignment

                ComputeResult res = computeProbability(fullAssign, bn);

                //update counters
                denominatorExtraSum += res.probability;
                mulCount += res.mulCount;

                if (!firstDenominatorNewTerm){
                    addCount++; //+ is not considered in first term
                }
                firstDenominatorNewTerm = false;

                //System.out.println("[denominator - Extra] Term: " + assign.map + ", prob=" + res.probability + ", mul=" + res.mulCount);
            }
        }

        //add the numerator amount to the denominator amount
        double denominator = numeratorSum + denominatorExtraSum;
        if (denominatorExtraSum != 0.0){
            addCount++; //this required one + operation.
        }

        //System.out.println("denominator = " + denominator);

        double finalProb = numeratorSum / denominator; //final probability

        //System.out.println("final Probability = " + finalProb);

        return new Result(finalProb, addCount, mulCount);
    }

    // returns a list of unassigned variables
    private static List<String> findFreeVariables(Map<String, String> assignment, BayesianNetwork bn){
        List<String> freeVars = new ArrayList<>();
        for (Variable var : bn.variables){
            if (!assignment.containsKey(var.name)){
                freeVars.add(var.name);
            }
        }
        return freeVars;
    }

    //creates all combinations for assigning the free variables.
    private static List<Assignment> generateAllAssignments(List<String> vars, BayesianNetwork bn){
        List<Assignment> result = new ArrayList<>();
        generateAllAssignmentsHelper(vars, 0, new LinkedHashMap<>(), bn, result);
        return result;
    }

    //recursive helper function - create all combinations
    private static void generateAllAssignmentsHelper(List<String> vars, int index, Map<String, String> current, BayesianNetwork bn, List<Assignment> result){

        if (index == vars.size()){ //stop after going through all variables
            result.add(new Assignment(new LinkedHashMap<>(current)));
            return;
        }

        String varName = vars.get(index); //next variable
        Variable var = bn.getVariableByName(varName);

        for (String outcome : var.outcomes){ //for every possible value of var
            current.put(varName, outcome);
            generateAllAssignmentsHelper(vars, index + 1, current, bn, result); //recursive call, to call the next variable
            current.remove(varName); //removing a value to check for another value later
        }
    }

    private static class ComputeResult{
        double probability;
        int mulCount;

        ComputeResult(double probability, int mulCount){ //constructor
            this.probability = probability;
            this.mulCount = mulCount;
        }
    }


    //calculates the overall probability of full assignment
    private static ComputeResult computeProbability(Map<String, String> assignment, BayesianNetwork bn){

        double result = 1.0;
        int localMulCount = 0;
        boolean first = true; //flag to know when to start counting *

        //loop over all variables
        for (Variable var : bn.variables){

            String value = assignment.get(var.name);
            if (value == null){ //if there is no value for var we will skip him
                continue;
            }
            double prob = getProbability(var, value, assignment, bn);
            //System.out.println("P(" + var.name + "=" + value + ") = " + prob);

            result *= prob;

            if (!first){ //counting * operation (only if it is not the first operation)
                localMulCount++;
            }
            first = false;
        }

        //System.out.println("product = " + result + ", mulCount in Term = " + localMulCount);
        return new ComputeResult(result, localMulCount);
    }

    //returns the probability value from the variable's CPT table.
    private static double getProbability(Variable var, String value, Map<String, String> assignment, BayesianNetwork bn){
        int index = 0;
        int base = 1;

        //loop over all parents (last to first)
        for (int i = var.parents.size() - 1; i >= 0; i--){
            String parentName = var.parents.get(i); //parent name
            Variable parentVar = bn.getVariableByName(parentName); //parent variable
            String val = assignment.get(parentName); //parent's value
            int pos = parentVar.outcomes.indexOf(val); //position of the parent's value in its outcomes list


            int addition = pos * base;
            index = index + addition;
            int parentOutcomeCount = parentVar.outcomes.size(); //# possible parent's values
            base = base * parentOutcomeCount; //update base for the next parent

        }

        int valIndex = var.outcomes.indexOf(value);
        index = index * var.outcomes.size() + valIndex;

        return var.cpt[index];
    }

    private static class Assignment{
        Map<String, String> map;

        Assignment(Map<String, String> map){ //constructor
            this.map = map;
        }

    }
}
