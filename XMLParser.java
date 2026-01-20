import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class XMLParser{
    public static BayesianNetwork readXML(String fileName){
        BayesianNetwork network = new BayesianNetwork();

        try{
            //read the XML file from the project
            File file = new File(fileName);

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize(); //to delete extra spaces

            NodeList varNodes = doc.getElementsByTagName("VARIABLE");
            for(int i = 0; i < varNodes.getLength(); i++){
                Element varElement = (Element) varNodes.item(i);
                Variable var = new Variable();

                var.name = varElement.getElementsByTagName("NAME").item(0).getTextContent(); //saving the variable name

                NodeList outcomes = varElement.getElementsByTagName("OUTCOME");
                for(int j = 0; j < outcomes.getLength(); j++){
                    var.outcomes.add(outcomes.item(j).getTextContent()); //saving possible values
                }
                network.addVariable(var); //add variable to network
            }

            //reading cpt's
            NodeList defNodes = doc.getElementsByTagName("DEFINITION");
            for(int i =0; i < defNodes.getLength(); i++){
                Element defElement = (Element) defNodes.item(i);

                String varName = defElement.getElementsByTagName("FOR").item(0).getTextContent(); //relevant variable
                Variable var = network.getVariableByName(varName);

                NodeList givenList = defElement.getElementsByTagName("GIVEN");
                for (int j = 0; j < givenList.getLength(); j++){
                    var.parents.add(givenList.item(j).getTextContent()); //adding relevant parents
                }

                String tableText = defElement.getElementsByTagName("TABLE").item(0).getTextContent();
                String[] probs = tableText.trim().split("\\s+"); //split by spaces
                var.cpt = new double[probs.length];
                for(int j = 0; j < probs.length; j++){
                    var.cpt[j] = Double.parseDouble(probs[j]); //convert to double
                }
            }

        }catch(Exception e){
            System.out.println("error reading XML: " + e.getMessage());
        }
        return network;
    }
}
