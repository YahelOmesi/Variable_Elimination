import java.util.ArrayList;

public class Variable{
    public String name; //var name
    public ArrayList<String> parents = new ArrayList<>();
    public ArrayList<String> outcomes = new ArrayList<>(); //possible values for var
    public double[] cpt; //probability for each combination


//    public void printCPT(){
//        System.out.println("Variable: " + name);
//        if (!parents.isEmpty()) {
//            System.out.println("Parents: " + parents);
//        } else {
//            System.out.println("No parents");
//        }
//        System.out.println("Outcomes: " + outcomes);
//        System.out.print("CPT: ");
//        for (double p : cpt) {
//            System.out.print(p + " ");
//        }
//        System.out.println("\n----------------------");
//    }


}

