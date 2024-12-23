import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.List;

import cmm_grammar.cmmParser;
import typed_tree.*;

public class TypeChecker {

    class TypeCheckerEnvironment extends Environment<CType> {

        public record Signature(
                CType returns,
                LinkedHashMap<String, CType> parameters) {
        }

        public LinkedHashMap<String, Signature> signs = new LinkedHashMap<>();

        public void addSimpleFunction(String name,
                CType returnObject,
                CType paramObject) {
            LinkedHashMap<String, CType> parameters = new LinkedHashMap<>();
            if (paramObject != null) {
                parameters.put("param", paramObject);
            }
            Signature signature = new Signature(returnObject, parameters);
            signs.put(name, signature);
        }

        public void newContextCurrentFunc() {
            this.contexts.push(this.currentFuncParameters());
        }

        public CType currrentReturnType() {
            return signs.get(currentFuncName).returns();
        }

        public LinkedHashMap<String, CType> currentFuncParameters() {
            return new LinkedHashMap<String, CType>(
                    signs.get(currentFuncName).parameters());
        }

        @Override
        public TypeCheckerEnvironment currentContextOnly() {
            TypeCheckerEnvironment scopeCopy = new TypeCheckerEnvironment();
            if (!contexts.isEmpty()) {
                scopeCopy.contexts.push(new LinkedHashMap<>(contexts.peek()));
            }
            scopeCopy.signs = new LinkedHashMap<>(this.signs);
            scopeCopy.currentFuncName = this.currentFuncName;
            return scopeCopy;
        }

        public TypeCheckerEnvironment.Signature lookupFunc(String funcName) {
            if (this.signs.containsKey(funcName)) {
                return this.signs.get(funcName);
            } else {
                throw new TypeException("Function " + funcName
                        + " has not yet been defined.");
            }
        }

        public void extendFunc(String funcName,
                TypeCheckerEnvironment.Signature funcSignature) {

            if (this.signs.containsKey(funcName)) {
                throw new TypeException("Function with name " + funcName
                        + " already declared.");
            }
            this.signs.put(funcName, funcSignature);
            for (String parameterName : funcSignature.parameters().keySet()) {
                this.extendVar(parameterName, funcSignature
                        .parameters().get(parameterName));
            }
        }

    }

    public TypedStm checkStm(TypeCheckerEnvironment env,
            cmmParser.StmContext stm) {
        return switch (stm) {
            case cmmParser.ExpStmContext s -> {
                TypedExp inferredExp = inferExp(env, s.exp());
                yield new TypedStm.TypedSExp(
                        inferredExp.type(),
                        inferredExp);
            }

            case cmmParser.DeclsStmContext s -> {
                CType cType = toCType(s.type());
                if (cType == CType.VOID) {
                    throw new TypeException("Variable cannot be of type void.");
                }
                LinkedList<String> nameList = new LinkedList<>();
                for (var id : s.Ident()) {
                    String varName = id.getText();
                    if ((env.currentContextOnly().existsVar(varName))) {
                        throw new TypeException("Variable with name " + varName
                                + " already declared in current scope.");
                    }
                    env.extendVar(varName, cType);
                    nameList.add(varName);
                }
                yield new TypedStm.TypedSDecls(
                        cType,
                        nameList);
            }

            case cmmParser.InitStmContext s -> {
                String varName = s.Ident().getText();
                if (env.currentContextOnly().existsVar(varName)) {

                    throw new TypeException("Variable with name " + varName
                            + " already declared in current scope.");
                }

                CType cType = toCType(s.type());
                env.extendVar(varName, cType);
                TypedExp typedExp = inferExp(env, s.exp());
                if (cType == CType.VOID) {
                    throw new TypeException("Variable can not be of type void "
                            + "in initialization.");
                }
                if (!isConvertible(cType, typedExp.type())) {
                    throw new TypeException("Illegal conversion in " +
                            "initializaton. Expected type " + cType +
                            ", but got " + typedExp.type() + " instead.");
                }

                yield new TypedStm.TypedSInit(
                        cType,
                        varName,
                        promoteExp(typedExp, cType));
            }

            case cmmParser.ReturnStmContext s -> {
                TypedExp typedRetExp = inferExp(env, s.exp());
                CType retType = env.currrentReturnType();
                if (isConvertible(retType, typedRetExp.type())) {
                    yield new TypedStm.TypedSReturn(promoteExp(
                            typedRetExp,
                            env.currrentReturnType()));
                } else {
                    throw new TypeException("Illegal conversion in return. "
                            + "Expected " + typedRetExp.type() + ", but got "
                            + retType + " instead");
                }
            }

            case cmmParser.WhileStmContext s -> {
                TypedExp inferredExp = inferExp(env, s.exp());
                if (inferredExp.type() != CType.BOOL) {
                    throw new TypeException(
                            "Expression in 'while' must have type bool.");
                }
                env.newContext();
                TypedStm typedStm = checkStm(env, s.stm());
                env.removeContext();
                yield new TypedStm.TypedSWhile(inferredExp, typedStm);

            }

            case cmmParser.BlockStmContext s -> {
                env.newContext();
                LinkedList<TypedStm> typedStmList = new LinkedList<>();
                for (cmmParser.StmContext statement : s.stm()) {
                    typedStmList.add(checkStm(env, statement));
                }
                env.removeContext();
                yield new TypedStm.TypedSBlock(typedStmList);
            }

            case cmmParser.IfElseStmContext s -> {
                TypedExp inferredExp = inferExp(env, s.exp());
                if (inferredExp.type() != CType.BOOL) {
                    throw new TypeException(
                            "If Else expression must have type bool.");
                }
                // create separate scopes for the if and else so that they
                // don't leak
                env.newContext();
                TypedStm typedStmIf = checkStm(env, s.stm(0));
                env.removeContext();
                env.newContext();
                TypedStm typedStmElse = checkStm(env, s.stm(1));
                env.removeContext();

                yield new TypedStm.TypedSIfElse(
                        inferredExp,
                        typedStmIf,
                        typedStmElse);
            }

            default -> throw new IllegalStateException("Case for " + stm
                    + " is not yet implemented.");
        };
    }

