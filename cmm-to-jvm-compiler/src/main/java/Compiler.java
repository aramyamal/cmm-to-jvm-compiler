import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Stream;

import typed_tree.*;

public class Compiler {

    private record VarEntry(CType type, Integer address) {
    }

    public class CompilerEnvironment extends Environment<VarEntry> {

        public CompilerEnvironment(String className) {
            this.className = className;
        }

        private String className;

        // keeping track of addresses for variables
        private TreeSet<Integer> removedAddresses = new TreeSet<>();
        private TreeSet<Integer> currentAddresses = new TreeSet<>();

        public Integer nextAddress(CType type) {
            Integer candidate;
            if (!removedAddresses.isEmpty()) {
                candidate = removedAddresses.pollFirst();
            } else {
                if (currentAddresses.isEmpty()) {
                    candidate = 0;
                } else {
                    candidate = currentAddresses.getLast() + 1;
                }
            }
            currentAddresses.add(candidate);
            if (type == CType.DOUBLE) {
                currentAddresses.add(candidate + 1);
            }
            return candidate;
        }

        public void removeAddress(CType type, Integer address) {
            if (currentAddresses.remove(address)) {
                removedAddresses.add(address);
                if (type == CType.DOUBLE) {
                    removeAddress(CType.VOID, address + 1);
                }
            }
        }

        // current maximum amount of variables
        private Integer maxVarAmount = 0;

        // current amount of variables
        private Integer currentVarAmount = 0;

        // current maximum stack size
        private Integer maxStackSize = 0;

        // current stack size
        private Integer currentStackSize = 0;

        // next label name
        private Integer nextLabel = 0;

        @Override
        public void extendVar(String variableName, VarEntry var) {
            super.extendVar(variableName, var);
            if (var.type() == CType.DOUBLE) {
                currentVarAmount = currentVarAmount + 2;
            } else {
                ++currentVarAmount;
            }
            if (currentVarAmount > maxVarAmount) {
                maxVarAmount = currentVarAmount;
            }
        }

        public void extendVar(String variableName, CType type) {
            VarEntry varEntry = new VarEntry(type, this.nextAddress(type));
            this.extendVar(variableName, varEntry);
        }

        @Override
        public void removeContext() {
            for (VarEntry entry : contexts.peek().values()) {
                if (entry.type() == CType.DOUBLE) {
                    currentVarAmount = currentVarAmount + 2;
                } else {
                    --currentVarAmount;
                }
                removeAddress(entry.type(), entry.address());
            }
            super.removeContext();
        }

        public void deltaStackSize(int change) {
            currentStackSize += change;
            if (currentStackSize > maxStackSize) {
                maxStackSize = currentStackSize;
            }
        }

        public void deltaStackSize(CType type, int change) {
            if (type == CType.INT || type == CType.BOOL) {
                this.deltaStackSize(change);
            } else if (type == CType.DOUBLE) {
                this.deltaStackSize(2 * change);
            } else { // (if CType.VOID)
                // do nothing
            }
        }

        public Integer maxVarAmount() {
            return maxVarAmount;
        }

        public Integer maxStackSize() {
            return maxStackSize;
        }

        public Integer currentStackSize() {
            return currentStackSize;
        }

        public String className() {
            return className;
        }

        public Integer nextLabelIncr() {
            return nextLabel++;
        }

        public Integer previousLabel() {
            return nextLabel - 1;
        }

    }

    public class InstructionBuilder {
        private LinkedList<JVMInstr> instructions;
        private CompilerEnvironment env;

        // constructor
        public InstructionBuilder(CompilerEnvironment environment) {
            this.instructions = new LinkedList<>();
            this.env = environment;
        }

        public void add(JVMInstr instr) {
            instructions.add(instr);
            this.updateEnv(instr);
        }

        public void addFirst(JVMInstr instr) {
            instructions.addFirst(instr);
        }

        public JVMInstr getLast() {
            return instructions.getLast();
        }

