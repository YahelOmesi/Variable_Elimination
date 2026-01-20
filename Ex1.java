import java.io.IOException;
import java.io.BufferedWriter;
import java.util.List;
import java.io.FileWriter;


public class Ex1{
    public static void main(String[] args)
    {
        //read from input.txt
        List<String> inputLines = InputReader.readLines("input.txt");

        //stop the program if file is empty
        if(inputLines.isEmpty()){
            return;
        }

        String xmlFileName = inputLines.get(0); //name of XML file
        BayesianNetwork bn = XMLParser.readXML(xmlFileName);

//        System.out.println("----- CPTs for all variables -----");
//        for(Variable var : bn.variables){
//            var.printCPT();
//        }

        List<String> queries = inputLines.subList(1, inputLines.size()); //store al query in list

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"))){
            for (String queryLine : queries){ //loop over all queries
                try {
                    Query q = new Query(queryLine);
                    QueryValidator.validate(q, bn);

                    if(!q.conditional){ //full joint probability

                        JointProbability.Result res = JointProbability.run(q, bn);
                        res.print();

                        String formatted = String.format("%.5f,0,%d", res.probability, res.mulCount); //in full joint probability there is no additions operations
                        writer.write(formatted);
                        writer.newLine();

                    } else {
                        switch (q.algoNum) {
                            case 1: { //joint probability
                                SimpleInference.Result res = SimpleInference.run(q, bn);
                                String formatted = String.format("%.5f,%d,%d", res.probability, res.addCount, res.mulCount);
                                writer.write(formatted);
                                writer.newLine();
                                break;
                            }
                            case 2:
                            case 3: { //variable elimination
                                VariableElimination.Result res = VariableElimination.run(q, bn);
                                String formatted = String.format("%.5f,%d,%d", res.probability, res.addCount, res.mulCount);
                                writer.write(formatted);
                                writer.newLine();
                                break;
                            }
                            default: {
                                writer.write("0.00000,0,0");
                                writer.newLine();
                            }
                        }
                    }
                } catch (Exception e) {
                    writer.write("0.00000,0,0");
                    writer.newLine();
                    System.out.println("skipping invalid query: " + queryLine + " – " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("failed to write to output.txt: " + e.getMessage());
        }
    }
}
