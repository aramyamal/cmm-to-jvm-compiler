package typed_tree;

import java.util.LinkedList;

public interface TypedStm {
    record Exp(CType type, TypedExp exp) implements TypedStm {
    }

    record Decls(CType type, LinkedList<String> names) implements TypedStm {
    }

    record Init(CType type, String name, TypedExp exp) implements TypedStm {
    }

    record Return(TypedExp exp) implements TypedStm {
    }

    record While(TypedExp exp, TypedStm stm) implements TypedStm {
    }

    record Block(LinkedList<TypedStm> stms) implements TypedStm {
    }

    record IfElse(TypedExp exp, TypedStm stm1, TypedStm stm2) implements TypedStm {
    }
}
