
import API.API;
import CatalogManager.CatalogManager;
import Interpreter.Interpreter;
import Interpreter.State;


import java.io.*;

public class Main {
    public static void main(String[] args) throws IOException {
        int argv = args.length;
        switch (argv) {
            case 0: {
                System.out.println("\u001B[32m" + "\n\t*** Welcome to MiniSQL System ***" + "\u001B[0m");
                try {
                    API.Initialize();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    System.exit(-1);
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                String input_string;
                while (Interpreter.GetState() != State.QUIT) {
                    if (Interpreter.GetState() == State.IDLE)
                        System.out.print(" >> ");
                    else
                        System.out.print("    ");

                    input_string = br.readLine();
                    if(!input_string.equals("")){
                        try {
                            input_string = Interpreter.ProcessInput(input_string);
                            Interpreter.ReadInput(input_string);
                        } catch (Exception e) {
                            Interpreter.SetState(State.IDLE);
                            API.Clear();
                            System.out.println(e.getMessage());
                        }
                    }
                }
                System.out.println("\u001B[32m" + "\n\t*** Bye ***" + "\u001B[0m");
            }
            break;
            case 1: {

            }
            break;
            default: {
                System.out.println("Invalid Arguments!");
            }
        }
    }
}
