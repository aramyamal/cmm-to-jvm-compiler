package typed_tree;

import java.util.LinkedList;

public interface TypedStm {
    record TypedSExp(CType type, TypedExp exp) implements TypedStm {
    }

    record TypedSDecls(CType type, LinkedList<String> names) implements TypedStm {
    }

    record TypedSInit(CType type, String name, TypedExp exp) implements TypedStm {
    }

    record TypedSReturn(TypedExp exp) implements TypedStm {
    }

    record TypedSWhile(TypedExp exp, TypedStm stm) implements TypedStm {
    }

    record TypedSBlock(LinkedList<TypedStm> stms) implements TypedStm {
    }

    record TypedSIfElse(TypedExp exp, TypedStm stm1, TypedStm stm2) implements TypedStm {
    }
}
