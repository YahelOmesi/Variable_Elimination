import java.util.ArrayList;

public class BayesianNetwork{

    public ArrayList<Variable> variables = new ArrayList<>(); //store network variable
    public void addVariable(Variable var){
        variables.add(var);
    }
    public Variable getVariableByName(String name){
        for(Variable var : variables){
            if(var.name.equals(name)){
                return var;
            }
        }
        return null;
    }

}

