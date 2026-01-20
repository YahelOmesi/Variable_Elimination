import java.util.*;

public class JointProbability{

    public static class Result{
        public double probability;
        public int mulCount;

        public Result(double probability, int mulCount){ //constructor
            this.probability = probability;
            this.mulCount = mulCount;
        }

        //print by format
        public void print() {
            //System.out.printf("%.5f,0,%d%n", probability, mulCount);
        }
    }

    public static Result run(Query q, BayesianNetwork bn){

        Map<String, String> assignment = q.evidence; //take full assignment from query

        int mulCount = 0;
        double result = 1.0;
        boolean first = true; //check if this is a first multiplication

        //loop over all network variables
        for (Variable var : bn.variables)
        {
            String value = assignment.get(var.name); //value of current variable

            if (value == null){
                throw new RuntimeException("variable " + var.name + " is missing in assignment for joint query.");
            }

            double prob = getProbability(var, value, assignment, bn);//calculate probability considering parents
            result *= prob;

            if(!first){ //count * operations only after the first multiplication
                mulCount++;
            }
            first = false;

            //System.out.println("multiply: P(" + var.name + "=" + value + ") = " + prob);
        }

        return new Result(result, mulCount);
    }

    private static double getProbability(Variable var, String value, Map<String, String> assignment, BayesianNetwork bn){

        //follow position in probability array
        int index = 0;
        int base = 1;

        //loop from last parent to first
        for (int i = var.parents.size() - 1; i >= 0; i--){
            String parentName = var.parents.get(i);//parent's name
            Variable parentVar = bn.getVariableByName(parentName); //parent as a variable
            String val = assignment.get(parentName); //parent's assignment
            int pos = parentVar.outcomes.indexOf(val); //values position

            //update according to possible values
            index += pos * base;
            base *= parentVar.outcomes.size();
        }

        //updating index according to possible values
        int valIndex = var.outcomes.indexOf(value);
        index = index * var.outcomes.size() + valIndex;

        return var.cpt[index]; //return probability by index
    }
}
