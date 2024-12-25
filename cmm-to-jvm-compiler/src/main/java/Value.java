public interface Value {
    Object value();

    Value.Dubbel toDubbel();

    Value.Int toInt();

    Value.Bool toBool();

    Value increment();

    Value decrement();

    Value multiply(Value other);

    Value divide(Value other);

    Value add(Value other);

    Value subtract(Value other);

    public record Int(Integer value) implements Value {

        @Override
        public Value.Dubbel toDubbel() {
            return new Value.Dubbel(this.value.doubleValue());
        }

        @Override
        public Value.Int toInt() {
            return this;
        }

        @Override
        public Value.Bool toBool() {
            return new Value.Bool(this.value != 0);
        }

        @Override
        public Value increment() {
            return new Value.Int(this.value + 1);
        }

        @Override
        public Value decrement() {
            return new Value.Int(this.value - 1);
        }

        @Override
        public Value multiply(Value other) {
            return new Value.Int(this.value * other.toInt().value());
        }

        @Override
        public Value divide(Value other) {
            if (other instanceof Value.Int intValue) {
                return new Value.Int(this.value / intValue.value());
            } else if (other instanceof Value.Dubbel dubbelValue) {
                return new Value.Dubbel(this.value / dubbelValue.value());
            } else {
                throw new UnsupportedOperationException("Division not supported with this type.");
            }
        }

        @Override
        public Value add(Value other) {
            if (other instanceof Value.Int intValue) {
                return new Value.Int(this.value + intValue.value());
            } else if (other instanceof Value.Dubbel dubbelValue) {
                return new Value.Dubbel(this.value + dubbelValue.value());
            }
            throw new UnsupportedOperationException("Addition not supported for this type.");
        }

        @Override
        public Value subtract(Value other) {
            if (other instanceof Value.Int intValue) {
                return new Value.Int(this.value - intValue.value());
            } else if (other instanceof Value.Dubbel dubbelValue) {
                return new Value.Dubbel(this.value - dubbelValue.value());
            }
            throw new UnsupportedOperationException("Subtraction not supported for this type.");
        }
    }

    public record Dubbel(Double value) implements Value {

        @Override
        public Value.Dubbel toDubbel() {
            return this;
        }

        @Override
        public Value.Int toInt() {
            return new Value.Int(this.value.intValue());
        }

        @Override
        public Value.Bool toBool() {
            return new Value.Bool(this.value != 0.0);
        }

        @Override
        public Value increment() {
            return new Value.Dubbel(this.value + 1);
        }

        @Override
        public Value decrement() {
            return new Value.Dubbel(this.value - 1);
        }

        @Override
        public Value multiply(Value other) {
            return new Value.Dubbel(this.value * other.toDubbel().value());
        }

        @Override
        public Value divide(Value other) {
            if (other instanceof Value.Int intValue) {
                return new Value.Dubbel(this.value / intValue.value().doubleValue());
            } else if (other instanceof Value.Dubbel dubbelValue) {
                return new Value.Dubbel(this.value / dubbelValue.value());
            } else {
                throw new UnsupportedOperationException("Division not supported with this type.");
            }
        }

        @Override
        public Value add(Value other) {
            if (other instanceof Value.Int intValue) {
                return new Value.Dubbel(this.value + intValue.value());
            } else if (other instanceof Value.Dubbel dubbelValue) {
                return new Value.Dubbel(this.value + dubbelValue.value());
            }
            throw new UnsupportedOperationException("Addition not supported for this type.");
        }

        @Override
        public Value subtract(Value other) {
            if (other instanceof Value.Int intValue) {
                return new Value.Dubbel(this.value - intValue.value());
            } else if (other instanceof Value.Dubbel dubbelValue) {
                return new Value.Dubbel(this.value - dubbelValue.value());
            }
            throw new UnsupportedOperationException("Subtraction not supported for this type.");
        }
    }

    public record Bool(Boolean value) implements Value {

        @Override
        public Value.Dubbel toDubbel() {
            return new Value.Dubbel(this.value ? 1.0 : 0.0);
        }

        @Override
        public Value.Int toInt() {
            return new Value.Int(this.value ? 1 : 0);
        }

        @Override
        public Value.Bool toBool() {
            return this;
        }

        @Override
        public Value increment() {
            throw new UnsupportedOperationException("Increment operation not supported for Bool.");
        }

        @Override
        public Value decrement() {
            throw new UnsupportedOperationException("Decrement operation not supported for Bool.");
        }

        @Override
        public Value multiply(Value other) {
            throw new UnsupportedOperationException("Multiplication not supported for Bool.");
        }

        @Override
        public Value divide(Value other) {
            throw new UnsupportedOperationException("Division not supported for Bool.");
        }

        @Override
        public Value add(Value other) {
            throw new UnsupportedOperationException("Addition not supported for Bool.");
        }

        @Override
        public Value subtract(Value other) {
            throw new UnsupportedOperationException("Subtraction not supported for Bool.");
        }
    }

    public record Void() implements Value {
        @Override
        public Object value() {
            return null;
        }

        @Override
        public Value.Dubbel toDubbel() {
            throw new UnsupportedOperationException("Void has no numerical representation.");
        }

        @Override
        public Value.Int toInt() {
            throw new UnsupportedOperationException("Void has no numerical representation.");
        }

        @Override
        public Value.Bool toBool() {
            throw new UnsupportedOperationException("Void has no boolean representation.");
        }

        @Override
        public Value increment() {
            throw new UnsupportedOperationException("Increment operation not supported for Void.");
        }

        @Override
        public Value decrement() {
            throw new UnsupportedOperationException("Decrement operation not supported for Void.");
        }

        @Override
        public Value multiply(Value other) {
            throw new UnsupportedOperationException("Multiplication not supported for Void.");
        }

        @Override
        public Value divide(Value other) {
            throw new UnsupportedOperationException("Division not supported for Void.");
        }

        @Override
        public Value add(Value other) {
            throw new UnsupportedOperationException("Addition not supported for Void.");
        }

        @Override
        public Value subtract(Value other) {
            throw new UnsupportedOperationException("Subtraction not supported for Void.");
        }
    }
}
