import java.util.Map;

public class QueryValidator{

    public static void validate(Query q, BayesianNetwork bn){
        //validate query variable
        if(q.conditional) {
            Variable queryVar = bn.getVariableByName(q.queryVar);
            if(queryVar == null){
                throw new IllegalArgumentException("query variable '" + q.queryVar + "' does not exist in the network.");
            }

            if(!queryVar.outcomes.contains(q.queryValue)) {
                throw new IllegalArgumentException("value '" + q.queryValue + "' is not valid for query variable '" + q.queryVar + "'");
            }
        }

        //validate evidence variables
        for(Map.Entry<String, String> entry : q.evidence.entrySet()){
            String varName = entry.getKey();
            String value = entry.getValue();

            Variable v = bn.getVariableByName(varName);
            if(v == null) {
                throw new IllegalArgumentException("evidence variable '" + varName + "' does not exist in the network.");
            }

            if(!v.outcomes.contains(value)){
                throw new IllegalArgumentException("value '" + value + "' is not valid for variable '" + varName + "'");
            }
        }

        //check contradiction between query and evidence
        if(q.conditional && q.evidence.containsKey(q.queryVar)){
            String evidenceValue = q.evidence.get(q.queryVar);
            if(!evidenceValue.equals(q.queryValue)){
                throw new IllegalArgumentException("contradiction: query asks '" + q.queryVar + "=" + q.queryValue +
                        "' but evidence gives '" + q.queryVar + "=" + evidenceValue + "'");
            }
        }
    }
}
