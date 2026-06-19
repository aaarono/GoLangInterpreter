package interpreter;

import java.util.List;


public class Value {
        enum Type { INT, FLOAT, BOOL, STRING, FUNCTION, BUILTIN_FUNCTION, ARRAY, MAP, NULL }

        Type type;
        Object value;

        Value(Type type, Object value) {
            this.type = type;
            this.value = value;
        }

        static Value nullValue() {
            return new Value(Type.NULL, null);
        }

        static Value defaultValueForType() {
            // Для упрощения всегда int=0
            return new Value(Type.INT, 0);
        }

        boolean toBoolean() {
            switch (type) {
                case BOOL: return (Boolean)value;
                case INT: return ((Integer)value) != 0;
                case FLOAT: return ((Double)value) != 0.0;
                case STRING: return !((String)value).isEmpty();
                case NULL: return false;
                default: return true;
            }
        }

        boolean isNumeric() {
            return type == Type.INT || type == Type.FLOAT;
        }

        double toDouble() {
            if (type == Type.INT) return (Integer)value;
            if (type == Type.FLOAT) return (Double)value;
            throw new RuntimeException("Value is not numeric");
        }

        boolean equalsValue(Value other) {
            if (this.type != other.type) return false;
            if (this.value == null) return other.value == null;
            return this.value.equals(other.value);
        }

        static Value fromBoolean(boolean b) {
            return new Value(Type.BOOL, b);
        }

        static Value fromArray(List<Value> arr) {
            return new Value(Type.ARRAY, arr);
        }

        String toStringValue() {
            if (value == null) return "null";
            return value.toString();
        }
}