        public JVMInstr getLastNonVoid() {
            return instructions.reversed().stream()
                    .filter(instr -> !instr.type().equals(CType.VOID))
                    .findFirst()
                    .orElse(null);
        }

        public Stream<JVMInstr> stream() {
            return instructions.stream();
        }

        public String toBytecode() {
            StringBuilder stringBuilder = new StringBuilder();
            for (JVMInstr instr : instructions) {
                stringBuilder.append(instr.println());
            }
            return stringBuilder.toString();
        }

        private void updateEnv(JVMInstr instr) {
            switch (instr) {

                case JVMInstr.Invokestatic i -> {
                    for (CType varType : i.varTypes()) {
                        env.deltaStackSize(varType, -1);
                    }
                    env.deltaStackSize(i.type(), 1);
                }

                case JVMInstr.Store i -> env.deltaStackSize(i.type(), -1);

                case JVMInstr.Load i -> env.deltaStackSize(i.type(), 1);

                case JVMInstr.Push i -> env.deltaStackSize(i.type(), 1);

                case JVMInstr.Cmp i -> {
                    env.deltaStackSize(i.type(), -2);
                }

                case JVMInstr.Ifeq _ -> env.deltaStackSize(-1);

                case JVMInstr.Ifne _ -> env.deltaStackSize(-1);

                case JVMInstr.Add i -> env.deltaStackSize(i.type(), -1);

                case JVMInstr.Sub i -> env.deltaStackSize(i.type(), -1);

                case JVMInstr.Mul i -> env.deltaStackSize(i.type(), -1);

                case JVMInstr.Div i -> env.deltaStackSize(i.type(), -1);

                case JVMInstr.Dup i -> env.deltaStackSize(i.type(), 1);

                case JVMInstr.Pop i -> env.deltaStackSize(i.type(), -1);

                case JVMInstr.I2d _ -> env.deltaStackSize(1);

                default -> {
                    // assuming an instruction does not change stack size, it
                    // is not implemented as a case
                }
            }
        }

        public InstructionBuilder optimize() {
            // TODO implement optimizer
            return this;
        }
    }

    // compilation statement cases should leave the size of the stack unchanged
    public void compileStm(
            CompilerEnvironment env,
            InstructionBuilder instrs,
            TypedStm stm) {

        switch (stm) {
            case TypedStm.Exp s -> {
                CType type = s.exp().type();
                compileExp(env, instrs, s.exp());
                // pop the value of the expression so that all statements leave
                // the stack size unchanged, since expressions increase stack
                // size by 1 or 2 depending on its type
                if (type != CType.VOID) {
                    instrs.add(new JVMInstr.Pop(type));
                }
            }

            case TypedStm.Decls s -> {
                CType declType = s.type();
                for (String variableName : s.names()) {
                    env.extendVar(variableName, declType);
                }
            }

            case TypedStm.Init s -> {
                env.extendVar(s.name(), s.type());
                compileExp(env, instrs, s.exp());
                instrs.add(new JVMInstr.Store(
                        s.type(),
                        env.lookupVar(s.name()).address()));
            }

            case TypedStm.Return s -> {
                compileExp(env, instrs, s.exp());
                // handle return seperately
                if (env.currentFuncName().equals("main")) {
                    instrs.add(new JVMInstr.Return(CType.VOID));
                } else {
                    instrs.add(new JVMInstr.Return(s.exp().type()));
                }
            }

            case TypedStm.While s -> {
                // get needed labels
                Integer testLabel = env.nextLabelIncr();
                Integer endLabel = env.nextLabelIncr();
                instrs.add(new JVMInstr.Label(testLabel));

                // evaluating the boolean expression will leave 1 or 0 on stack
                compileExp(env, instrs, s.exp());

                instrs.add(new JVMInstr.Ifeq(endLabel));
                env.newContext();
                compileStm(env, instrs, s.stm());
                env.removeContext();
                instrs.add(new JVMInstr.Goto(testLabel));
                instrs.add(new JVMInstr.Label(endLabel));
            }

            case TypedStm.IfElse s -> {
                // get needed labels
                Integer falseLabel = env.nextLabelIncr();
                Integer trueLabel = env.nextLabelIncr();

                compileExp(env, instrs, s.exp());
                instrs.add(new JVMInstr.Ifeq(falseLabel));
                env.newContext();
                compileStm(env, instrs, s.stm1());
                env.removeContext();
                instrs.add(new JVMInstr.Goto(trueLabel));
                instrs.add(new JVMInstr.Label(falseLabel));
                env.newContext();
                compileStm(env, instrs, s.stm2());
                env.removeContext();
                instrs.add(new JVMInstr.Label(trueLabel));
            }

            case TypedStm.Block s -> {
                env.newContext();
                for (var statement : s.stms()) {
                    compileStm(env, instrs, statement);
                }
                env.removeContext();
            }

            default -> throw new RuntimeException(
                    "Statement " + stm.getClass() + " not yet implemented.");
        }
    }

