import java.util.LinkedList;
import typed_tree.*;

public interface JVMInstr {
    String println();

    CType type();

    public static String getJVMType(CType type) {
        return switch (type) {
            case CType.INT -> "I";
            case CType.DOUBLE -> "D";
            case CType.BOOL -> "Z";
            case CType.VOID -> "V";
            default -> throw new IllegalArgumentException("Unsupported CType: "
                    + type);
        };
    }

    // method definition
    record Dotmethod(
            String name,
            LinkedList<CType> varTypes,
            CType type,
            Integer limitlocals,
            Integer limitstack)
            implements JVMInstr {

        @Override
        public String println() {
            StringBuilder inputs = new StringBuilder();
            for (CType type : varTypes) {
                inputs.append(getJVMType(type));
            }
            String output = getJVMType(type);
            return ".method public static "
                    + name
                    + "(" + inputs.toString() + ")"
                    + output + "\n"
                    + "\t.limit locals " + this.limitlocals + "\n"
                    + "\t.limit stack  " + this.limitstack + "\n";
        }
    }

    // method definition for main function
    record DotmethodMain(
            Integer limitlocals,
            Integer limitstack)
            implements JVMInstr {

        @Override
        public CType type() {
            return CType.VOID;
        }

        @Override
        public String println() {
            return ".method public static "
                    + "main([Ljava/lang/String;)V" + "\n"
                    + "\t.limit locals " + this.limitlocals + "\n"
                    + "\t.limit stack  " + this.limitstack + "\n";
        }
    }

    // end method instruction
    record DotEndMethod() implements JVMInstr {
        @Override
        public String println() {
            return ".end method\n\n";
        }

        @Override
        public CType type() {
            return CType.VOID;
        }
    }

    record Return(CType type) implements JVMInstr {

        @Override
        public String println() {

            return switch (type) {

                case CType.INT, CType.BOOL -> {
                    yield "\tireturn\n";
                }

                case CType.DOUBLE -> {
                    yield "\tdreturn\n";
                }

                case CType.VOID -> {
                    yield "\treturn\n";
                }

                default -> throw new IllegalArgumentException(
                        "Unsupported return type: " + type);
            };
        }
    }

    // function calls
    record Invokestatic(
            String name,
            String className,
            LinkedList<CType> varTypes,
            CType type)
            implements JVMInstr {

        @Override
        public String println() {
            StringBuilder inputs = new StringBuilder();
            for (CType type : varTypes) {
                inputs.append(getJVMType(type));
            }
            String output = getJVMType(type);
            return "\tinvokestatic " + className + "/" + name
                    + "(" + inputs.toString() + ")" + output + "\n";
        }
    }

    // jump labels
    record Label(Integer label) implements JVMInstr {
        public String println() {
            return "LABEL" + label + ":\n";
        }

        @Override
        public CType type() {
            return CType.VOID;
        }
    }

    record Goto(Integer label) implements JVMInstr {
        public String println() {
            return "\tgoto " + "LABEL" + label + "\n";
        }

        @Override
        public CType type() {
            return CType.VOID;
        }
    }

    // store variables
    record Store(CType type, Integer address) implements JVMInstr {

        public String println() {

            return switch (type) {

                case CType.INT, CType.BOOL -> {
                    if (address >= 0 && address <= 3) {
                        yield "\tistore_" + address + "\n";
                    }
                    yield "\tistore " + address + "\n";
                }

                case CType.DOUBLE -> {
                    if (address >= 0 && address <= 3) {
                        yield "\tdstore_" + address + "\n";
                    }
                    yield "\tdstore " + address + "\n";
                }

                case CType.VOID -> throw new IllegalArgumentException(
                        "Cannot store void type");

                default -> throw new IllegalArgumentException(
                        "Unsupported store type: " + type);
            };
        }
    }

    // load variables
    record Load(CType type, Integer address) implements JVMInstr {

        public String println() {
            return switch (type) {

                case CType.INT, CType.BOOL -> {
                    if (address >= 0 && address <= 3) {
                        yield "\tiload_" + address + "\n";
                    }
                    yield "\tiload " + address + "\n";
                }

                case CType.DOUBLE -> {
                    if (address >= 0 && address <= 3) {
                        yield "\tdload_" + address + "\n";
                    }
                    yield "\tdload " + address + "\n";
                }

                case CType.VOID -> throw new IllegalArgumentException(
                        "Cannot load void type");

                default -> throw new IllegalArgumentException(
                        "Unsupported load type: " + type);
            };
        }
    }

    // push constant value to stack
    record Push(CType type, Object value) implements JVMInstr {

        public String println() {

            return switch (type) {

                case CType.INT -> {
                    Integer intValue = (Integer) value;
                    if (intValue >= -1 && intValue <= 5) {
                        yield "\ticonst_" +
                                (intValue == -1 ? "m1" : intValue)
                                + "\n";
                    } else if (intValue >= -128 && intValue <= 127) {
                        yield "\tbipush " + intValue + "\n";
                    } else {
                        yield "\tldc " + intValue + "\n";
                    }
                }

                case CType.BOOL -> {
                    Boolean boolValue = (Boolean) value;
                    yield "\ticonst_" + (boolValue ? "1" : "0") + "\n";
                }

                case CType.DOUBLE -> {
                    Double doubleValue;
                    if (value instanceof Integer integerValue) {
                        doubleValue = integerValue.doubleValue();
                    } else {
                        doubleValue = (Double) value;
                    }
                    if (doubleValue == 0.0 || doubleValue == 1.0) {
                        yield "\tdconst_" + doubleValue.intValue() + "\n";
                    }
                    yield "\tldc2_w " + doubleValue + "\n";
                }

                default -> throw new IllegalArgumentException(
                        "Unsupported constant type: " + type);
            };
        }
    }

