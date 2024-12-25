package typed_tree;

public interface TypedArg {
    CType type();
    String id();

    record Decl(CType type, String id) implements TypedArg {
    }
}

