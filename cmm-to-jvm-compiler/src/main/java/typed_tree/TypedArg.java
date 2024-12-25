package typed_tree;

public interface TypedArg {

    record Decl(CType type, String id) implements TypedArg {
    }
}

