package typed_tree;

import java.util.LinkedList;

public interface TypedDef {

    record TypedDFun(CType returns, LinkedList<TypedArg> args,
            LinkedList<TypedStm> stms, String funcName) implements TypedDef {
    }
};