    // compare two newest stack elements and jump accordingly
    record Cmp(CType cmpType, Operator operator, Integer label)
            implements JVMInstr {

        @Override
        public CType type() {
            return CType.BOOL;
        }

        public String println() {
            return switch (cmpType) {
                case CType.INT -> switch (operator) {
                    case Operator.LTH -> "\tif_icmplt " + "LABEL" + label + "\n";
                    case Operator.GTH -> "\tif_icmpgt " + "LABEL" + label + "\n";
                    case Operator.LTE -> "\tif_icmple " + "LABEL" + label + "\n";
                    case Operator.GTE -> "\tif_icmpge " + "LABEL" + label + "\n";
                    case Operator.EQU -> "\tif_icmpeq " + "LABEL" + label + "\n";
                    case Operator.NEQ -> "\tif_icmpne " + "LABEL" + label + "\n";
                    default -> throw new IllegalArgumentException(
                            "Unsupported int operator" + operator.getClass());
                };
                case CType.DOUBLE -> switch (operator) {
                    case Operator.LTH -> "\tdcmpg\n\tiflt " + "LABEL" + label + "\n";
                    case Operator.GTH -> "\tdcmpg\n\tifgt " + "LABEL" + label + "\n";
                    case Operator.LTE -> "\tdcmpg\n\tifle " + "LABEL" + label + "\n";
                    case Operator.GTE -> "\tdcmpg\n\tifge " + "LABEL" + label + "\n";
                    case Operator.EQU -> "\tdcmpg\n\tifeq " + "LABEL" + label + "\n";
                    case Operator.NEQ -> "\tdcmpg\n\tifne " + "LABEL" + label + "\n";
                    default -> throw new IllegalArgumentException(
                            "Unsupported double operator"
                                    + operator.getClass());
                };

                case CType.BOOL -> switch (operator) {
                    case Operator.EQU -> "\tif_icmpeq " + "LABEL" + label + "\n";
                    case Operator.NEQ -> "\tif_icmpne " + "LABEL" + label + "\n";
                    default -> throw new IllegalArgumentException(
                            "Unsupported bool operator" + operator.getClass());
                };

                default -> throw new IllegalArgumentException(
                        "Unsupported type for comparison "
                                + cmpType.getClass());
            };
        }
    }

    // compare latest stack element to 0
    record Ifeq(Integer label) implements JVMInstr {

        public String println() {
            return "\tifeq " + "LABEL" + label + "\n";
        }

        @Override
        public CType type() {
            return CType.BOOL;
        }
    }

    // compare if latest stack element greater than zero
    record Ifne(Integer label) implements JVMInstr {

        public String println() {
            return "\tifne " + "LABEL" + label + "\n";
        }

        @Override
        public CType type() {
            return CType.BOOL;
        }
    }

    // addition
    record Add(CType type) implements JVMInstr {

        public String println() {

            return switch (type) {

                case CType.INT -> {
                    yield "\tiadd\n";
                }

                case CType.DOUBLE -> {
                    yield "\tdadd\n";
                }

                default -> throw new IllegalArgumentException(
                        "Unsupported addition type: " + type);
            };
        }
    }

    // subtraction
    record Sub(CType type) implements JVMInstr {

        public String println() {

            return switch (type) {

                case CType.INT -> {
                    yield "\tisub\n";
                }

                case CType.DOUBLE -> {
                    yield "\tdsub\n";
                }

                default -> throw new IllegalArgumentException(
                        "Unsupported addition type: " + type);
            };
        }
    }

    // multiplication
    record Mul(CType type) implements JVMInstr {

        public String println() {

            return switch (type) {

                case CType.INT -> {
                    yield "\timul\n";
                }

                case CType.DOUBLE -> {
                    yield "\tdmul\n";
                }

                default -> throw new IllegalArgumentException(
                        "Unsupported multiplication type: " + type);
            };
        }
    }

    // division
    record Div(CType type) implements JVMInstr {

        public String println() {

            return switch (type) {

                case CType.INT -> {
                    yield "\tidiv\n";
                }

                case CType.DOUBLE -> {
                    yield "\tddiv\n";
                }

                default -> throw new IllegalArgumentException(
                        "Unsupported division type: " + type);
            };
        }
    }

    // duplicate newest stack element
    record Dup(CType type) implements JVMInstr {

        public String println() {
            if (type == CType.DOUBLE) {
                return "\tdup2\n";
            }
            return "\tdup\n";
        }
    }

    // pop stack
    record Pop(CType type) implements JVMInstr {

        public String println() {
            if (type == CType.DOUBLE) {
                return "\tpop2\n";
            }
            return "\tpop\n";
        }
    }

    // integer to double
    record I2d() implements JVMInstr {

        public String println() {
            return "\ti2d\n";
        }

        @Override
        public CType type() {
            return CType.DOUBLE;
        }
    }
}
