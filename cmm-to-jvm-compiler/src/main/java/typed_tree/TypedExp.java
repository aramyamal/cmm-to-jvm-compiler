package typed_tree;

import java.util.LinkedList;

public interface TypedExp {
    CType type();

    // ## Atomic expressions
    // ### Literals
    record TypedEBool(Boolean value) implements TypedExp {
        public CType type() {
            return CType.BOOL;
        }
    }

    record TypedEInt(Integer value) implements TypedExp {
        public CType type() {
            return CType.INT;
        }
    }

    record TypedEDouble(Double value) implements TypedExp {
        public CType type() {
            return CType.DOUBLE;
        }
    }

    // ### Identifiers and function calls
    record TypedEId(String id, CType type) implements TypedExp {
    }

    record TypedEApp(String id, LinkedList<TypedExp> exps, CType type)
            implements TypedExp {
    }

    // ### Increment and decrement
    record TypedEPost(String id, CType type, Operator operator)
            implements TypedExp {
    }

    record TypedEPre(String id, CType type, Operator operator)
            implements TypedExp {
    }

    // ## Compund expressions
    record TypedEMul(TypedExp exp1,
            TypedExp exp2,
            CType type,
            Operator operator)
            implements TypedExp {
    }

    record TypedEAdd(TypedExp exp1, 
            TypedExp exp2, 
            CType type, 
            Operator operator)
            implements TypedExp {
    }

    record TypedECmp(TypedExp exp1, TypedExp exp2, Operator operator)
            implements TypedExp {

        public CType type() {
            return CType.BOOL;
        }
    }

    record TypedEAnd(TypedExp exp1, TypedExp exp2) implements TypedExp {
        public CType type() {
            return CType.BOOL;
        }
    }

    record TypedEOr(TypedExp exp1, TypedExp exp2) implements TypedExp {
        public CType type() {
            return CType.BOOL;
        }
    }

    record TypedEAss(String id, TypedExp exp, CType type) implements TypedExp {
    }

    record TypedEI2D(TypedExp exp) implements TypedExp {
        public CType type() {
            return CType.DOUBLE;
        }
    }

}