    public TypedExp inferExp(TypeCheckerEnvironment env,
            cmmParser.ExpContext exp) {

        return switch (exp) {
            case cmmParser.BoolExpContext e -> {
                if (e.boolLit() instanceof cmmParser.FalseLitContext) {
                    yield new TypedExp.TypedEBool(true);
                } else {
                    yield new TypedExp.TypedEBool(false);
                }
            }

            case cmmParser.IntExpContext e -> {
                yield new TypedExp.TypedEInt(
                        Integer.parseInt(e.Integer().getText()));
            }

            case cmmParser.DoubleExpContext e -> {
                yield new TypedExp.TypedEDouble(
                        Double.parseDouble(e.Double().getText()));
            }

            case cmmParser.IdentExpContext e -> {
                String varName = e.Ident().getText();
                // typechecking done in lookupVar
                yield new TypedExp.TypedEId(varName, env.lookupVar(varName));
            }

            // check if function is defined before it is called and that the
            // call is correct
            case cmmParser.AppExpContext e -> {
                String functionName = e.Ident().getText();
                // check if the function signature is in the environment
                TypeCheckerEnvironment.Signature envSign = env.lookupFunc(
                        functionName);
                LinkedList<CType> signCTypeList = new LinkedList<>(
                        envSign.parameters().values());

                // construct list of typed expressions
                LinkedList<TypedExp> typedExpList = new LinkedList<>();
                LinkedList<CType> varCTypes = new LinkedList<>();

                // infer and collect argument types
                for (var expression : e.exp()) {
                    TypedExp inferredExp = inferExp(env, expression);
                    varCTypes.add(inferredExp.type());
                    typedExpList.add(inferredExp);
                }

                // check if the number of arguments matches function signature
                if (signCTypeList.size() != varCTypes.size() &&
                        !envSign.parameters().isEmpty()) {
                    throw new TypeException("Function '" + functionName
                            + "' called with incorrect number of arguments.");
                }

                // verify and promote argument types
                for (int i = 0; i < signCTypeList.size(); i++) {
                    CType expected = signCTypeList.get(i);
                    CType actual = varCTypes.get(i);

                    if (!isConvertible(expected, actual)) {
                        throw new TypeException("Argument " + (i + 1)
                                + " of function '" + functionName
                                + "' has incompatible type. Expected "
                                + expected + " but got " + actual + ".");
                    }

                    // promote expression if needed
                    typedExpList.set(i,
                            promoteExp(typedExpList.get(i), expected));
                }

                yield new TypedExp.TypedEApp(
                        functionName,
                        typedExpList,
                        envSign.returns());
            }

            case cmmParser.PostExpContext e -> {
                String varName = e.Ident().getText();
                CType inferredType = env.lookupVar(varName);
                if (inferredType != CType.INT && inferredType != CType.DOUBLE) {
                    throw new TypeException("Increment/decrement can only be "
                            + "done on Int or Double");
                }
                // get operator type
                Operator operator = switch (e.incDecOp()) {
                    case cmmParser.IncContext _ -> {
                        yield Operator.INC;
                    }

                    case cmmParser.DecContext _ -> {
                        yield Operator.DEC;
                    }

                    default -> {
                        throw new TypeException("Operator " + e.incDecOp()
                                + "not yet implemented for suffix incr.drecr");
                    }
                };

                yield new TypedExp.TypedEPost(
                        varName,
                        inferredType,
                        operator);
            }

            case cmmParser.PreExpContext e -> {
                String varName = e.Ident().getText();
                CType inferredType = env.lookupVar(varName);
                if (inferredType != CType.INT && inferredType != CType.DOUBLE) {
                    throw new TypeException("Increment/decrement can only be "
                            + "done on Int or Double");
                }
                // get operator type
                Operator operator = switch (e.incDecOp()) {
                    case cmmParser.IncContext _ -> {
                        yield Operator.INC;
                    }

                    case cmmParser.DecContext _ -> {
                        yield Operator.DEC;
                    }

                    default -> {
                        throw new TypeException("Operator " + e.incDecOp()
                                + "not yet implemented for prefix incr/decr");
                    }
                };

                yield new TypedExp.TypedEPre(
                        varName,
                        inferredType,
                        operator);
            }

            case cmmParser.MulExpContext e -> {
                TypedExp typedExpLhs = inferExp(env, e.exp(0));
                TypedExp typedExpRhs = inferExp(env, e.exp(1));

                if (typedExpLhs.type() == CType.BOOL ||
                        typedExpRhs.type() == CType.BOOL) {

                    throw new TypeException("Multiplication/division for bool "
                            + "not allowed.");

                } else if (typedExpLhs.type() == CType.VOID ||
                        typedExpRhs.type() == CType.VOID) {

                    throw new TypeException("Multiplication/division for void "
                            + "not allowed.");
                }

                CType domCType = domCType(typedExpLhs.type(), typedExpRhs.type());

                // get operator type
                Operator operator = switch (e.mulOp()) {
                    case cmmParser.MulContext _ -> {
                        yield Operator.MUL;
                    }

                    case cmmParser.DivContext _ -> {
                        yield Operator.DIV;
                    }

                    default -> {
                        throw new TypeException("Operator " + e.mulOp()
                                + "not yet implemented for multiplication-like "
                                + "operations");
                    }
                };

                yield new TypedExp.TypedEMul(
                        promoteExp(typedExpLhs, domCType),
                        promoteExp(typedExpRhs, domCType),
                        domCType,
                        operator);
            }

            case cmmParser.AddExpContext e -> {
                TypedExp typedExpLhs = inferExp(env, e.exp(0));
                TypedExp typedExpRhs = inferExp(env, e.exp(1));
                if (typedExpLhs.type() == CType.BOOL ||
                        typedExpRhs.type() == CType.BOOL) {

                    throw new TypeException("Addition and subtraction for bool "
                            + "not allowed.");

                } else if (typedExpLhs.type() == CType.VOID ||
                        typedExpRhs.type() == CType.VOID) {

                    throw new TypeException("Addition and subtraction for void "
                            + "not allowed.");
                }

                CType domCType = domCType(typedExpLhs.type(), typedExpRhs.type());

                // get operator type
                Operator operator = switch (e.addOp()) {
                    case cmmParser.AddContext _ -> {
                        yield Operator.ADD;
                    }

                    case cmmParser.SubContext _ -> {
                        yield Operator.DIV;
                    }

                    default -> {
                        throw new TypeException("Operator " + e.addOp()
                                + "not yet implemented for addition/subtraction"
                                + "operations");
                    }
                };

                yield new TypedExp.TypedEAdd(
                        promoteExp(typedExpLhs, domCType),
                        promoteExp(typedExpRhs, domCType),
                        domCType,
                        operator);
            }

            case cmmParser.CmpExpContext e -> {
                TypedExp typedExpLhs = inferExp(env, e.exp(0));
                TypedExp typedExpRhs = inferExp(env, e.exp(1));

                if (typedExpLhs.type() == CType.VOID ||
                        typedExpRhs.type() == CType.VOID) {
                    throw new TypeException("Comparison with void type not "
                            + "allowed.");
                }

                // get operator type
                Operator operator = switch (e.cmpOp()) {
                    case cmmParser.LThContext _ -> {
                        yield Operator.LTH;
                    }
                    case cmmParser.GThContext _ -> {
                        yield Operator.GTH;
                    }
                    case cmmParser.LTEContext _ -> {
                        yield Operator.LTE;
                    }
                    case cmmParser.GTEContext _ -> {
                        yield Operator.GTE;
                    }
                    case cmmParser.EquContext _ -> {
                        if ((typedExpLhs.type() == CType.BOOL) ^
                                (typedExpRhs.type() == CType.BOOL)) {
                            throw new TypeException("Equality comparison "
                                    + "between bool and non-bool types is not "
                                    + "allowed.");
                        }
                        yield Operator.EQU;
                    }
                    case cmmParser.NEqContext _ -> {
                        if ((typedExpLhs.type() == CType.BOOL) ^
                                (typedExpRhs.type() == CType.BOOL)) {
                            throw new TypeException("Inequality comparison "
                                    + "between bool and "
                                    + "non-bool types is not allowed.");
                        }
                        yield Operator.NEQ;
                    }
                    default -> {
                        throw new TypeException("Operator " + e.cmpOp()
                                + "not yet implemented for comparison "
                                + "operations");
                    }
                };

                CType domCType = domCType(
                        typedExpLhs.type(),
                        typedExpRhs.type());

                yield new TypedExp.TypedECmp(
                        promoteExp(typedExpLhs, domCType),
                        promoteExp(typedExpRhs, domCType),
                        operator);
            }

            case cmmParser.AndExpContext e -> {
                TypedExp typedExpLhs = inferExp(env, e.exp(0));
                TypedExp typedExpRhs = inferExp(env, e.exp(1));
                if (!(typedExpLhs.type() == CType.BOOL &&
                        typedExpRhs.type() == CType.BOOL)) {

                    throw new TypeException("AND (&&) operation can only occur "
                            + "between booleans");
                }
                yield new TypedExp.TypedEAnd(typedExpLhs, typedExpRhs);
            }

            case cmmParser.OrExpContext e -> {
                TypedExp typedExp1 = inferExp(env, e.exp(0));
                TypedExp typedExp2 = inferExp(env, e.exp(1));
                if (!(typedExp1.type() == CType.BOOL &&
                        typedExp2.type() == CType.BOOL)) {
                    throw new TypeException("|| operation can only occur "
                            + "between booleans");
                }
                yield new TypedExp.TypedEOr(typedExp1, typedExp2);
            }

            case cmmParser.AssExpContext e -> {
                String varName = e.Ident().getText();
                CType variableType = env.lookupVar(varName);
                TypedExp typedExp = inferExp(env, e.exp());
                if (!isConvertible(variableType, typedExp.type())) {
                    throw new TypeException("Illegal implicit conversion in "
                            + "assignment. Expected " + variableType
                            + ", but got " + typedExp.type() + " instead.");
                }

                yield new TypedExp.TypedEAss(
                        varName,
                        promoteExp(typedExp, variableType),
                        variableType);
            }

            default -> throw new IllegalStateException("Case for " + exp
                    + " is not implemented.");
        };
    }