    // compilation of each expression should either increase stack size by
    // 0, 1, or 2 depending on exp types void, int/bool, or double respectivaley
    public void compileExp(
            CompilerEnvironment env,
            InstructionBuilder instrs,
            TypedExp exp) {

        switch (exp) {
            case TypedExp.BoolLit e -> {
                if (e.value()) {
                    instrs.add(new JVMInstr.Push(CType.BOOL, true));
                } else {
                    instrs.add(new JVMInstr.Push(CType.BOOL, false));
                }
            }

            case TypedExp.IntLit e -> {
                instrs.add(new JVMInstr.Push(CType.INT, e.value()));
            }

            case TypedExp.DoubleLit e -> {
                instrs.add(new JVMInstr.Push(CType.DOUBLE, e.value()));
            }

            case TypedExp.Ident e -> {
                Integer address = env.lookupVar(e.id()).address();
                instrs.add(new JVMInstr.Load(e.type(), address));
            }

            case TypedExp.Func e -> {
                // handle input output functions first
                if (e.id().equals("readInt")) {
                    instrs.add(new JVMInstr.Invokestatic(
                            "readInt",
                            "Runtime",
                            new LinkedList<>(),
                            CType.INT));
                    return;
                }
                if (e.id().equals("readDouble")) {
                    instrs.add(new JVMInstr.Invokestatic(
                            "readDouble",
                            "Runtime",
                            new LinkedList<>(),
                            CType.DOUBLE));
                    return;
                }
                if (e.id().equals("printInt")) {
                    compileExp(env, instrs, e.exps().get(0));
                    instrs.add(new JVMInstr.Invokestatic(
                            "printInt",
                            "Runtime",
                            new LinkedList<>(List.of(CType.INT)),
                            CType.VOID));
                    return;
                }
                if (e.id().equals("printDouble")) {
                    compileExp(env, instrs, e.exps().get(0));
                    instrs.add(new JVMInstr.Invokestatic(
                            "printDouble",
                            "Runtime",
                            new LinkedList<>(List.of(CType.DOUBLE)),
                            CType.VOID));
                    return;
                }

                // for each expression, calculate it and and put it on top of
                // the stack. also add them to a newly created context and pop
                // that context after function call to keep track of limitlocals
                env.newContext();
                LinkedList<CType> inputTypes = new LinkedList<>();
                for (var inputExp : e.exps()) {
                    CType inputType = inputExp.type();
                    if (inputType != CType.VOID) {
                        inputTypes.add(inputType);
                        env.extendVar(UUID.randomUUID().toString(), inputType);
                    }
                    compileExp(env, instrs, inputExp);
                }
                env.removeContext();

                instrs.add(new JVMInstr.Invokestatic(
                        e.id(),
                        env.className(),
                        inputTypes,
                        e.type()));
            }

            case TypedExp.Post e -> {
                var varEntry = env.lookupVar(e.id());
                CType type = varEntry.type();
                Integer address = varEntry.address();
                instrs.add(new JVMInstr.Load(type, address));
                instrs.add(new JVMInstr.Dup(instrs.getLastNonVoid().type()));
                instrs.add(new JVMInstr.Push(type, 1));
                if (e.operator() == Operator.INC) {
                    instrs.add(new JVMInstr.Add(type));
                } else { // Operator.DEC
                    instrs.add(new JVMInstr.Sub(type));
                }
                instrs.add(new JVMInstr.Store(type, address));
            }

            case TypedExp.Pre e -> {
                var varEntry = env.lookupVar(e.id());
                CType type = varEntry.type();
                Integer address = varEntry.address();
                instrs.add(new JVMInstr.Load(type, address));
                instrs.add(new JVMInstr.Push(type, 1));
                if (e.operator() == Operator.INC) {
                    instrs.add(new JVMInstr.Add(type));
                } else { // Operator.DEC
                    instrs.add(new JVMInstr.Sub(type));
                }
                instrs.add(new JVMInstr.Dup(instrs.getLastNonVoid().type()));
                instrs.add(new JVMInstr.Store(type, address));
            }

            case TypedExp.Mul e -> {
                compileExp(env, instrs, e.exp1());
                compileExp(env, instrs, e.exp2());
                if (e.operator() == Operator.MUL) {
                    instrs.add(new JVMInstr.Mul(e.type()));
                } else { // Operator.DIV
                    instrs.add(new JVMInstr.Div(e.type()));
                }
            }

            case TypedExp.Add e -> {
                compileExp(env, instrs, e.exp1());
                compileExp(env, instrs, e.exp2());
                if (e.operator() == Operator.ADD) {
                    instrs.add(new JVMInstr.Add(e.type()));
                } else { // Operator.SUB
                    instrs.add(new JVMInstr.Sub(e.type()));
                }
            }

            case TypedExp.Cmp e -> {
                Integer label = env.nextLabelIncr();

                instrs.add(new JVMInstr.Push(CType.BOOL, true));
                compileExp(env, instrs, e.exp1());
                compileExp(env, instrs, e.exp2());
                instrs.add(new JVMInstr.Cmp(
                        e.exp1().type(),
                        e.operator(),
                        label));
                instrs.add(new JVMInstr.Pop(instrs.getLastNonVoid().type()));
                instrs.add(new JVMInstr.Push(CType.BOOL, false));
                instrs.add(new JVMInstr.Label(label));
            }

            case TypedExp.Or e -> {
                Integer trueLabel = env.nextLabelIncr();
                Integer falseLabel = env.nextLabelIncr();

                compileExp(env, instrs, e.exp1());
                instrs.add(new JVMInstr.Ifne(trueLabel));
                compileExp(env, instrs, e.exp2());
                instrs.add(new JVMInstr.Ifne(trueLabel));
                instrs.add(new JVMInstr.Push(CType.BOOL, false));
                instrs.add(new JVMInstr.Goto(falseLabel));
                instrs.add(new JVMInstr.Label(trueLabel));
                instrs.add(new JVMInstr.Push(CType.BOOL, true));
                instrs.add(new JVMInstr.Label(falseLabel));
            }

            case TypedExp.And e -> {
                Integer trueLabel = env.nextLabelIncr();
                Integer falseLabel = env.nextLabelIncr();

                compileExp(env, instrs, e.exp1());
                instrs.add(new JVMInstr.Ifeq(falseLabel));
                compileExp(env, instrs, e.exp2());
                instrs.add(new JVMInstr.Ifeq(falseLabel));
                instrs.add(new JVMInstr.Push(CType.BOOL, true));
                instrs.add(new JVMInstr.Goto(trueLabel));
                instrs.add(new JVMInstr.Label(falseLabel));
                instrs.add(new JVMInstr.Push(CType.BOOL, false));
                instrs.add(new JVMInstr.Label(trueLabel));
            }

            case TypedExp.Assign e -> {
                var varEntry = env.lookupVar(e.id());
                compileExp(env, instrs, e.exp());
                instrs.add(new JVMInstr.Dup(instrs.getLastNonVoid().type()));
                instrs.add(new JVMInstr.Store(e.type(), varEntry.address()));
            }

            case TypedExp.Int2Double e -> {
                compileExp(env, instrs, e.exp());
                instrs.add(new JVMInstr.I2d());
            }

            default -> throw new RuntimeException(
                    "Expression " + exp.getClass() + " not yet implemented.");
        }
    }

