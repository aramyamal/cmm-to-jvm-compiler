import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import cmm_grammar.*;
import typed_tree.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        String mode = "--compile"; // default mode
        String srcFile = null;
        String outputFile = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-i":
                case "--interpret":
                    mode = "--interpret";
                    break;
                case "-c":
                case "--compile":
                    mode = "--compile";
                    break;
                case "-o":
                case "--output":
                    if (i + 1 < args.length) {
                        outputFile = args[++i];
                    } else {
                        System.err.println("Error: Missing argument for -o/--output");
                        printUsageAndExit();
                    }
                    break;
                default:
                    if (srcFile == null) {
                        srcFile = args[i];
                    } else {
                        System.err.println("Error: Unexpected argument " + args[i]);
                        printUsageAndExit();
                    }
                    break;
            }
        }

        if (srcFile == null) {
            System.err.println("Error: No source file provided");
            printUsageAndExit();
        }

        // Validate source file
        if (!Files.exists(Paths.get(srcFile))) {
            System.err.println("Error: File not found - " + srcFile);
            System.exit(1);
        }

        // find output file name if none is given from input file
        if (outputFile == null) {
            Integer divider = srcFile.lastIndexOf('.');
            if (divider <= 0)
                outputFile = srcFile + ".j";
            else
                outputFile = srcFile.substring(0, divider) + ".j";
        }

        try {
            CharStream input = CharStreams.fromFileName(srcFile);
            cmmLexer lexer = new cmmLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            cmmParser parser = new cmmParser(tokens);
            cmmParser.ProgramContext cProgram = parser.program();

            TypedProgram tProgram = new TypeChecker().typecheck(cProgram);

            if (mode.equals("--interpret")) {
                new Interpreter().interpret(tProgram);
            } else if (mode.equals("--compile")) {
                compileAndWriteToFile(tProgram, outputFile);
            } else {
                printUsageAndExit();
            }
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

    private static void compileAndWriteToFile(TypedProgram tProgram, String outputFile) {
        try {
            String jtext = new Compiler().compile(outputFile, tProgram);
            PrintWriter writer = new PrintWriter(outputFile);
            writer.print(jtext);
            writer.close();
            System.out.println("Compiled successfully to " + outputFile);
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsageAndExit() {
        System.err.println("Usage:");
        System.err.println("  java Main [-i/--interpret | -c/--compile] " +
                "[-o/--output <output_file>] <source_file>");
        System.err.println("  java Main <SourceFile>    # Default is compile mode");
        System.exit(1);
    }
}
