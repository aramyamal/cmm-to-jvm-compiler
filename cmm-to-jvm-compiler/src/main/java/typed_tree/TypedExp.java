package typed_tree;

import java.util.LinkedList;

public interface TypedExp {
    CType type();

    record Paren(TypedExp exp, CType type) implements TypedExp {
    }

    // ## Atomic expressions
    // ### Literals
    record BoolLit(Boolean value) implements TypedExp {
        public CType type() {
            return CType.BOOL;
        }
    }

    record IntLit(Integer value) implements TypedExp {
        public CType type() {
            return CType.INT;
        }
    }

    record DoubleLit(Double value) implements TypedExp {
        public CType type() {
            return CType.DOUBLE;
        }
    }

    // ### Identifiers and function calls
    record Ident(String id, CType type) implements TypedExp {
    }

    record Func(String id, LinkedList<TypedExp> exps, CType type)
            implements TypedExp {
    }

    // ### Increment and decrement
    record Post(String id, CType type, Operator operator)
            implements TypedExp {
    }

    record Pre(String id, CType type, Operator operator)
            implements TypedExp {
    }

    // ## Compund expressions
    record Mul(TypedExp exp1,
            TypedExp exp2,
            CType type,
            Operator operator)
            implements TypedExp {
    }

    record Add(TypedExp exp1,
            TypedExp exp2,
            CType type,
            Operator operator)
            implements TypedExp {
    }

    record Cmp(TypedExp exp1, TypedExp exp2, Operator operator)
            implements TypedExp {

        public CType type() {
            return CType.BOOL;
        }
    }

    record And(TypedExp exp1, TypedExp exp2) implements TypedExp {
        public CType type() {
            return CType.BOOL;
        }
    }

    record Or(TypedExp exp1, TypedExp exp2) implements TypedExp {
        public CType type() {
            return CType.BOOL;
        }
    }

    record Assign(String id, TypedExp exp, CType type) implements TypedExp {
    }

    record Int2Double(TypedExp exp) implements TypedExp {
        public CType type() {
            return CType.DOUBLE;
        }
    }

}