    public CType toCType(cmmParser.TypeContext fromType) {
        return switch (fromType.getChild(0)) {
            case cmmParser.IntTypeContext _ -> CType.INT;
            case cmmParser.DoubleTypeContext _ -> CType.DOUBLE;
            case cmmParser.BoolTypeContext _ -> CType.BOOL;
            case cmmParser.VoidTypeContext _ -> CType.VOID;
            default -> throw new IllegalStateException("Type " + fromType
                    + " is not yet implemented.");
        };
    }

    public boolean isConvertible(CType expected, CType actual) {
        // i learned about this way of returning bools in if else statements so
        // i wanted to try it out :)
        return (expected == CType.DOUBLE &&
                (actual == CType.INT || actual == CType.DOUBLE)) ||
                (expected == CType.INT && actual == CType.INT) ||
                (expected == CType.BOOL && actual == CType.BOOL) ||
                (expected == CType.VOID && actual == CType.VOID);
    }

    public CType domCType(CType cType1, CType cType2) {
        if ((cType1 == CType.INT && cType2 == CType.DOUBLE) ||
                (cType1 == CType.DOUBLE && cType2 == CType.INT)) {
            return CType.DOUBLE;
        } else if ((cType1 == CType.DOUBLE && cType2 == CType.DOUBLE)) {
            return CType.DOUBLE;
        } else if ((cType1 == CType.INT && cType2 == CType.INT)) {
            return CType.INT;
        } else if ((cType1 == CType.VOID && cType2 == CType.VOID)) {
            return CType.VOID;
        } else if ((cType1 == CType.BOOL && cType2 == CType.BOOL)) {
            return CType.BOOL;
        } else {
            throw new TypeException("Illegal implicit conversion.");
        }
    }

