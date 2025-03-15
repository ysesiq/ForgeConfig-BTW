package net.minecraftforge.common;

import java.util.ArrayList;

public class Property {
    private String name;
    private String value;
    public String comment;
    private String[] values;
    private final boolean wasRead;
    private final boolean isList;
    private final Type type;
    private boolean changed;

    public enum Type {
        STRING,
        INTEGER,
        BOOLEAN,
        DOUBLE;

        private static Type[] values = {STRING, INTEGER, BOOLEAN, DOUBLE};

        public static Type tryParse(char id) {
            for (int x = 0; x < values.length; x++) {
                if (values[x].getID() == id) {
                    return values[x];
                }
            }
            return STRING;
        }

        public char getID() {
            return name().charAt(0);
        }
    }

    public Property() {
        this.changed = false;
        this.wasRead = false;
        this.type = null;
        this.isList = false;
    }

    public Property(String name, String value, Type type) {
        this(name, value, type, false);
    }

    Property(String name, String value, Type type, boolean read) {
        this.changed = false;
        setName(name);
        this.value = value;
        this.type = type;
        this.wasRead = read;
        this.isList = false;
    }

    public Property(String name, String[] values, Type type) {
        this(name, values, type, false);
    }

    Property(String name, String[] values, Type type, boolean read) {
        this.changed = false;
        setName(name);
        this.type = type;
        this.values = values;
        this.wasRead = read;
        this.isList = true;
    }

    public String getString() {
        return this.value;
    }

    public int getInt() {
        return getInt(-1);
    }

    public int getInt(int _default) {
        try {
            return Integer.parseInt(this.value);
        } catch (NumberFormatException e) {
            return _default;
        }
    }

    public boolean isIntValue() {
        try {
            Integer.parseInt(this.value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean getBoolean(boolean _default) {
        if (isBooleanValue()) {
            return Boolean.parseBoolean(this.value);
        }
        return _default;
    }

    public boolean isBooleanValue() {
        return "true".equals(this.value.toLowerCase()) || "false".equals(this.value.toLowerCase());
    }

    public boolean isDoubleValue() {
        try {
            Double.parseDouble(this.value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public double getDouble(double _default) {
        try {
            return Double.parseDouble(this.value);
        } catch (NumberFormatException e) {
            return _default;
        }
    }

    public String[] getStringList() {
        return this.values;
    }

    public int[] getIntList() {
        ArrayList<Integer> nums = new ArrayList<>();
        for (String value : this.values) {
            try {
                nums.add(Integer.valueOf(Integer.parseInt(value)));
            } catch (NumberFormatException e) {
            }
        }
        int[] primitives = new int[nums.size()];
        for (int i = 0; i < nums.size(); i++) {
            primitives[i] = nums.get(i).intValue();
        }
        return primitives;
    }

    public boolean isIntList() {
        for (String value : this.values) {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public boolean[] getBooleanList() {
        ArrayList<Boolean> tmp = new ArrayList<>();
        for (String value : this.values) {
            try {
                tmp.add(Boolean.valueOf(Boolean.parseBoolean(value)));
            } catch (NumberFormatException e) {
            }
        }
        boolean[] primitives = new boolean[tmp.size()];
        for (int i = 0; i < tmp.size(); i++) {
            primitives[i] = tmp.get(i).booleanValue();
        }
        return primitives;
    }

    public boolean isBooleanList() {
        for (String value : this.values) {
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                return false;
            }
        }
        return true;
    }

    public double[] getDoubleList() {
        ArrayList<Double> tmp = new ArrayList<>();
        for (String value : this.values) {
            try {
                tmp.add(Double.valueOf(Double.parseDouble(value)));
            } catch (NumberFormatException e) {
            }
        }
        double[] primitives = new double[tmp.size()];
        for (int i = 0; i < tmp.size(); i++) {
            primitives[i] = tmp.get(i).doubleValue();
        }
        return primitives;
    }

    public boolean isDoubleList() {
        for (String value : this.values) {
            try {
                Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean wasRead() {
        return this.wasRead;
    }

    public Type getType() {
        return this.type;
    }

    public boolean isList() {
        return this.isList;
    }

    public boolean hasChanged() {
        return this.changed;
    }

    void resetChangedState() {
        this.changed = false;
    }

    public void set(String value) {
        this.value = value;
        this.changed = true;
    }

    public void set(String[] values) {
        this.values = values;
        this.changed = true;
    }

    public void set(int value) {
        set(Integer.toString(value));
    }

    public void set(boolean value) {
        set(Boolean.toString(value));
    }

    public void set(double value) {
        set(Double.toString(value));
    }
}
