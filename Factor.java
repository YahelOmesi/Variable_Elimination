import java.util.*;

public class Factor{

    public List<String> variables; //variables involved in current factor
    public Map<Map<String, String>, Double> table; // Assignment to corresponding probability

    public Factor(List<String> variables){ //constructor
        this.variables = new ArrayList<>(variables);
        this.table = new LinkedHashMap<>();
    }

    //create factor from variable, considering evidence
    public static Factor createFromVariable(Variable var, Map<String, String> evidence, BayesianNetwork bn){
        //System.out.println("building factor for variable: " +var.name);

        List<String> factorVars = new ArrayList<>(var.parents);//add var parents
        factorVars.add(var.name);// add current var

        Factor factor = new Factor(factorVars);

        List<Map<String, String>> allAssignments = generateAllAssignments(factorVars, var, evidence, bn);//considering all possible assignment

        for (Map<String, String> assignment : allAssignments){ //loop over possible assignment
            double prob = getProbability(var, assignment, bn);

            //System.out.println("assignment: " + assignment + ", probability: " + prob);

            if (VariableElimination.isConsistent(assignment, evidence)){ //add only if consistent with evidence
                factor.table.put(assignment, prob);
            }
        }

        return factor;
    }

    //generate all assignment consistent with the evidence
    private static List<Map<String, String>> generateAllAssignments(List<String> vars, Variable var, Map<String, String> evidence, BayesianNetwork bn){
        List<Map<String, String>> result = new ArrayList<>();
        generateAllAssignmentsHelper(vars, 0, new LinkedHashMap<>(), var, evidence, bn, result); //call recursive function
        return result;
    }

    //recursive helper to generate assignments
    private static void generateAllAssignmentsHelper(List<String> vars, int index, Map<String, String> current, Variable var, Map<String, String> evidence, BayesianNetwork bn, List<Map<String, String>> result){

        if (index == vars.size()){ //assignment completed
            result.add(new LinkedHashMap<>(current));
            return;
        }

        String varName = vars.get(index); //current var

        if (evidence.containsKey(varName)){ //evidence already exist, no need to check possible options
            current.put(varName, evidence.get(varName)); //fix given value
            generateAllAssignmentsHelper(vars, index + 1, current, var, evidence, bn, result); //recursive call to next var
            current.remove(varName);
        } else {
            //evidence doesnnt exist, iterate over all possible outcomes
            Variable networkVar = bn.getVariableByName(varName);
            for (String outcome : networkVar .outcomes){ //loop over all possible outcome
                current.put(varName, outcome); //fix possible outcome
                generateAllAssignmentsHelper(vars, index + 1, current, var, evidence, bn, result); //recursive call to next var
                current.remove(varName);
            }
        }
    }

    //get probability from CPT for given assignment
    private static double getProbability(Variable var, Map<String, String> assignment, BayesianNetwork bn){

        //follow position in probability array
        int index = 0;
        int base = 1;

        //loop from last parent to first
        for (int i = var.parents.size() - 1; i >= 0; i--){
            String parentName = var.parents.get(i); //parent's name
            Variable parentVar = bn.getVariableByName(parentName); //parent as a variable
            String value = assignment.get(parentName); //parent's assignment
            int pos = parentVar.outcomes.indexOf(value); //values position

            //update according to possible values
            index += pos * base;
            base *= parentVar.outcomes.size();
        }

        //add the index for the variable itself
        String varValue = assignment.get(var.name);

        //updating index according to possible values
        int varValueIndex = var.outcomes.indexOf(varValue);
        index = index * var.outcomes.size() + varValueIndex;

        double prob = var.cpt[index]; //find probability according to index
        //System.out.println("lookup CPT for assignment: " + assignment + ", CPT index: " + index + ", probability: " + prob);

        return prob;
    }

    //converts full assignment to assignment for factor variables only
    public double getProbabilityForAssignment(Map<String, String> fullAssignment){
        Map<String, String> projectedAssignment = new LinkedHashMap<>(); //store only relevant values

        for (String var : this.variables){
            projectedAssignment.put(var, fullAssignment.get(var));
        }
        return this.table.getOrDefault(projectedAssignment, 0.0);
    }
}