    public String compileFunc(TypedDef.Func function, String className) {

        CompilerEnvironment env = new CompilerEnvironment(className);
        env.setCurrentFunc(function.funcName());
        env.newContext();

        InstructionBuilder instructions = new InstructionBuilder(env);

        // add function input variables to the environment and assign their
        // bytecode addresses
        if (function.funcName().equals("main")) {
            // since String is not yet implemented we will assume that main has
            // input argument of type Void in the environment and that it will
            // never be used
            env.extendVar("args", new VarEntry(
                    CType.VOID,
                    env.nextAddress(CType.VOID)));
        } else {
            for (var arg : function.args()) {
                env.extendVar(arg.id(), new VarEntry(
                        arg.type(),
                        env.nextAddress(arg.type())));
            }
        }

        // compile the statments in the function
        for (var stm : function.stms()) {
            compileStm(env, instructions, stm);
        }

        // handle method definition of main and other functions separately
        if (function.funcName().equals("main")) {
            instructions.addFirst(new JVMInstr.DotmethodMain(
                    env.maxVarAmount(),
                    env.maxStackSize()));

        } else { // functions not main
            // extract format of inputs
            LinkedList<CType> parameterCTypes = new LinkedList<>();
            for (var parameter : function.args()) {
                if (parameter.type()  != CType.VOID) {
                    parameterCTypes.add(parameter.type());
                } else {
                    throw new RuntimeException(parameter.getClass()
                            + "not yet implemented.");
                }
            }
            instructions.addFirst(new JVMInstr.Dotmethod(
                    function.funcName(),
                    parameterCTypes,
                    function.returns(),
                    env.maxVarAmount(),
                    env.maxStackSize()));
        }

        // if no return is given yet, or last instructions is not a return,
        // add a return at the last line for the function
        boolean hasReturnInstruction = instructions.stream()
                .anyMatch(instr -> instr instanceof JVMInstr.Return);
        if (!hasReturnInstruction ||
                !(instructions.getLast() instanceof JVMInstr.Return)) {
            instructions.add(new JVMInstr.Return(CType.VOID));
        }

        // end method instruction
        instructions.add(new JVMInstr.DotEndMethod());

        return instructions.optimize().toBytecode();
    }

