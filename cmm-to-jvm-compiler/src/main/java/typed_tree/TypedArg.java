package typed_tree;

public interface TypedArg {

    record TypedADecl(CType type, String id) implements TypedArg {
    }
}

