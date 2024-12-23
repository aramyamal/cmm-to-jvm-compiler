import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import cmm_grammar.*;
import typed_tree.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public String getGreeting() {
        return "Hello World!";
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Error: No file provided.");
            System.exit(1);
        }

        String filePath = args[0];
        if (!Files.exists(Paths.get(filePath))) {
            System.err.println("Error: File not found - " + filePath);
            System.exit(1);
        }

        try {
            CharStream input = CharStreams.fromFileName(filePath);
            cmmLexer lexer = new cmmLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            cmmParser parser = new cmmParser(tokens);
            cmmParser.ProgramContext t = parser.program();
            TypedProgram tProgram = new TypeChecker().typecheck(t);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        } catch (TypeException e) {
            System.out.println("TYPE ERROR");
            System.err.println(e.toString());
            System.exit(1);
        } catch (RuntimeException e) {
            System.out.println("INTERPRETER ERROR");
            System.err.println(e.toString());
            System.exit(-1);
        } catch (Throwable e) {
            System.out.println("SYNTAX ERROR");
            System.err.println(e.toString());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