    //// the output of the compiler.
    StringBuilder output = new StringBuilder();

    //// name should be just the class name without file extension.
    public String compile(String name, TypedProgram program) {

        LinkedList<TypedDef> definitions = program.defintions();
        // find main function
        var mainFunction = definitions.stream()
                .filter(func -> ((TypedDef.Func) func)
                        .funcName()
                        .equals("main"))
                .findFirst()
                .map(function -> (TypedDef.Func) function)
                .get();

        // get all other functions that are not main
        var functionsNotMain = definitions.stream()
                .filter(func -> !((TypedDef.Func) func)
                        .funcName()
                        .equals("main"))
                .map(function -> (TypedDef.Func) function)
                .toList();

        String mainBytecode = compileFunc(mainFunction, name);
        List<String> notMainBytecodes = functionsNotMain
                .stream()
                .map(function -> compileFunc(function, name))
                .toList();

        //// output boilerplate.
        println(".class public " + name);
        println(".super java/lang/Object");
        println();
        println(".method public <init>()V");
        println(".limit locals 1");
        println();
        println("\taload_0");
        println("\tinvokespecial java/lang/Object/<init>()V");
        println("\treturn");
        println();
        println(".end method");
        println();

        output.append(mainBytecode);
        for (String bytecode : notMainBytecodes) {
            output.append(bytecode);
        }
        System.out.println(output); // turn on for debugging
        return output.toString();
    }

    //// Auxiliary functions for producing output.
    private void println() {
        println("");
    }

    private void println(String s) {
        output.append(s);
        output.append('\n');
    }
}