    public TypedExp promoteExp(TypedExp exp, CType dominatingType) {
        if (exp.type() == CType.INT && dominatingType == CType.DOUBLE) {
            return new TypedExp.TypedEI2D(exp);
        } else {
            return exp;
        }
    }

    private void functionPass(
            TypeCheckerEnvironment env,
            List<cmmParser.DefContext> funcDefs) {

        for (cmmParser.DefContext func : funcDefs) {

            String name = func.Ident().getText();
            CType type = toCType(func.type());
            LinkedHashMap<String, CType> parameters = new LinkedHashMap<>();

            for (var param : func.arg()) {
                String paramName = param.Ident().getText();
                CType paramType = toCType(param.type());

                if (parameters.containsKey(paramName)) {
                    throw new TypeException(
                            "Duplicate function parameter names not allowed");
                }
                if (paramType == CType.VOID) {
                    throw new TypeException(
                            "Variable can not be of type void.");
                }
                parameters.put(paramName, paramType);
            }

            TypeCheckerEnvironment.Signature signature = new TypeCheckerEnvironment.Signature(type, parameters);
            env.extendFunc(name, signature);
        }
    }

    private TypedProgram stmPass(TypeCheckerEnvironment env,
            List<cmmParser.DefContext> funcDefs) {

        LinkedList<TypedDef> typedDefList = new LinkedList<>();

        for (cmmParser.DefContext func : funcDefs) {

            String funcName = func.Ident().getText();
            CType type = toCType(func.type());
            LinkedHashMap<String, CType> params = new LinkedHashMap<>();

            // set the current function being processed
            env.setCurrentFunc(funcName);
            // create a new context for that function
            env.newContextCurrentFunc();

            for (var param : func.arg()) {
                params.put(param.Ident().getText(), toCType(param.type()));
            }
            TypeCheckerEnvironment.Signature sign = new TypeCheckerEnvironment.Signature(type, params);

            // loop over all statements in current function
            LinkedList<TypedStm> typedStmList = new LinkedList<>();
            for (var stm : func.stm()) {
                typedStmList.add(checkStm(env, stm));
            }

            // construct argument list
            LinkedList<TypedArg> typedArgList = new LinkedList<>();
            for (String key : sign.parameters().keySet()) {
                typedArgList.add(new TypedArg.TypedADecl(params.get(key), key));
            }

            typedDefList.add(new TypedDef.TypedDFun(sign.returns(),
                    typedArgList, typedStmList, funcName));

            env.clearContexts();
        }
        return new TypedProgram(typedDefList);
    }

