import typed_tree.*;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Interpreter {

    static final Scanner scanner = new Scanner(System.in);

    public Optional<Value> runStm(Environment<Value> env,
            List<TypedDef.Func> availableFuncs, TypedStm stm) {

        return switch (stm) {

            case TypedStm.Exp s -> {
                runExp(env, availableFuncs, s.exp());
                yield Optional.empty();
            }

            case TypedStm.Decls s -> {
                if (s.type() == CType.INT) {
                    var declValue = new Value.Int(null);
                    for (String name : s.names()) {
                        env.extendVar(name, declValue);
                    }
                } else if (s.type() == CType.BOOL) {
                    var declValue = new Value.Bool(null);
                    for (String name : s.names()) {
                        env.extendVar(name, declValue);
                    }
                } else {
                    var declValue = new Value.Dubbel(null);
                    for (String name : s.names()) {
                        env.extendVar(name, declValue);
                    }
                }
                yield Optional.empty();
            }

            case TypedStm.Init s -> {
                env.extendVar(s.name(), runExp(env, availableFuncs,
                        s.exp()));
                yield Optional.empty();
            }

            case TypedStm.Return s -> {
                yield Optional.of(
                        runExp(env, availableFuncs,
                                s.exp()));
            }

            case TypedStm.While s -> {
                Optional<Value> retValue = Optional.empty();

                while (runExp(env, availableFuncs, s.exp()).toBool()
                        .value()) {

                    env.newContext();
                    retValue = runStm(env, availableFuncs, s.stm());
                    if (retValue.isPresent()) {
                        env.removeContext();
                        yield retValue;
                    }
                    env.removeContext();
                }
                yield Optional.empty();
            }

            case TypedStm.IfElse s -> {
                Optional<Value> stmReturn = Optional.empty();
                if (runExp(env, availableFuncs,
                        s.exp()).toBool().value()) {

                    env.newContext();
                    stmReturn = runStm(env, availableFuncs,
                            s.stm1());

                    env.removeContext();
                } else {
                    env.newContext();
                    stmReturn = runStm(env, availableFuncs,
                            s.stm2());

                    env.removeContext();
                }
                yield stmReturn;
            }

            case TypedStm.Block s -> {
                env.newContext();
                Optional<Value> retValue = Optional.empty();
                for (var blockStm : s.stms()) {
                    retValue = runStm(env, availableFuncs, blockStm);

                    if (retValue.isPresent()) {
                        env.removeContext();
                        yield retValue;
                    }
                }

                env.removeContext();
                yield Optional.empty();
            }

            default -> throw new RuntimeException("Statement " + stm +
                    "not implemented.");
        };
    }

    public Value runExp(Environment<Value> env,
            List<TypedDef.Func> availableFuncs, TypedExp exp) {

        return switch (exp) {

            case TypedExp.Paren e -> {
                yield runExp(env, availableFuncs, e.exp());
            }

            case TypedExp.BoolLit e -> {
                yield new Value.Bool(e.value());
            }

            case TypedExp.IntLit e -> {
                yield new Value.Int(e.value());
            }

            case TypedExp.DoubleLit e -> {
                yield new Value.Dubbel(e.value());
            }

            case TypedExp.Ident e -> {
                Value lookedUpValue = env.lookupVar(e.id());
                if (lookedUpValue.value() == null) {
                    throw new RuntimeException("Cannot evaluate unitialized " +
                            "variable " + e.id());
                }
                yield env.lookupVar(e.id());
            }

            case TypedExp.Func e -> {

                // handle input output functions first
                if (e.id().equals("readInt")) {
                    env.newContext();
                    Value.Int input = new Value.Int(scanner.nextInt());
                    env.removeContext();
                    yield input;
                }
                if (e.id().equals("readDouble")) {
                    env.newContext();
                    Value.Dubbel input = new Value.Dubbel(scanner.nextDouble());
                    env.removeContext();
                    yield input;
                }
                if (e.id().equals("printInt")) {
                    env.newContext();
                    var expressionValue = runExp(env, availableFuncs,
                            e.exps().getFirst()).value();

                    System.out.println(expressionValue);
                    env.removeContext();

                    yield new Value.Void();
                }
                if (e.id().equals("printDouble")) {
                    env.newContext();
                    var expressionValue = runExp(env, availableFuncs,
                            e.exps().getFirst()).toDubbel().value();

                    System.out.println(expressionValue);
                    env.removeContext();

                    yield new Value.Void();
                }

                // find the referenced function
                var referencedFunc = availableFuncs.stream()
                        .filter(function -> function.funcName()
                                .equals(e.id()))
                        .findFirst()
                        .get();

                // calculate each input expression
                LinkedHashMap<String, Value> inputs = new LinkedHashMap<>();
                int i = 0;
                for (TypedArg arg : referencedFunc.args()) {
                    if (!(arg instanceof TypedArg.Decl)) {
                        throw new RuntimeException(arg + "not yet implemented");
                    }
                    TypedArg.Decl inputParam = (TypedArg.Decl) arg;

                    var expressionValue = runExp(env, availableFuncs,
                            e.exps().get(i));
                    inputs.put(inputParam.id(), expressionValue);

                    ++i;
                }

                env.newContext();
                // extend the newly created evironment with the input variable
                // id and corresponding expression value
                for (var inputPair : inputs.entrySet()) {
                    env.extendVar(inputPair.getKey(), inputPair.getValue());
                }

                Optional<Value> returned = runFunction(env, availableFuncs,
                        referencedFunc);

                env.removeContext();

                yield returned.orElse(new Value.Void());

            }

            case TypedExp.Post e -> {
                String varName = e.id();
                Value idValue = env.lookupVar(e.id());
                Value newValue = e.operator() == Operator.INC
                        ? idValue.increment()
                        : idValue.decrement();
                env.assignVar(varName, newValue);
                yield idValue;
            }

            case TypedExp.Pre e -> {
                String varName = e.id();
                Value idValue = env.lookupVar(e.id());
                Value newValue = e.operator() == Operator.INC
                        ? idValue.increment()
                        : idValue.decrement(); // Operator.DEC
                env.assignVar(varName, newValue);
                yield newValue;
            }

            case TypedExp.Mul e -> {
                Value valueLhs = runExp(env, availableFuncs, e.exp1());
                Value valueRhs = runExp(env, availableFuncs, e.exp2());
                yield e.operator() == Operator.MUL
                        ? valueLhs.multiply(valueRhs)
                        : valueLhs.divide(valueRhs); // Operator.DIV
            }

            case TypedExp.Add e -> {
                Value valueLhs = runExp(env, availableFuncs, e.exp1());
                Value valueRhs = runExp(env, availableFuncs, e.exp2());
                yield e.operator() == Operator.ADD
                        ? valueLhs.add(valueRhs)
                        : valueLhs.subtract(valueRhs); // Operator.SUB
            }

            case TypedExp.Cmp e -> {
                Value valueLhs = runExp(env, availableFuncs,
                        e.exp1());
                Value valueRhs = runExp(env, availableFuncs,
                        e.exp2());

                if (valueLhs instanceof Value.Bool) {

                    var lhs = valueLhs.toBool().value();
                    var rhs = valueRhs.toBool().value();

                    yield switch (e.operator()) {

                        case Operator.EQU -> {
                            yield new Value.Bool(lhs == rhs);
                        }
                        case Operator.NEQ -> {
                            yield new Value.Bool(!(lhs == rhs));
                        }
                        default -> throw new RuntimeException("Case " 
                                + e.operator() + " not implemented for "
                                + "bool comparisons.");
                    };
                } else {
                    var lhs = valueLhs.toDubbel().value();
                    var rhs = valueRhs.toDubbel().value();

                    yield switch (e.operator()) {

                        case Operator.GTH -> {
                            yield new Value.Bool(lhs > rhs);
                        }
                        case Operator.LTH -> {
                            yield new Value.Bool(lhs < rhs);
                        }
                        case Operator.GTE -> {
                            yield new Value.Bool(lhs >= rhs);
                        }
                        case Operator.LTE -> {
                            yield new Value.Bool(lhs <= rhs);
                        }
                        case Operator.EQU -> {
                            yield new Value.Bool(lhs.equals(rhs));
                        }
                        case Operator.NEQ -> {
                            yield new Value.Bool(!lhs.equals(rhs));
                        }
                        default -> throw new RuntimeException("Case " 
                                + e.operator() + " not implemented for "
                                + "number comparisons.");
                    };
                }
            }

            case TypedExp.And e -> {
                Value.Bool valueLhs = runExp(
                        env,
                        availableFuncs,
                        e.exp1()).toBool();

                if (!valueLhs.value()) {
                    yield new Value.Bool(false); // short circuit
                }
                yield new Value.Bool(runExp(
                        env,
                        availableFuncs,
                        e.exp2()).toBool().value());
            }

            case TypedExp.Or e -> {
                Value.Bool valueLhs = runExp(
                        env,
                        availableFuncs,
                        e.exp1()).toBool();

                if (valueLhs.value()) {
                    yield new Value.Bool(true); // short circuit
                }
                yield new Value.Bool(runExp(
                        env,
                        availableFuncs,
                        e.exp2())
                        .toBool()
                        .value());
            }

            case TypedExp.Assign e -> {
                var expValue = runExp(env, availableFuncs, e.exp());
                env.assignVar(e.id(), expValue);
                yield expValue;
            }

            case TypedExp.Int2Double e -> {
                Value intValue = runExp(env, availableFuncs, e.exp());
                yield intValue.toDubbel();
            }

            default -> throw new RuntimeException("Expression " + exp
                    + "not implemented.");
        };
    }

    public Optional<Value> runFunction(Environment<Value> env,
            List<TypedDef.Func> availableFuncs,
            TypedDef.Func func) {

        var funcEnv = env.currentContextOnly();
        for (TypedStm stm : func.stms()) {
            Optional<Value> retValue = runStm(funcEnv, availableFuncs, stm);

            if (retValue.isPresent()) {
                return retValue;
            }
        }

        return Optional.empty();
    }

    public void interpret(TypedProgram p) {
        Environment<Value> env = new Environment<Value>();
        env.newContext();
        LinkedList<TypedDef> definitions = p.defintions();
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

        try {
            env.newContext();
            runFunction(env, functionsNotMain, mainFunction);
            env.removeContext();
            // rethrow exceptions as interpreter errors for testing
        } catch (TypeException e) {
            String message = e.getMessage();
            throw new RuntimeException(message);
        }
    }

}
