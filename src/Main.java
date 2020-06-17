
import API.API;
import Interpreter.*;

import java.io.*;

public class Main {
    public static void main(String[] args) throws IOException {
        int argv = args.length;
        switch (argv) {
            case 0: {   // main process
                System.out.println("\u001B[32m" + "\n\t*** Welcome to MiniSQL System ***" + "\u001B[0m");
                try {   // initialization
                    API.Initialize();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    System.exit(-1);    // failed & quit
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                String input_string;
                while (Interpreter.GetState() != State.QUIT) {  // processing
                    if (Interpreter.GetState() == State.IDLE) System.out.print(" >> "); // wait for next query
                    else System.out.print("    ");                                      // wait for query tokens
                    input_string = br.readLine();                                       // read line
                    if (!input_string.equals("")) {
                        try {
                            input_string = Interpreter.ProcessInput(input_string);      // preprocessing input string
                            Interpreter.ReadInput(input_string);                        // process in interpreter
                        } catch (Exception e) {
                            Interpreter.SetState(State.IDLE);                           // reset query status
                            API.Clear();                                                // clear API temporary data
                            System.out.println("\033[1;31m" + e.getMessage() + "\033[0m");  // error prompt
                        }
                    }
                }
                try {
                    API.Store();    // store
                    System.out.println("\u001B[32m" + "\n\t*** Bye ***" + "\u001B[0m");
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    System.exit(-1);
                }
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