    public TypedProgram typecheck(cmmParser.ProgramContext program) {

        TypeCheckerEnvironment environment = new TypeCheckerEnvironment();
        environment.newContext();

        environment.addSimpleFunction("printInt", CType.VOID, CType.INT);
        environment.addSimpleFunction("printDouble", CType.VOID, CType.DOUBLE);
        environment.addSimpleFunction("readInt", CType.INT, null);
        environment.addSimpleFunction("readDouble", CType.DOUBLE, null);

        List<cmmParser.DefContext> functionDefinitions = program.def();

        if (functionDefinitions.stream().noneMatch(function -> function
                        .Ident()
                        .getText()
                        .equals("main"))) {

            throw new TypeException("Program has no entrypoint 'main'.");
        }

        CType mainFuncType = toCType(functionDefinitions
                .stream()
                .filter(function -> function.Ident().getText().equals("main"))
                .findFirst()
                .get().type());

        if (mainFuncType != CType.INT) {
            throw new TypeException("'main' function is not of type 'int'.");
        }

        if (!functionDefinitions.stream()
                .filter(function -> function.Ident().getText().equals("main"))
                .findFirst()
                .get()
                .arg()
                .isEmpty()) {
            throw new TypeException("'main' function can not have parameters.");
        }

        functionPass(environment, functionDefinitions);

        return stmPass(environment, functionDefinitions);
    }
}
