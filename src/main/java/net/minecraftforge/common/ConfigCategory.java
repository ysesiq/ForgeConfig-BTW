package net.minecraftforge.common;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ConfigCategory implements Map<String, Property> {
    private String name;
    private String comment;
    private ArrayList<ConfigCategory> children;
    private Map<String, Property> properties;
    public final ConfigCategory parent;
    private boolean changed;

    public ConfigCategory(String name) {
        this(name, null);
    }

    public ConfigCategory(String name, ConfigCategory parent) {
        this.children = new ArrayList<>();
        this.properties = new TreeMap();
        this.changed = false;
        this.name = name;
        this.parent = parent;
        if (parent != null) {
            parent.children.add(this);
        }
    }

    @Override // java.util.Map
    public boolean equals(Object obj) {
        if (obj instanceof ConfigCategory) {
            ConfigCategory cat = (ConfigCategory) obj;
            return this.name.equals(cat.name) && this.children.equals(cat.children);
        }
        return false;
    }

    public String getQualifiedName() {
        return getQualifiedName(this.name, this.parent);
    }

    public static String getQualifiedName(String name, ConfigCategory parent) {
        return parent == null ? name : parent.getQualifiedName() + "." + name;
    }

    public ConfigCategory getFirstParent() {
        return this.parent == null ? this : this.parent.getFirstParent();
    }

    public boolean isChild() {
        return this.parent != null;
    }

    public Map<String, Property> getValues() {
        return ImmutableMap.copyOf(this.properties);
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean containsKey(String key) {
        return this.properties.containsKey(key);
    }

    public Property get(String key) {
        return this.properties.get(key);
    }

    private void write(BufferedWriter out, String... data) throws IOException {
        write(out, true, data);
    }

    private void write(BufferedWriter out, boolean new_line, String... data) throws IOException {
        for (String str : data) {
            out.write(str);
        }
        if (new_line) {
            out.write(Configuration.NEW_LINE);
        }
    }

    public void write(BufferedWriter out, int indent) throws IOException {
        String pad0 = getIndent(indent);
        String pad1 = getIndent(indent + 1);
        String pad2 = getIndent(indent + 2);
        write(out, pad0, "####################");
        write(out, pad0, "# ", this.name);
        if (this.comment != null) {
            write(out, pad0, "#===================");
            Splitter splitter = Splitter.onPattern("\r?\n");
            for (String line : splitter.split(this.comment)) {
                write(out, pad0, "# ", line);
            }
        }
        write(out, pad0, "####################", Configuration.NEW_LINE);
//        if (!Configuration.allowedProperties.matchesAllOf(this.name)) {
//            this.name = "\"" + this.name + "\"";
//        }
        write(out, pad0, this.name, " {");
        Property[] props = (Property[]) this.properties.values().toArray(new Property[this.properties.size()]);
        for (int x = 0; x < props.length; x++) {
            Property prop = props[x];
            if (prop.comment != null) {
                if (x != 0) {
                    out.newLine();
                }
                Splitter splitter2 = Splitter.onPattern("\r?\n");
                for (String commentLine : splitter2.split(prop.comment)) {
                    write(out, pad1, "# ", commentLine);
                }
            }
            String propName = prop.getName();
//            if (!Configuration.allowedProperties.matchesAllOf(propName)) {
//                propName = "\"" + propName + "\"";
//            }
            if (prop.isList()) {
                char type = prop.getType().getID();
                write(out, pad1, String.valueOf(type), ":", propName, " <");
                for (String line2 : prop.getStringList()) {
                    write(out, pad2, line2);
                }
                write(out, pad1, " >");
            } else if (prop.getType() == null) {
                write(out, pad1, propName, "=", prop.getString());
            } else {
                char type2 = prop.getType().getID();
                write(out, pad1, String.valueOf(type2), ":", propName, "=", prop.getString());
            }
        }
        Iterator<ConfigCategory> it = this.children.iterator();
        while (it.hasNext()) {
            ConfigCategory child = it.next();
            child.write(out, indent + 1);
        }
        write(out, pad0, "}", Configuration.NEW_LINE);
    }

    private String getIndent(int indent) {
        StringBuilder buf = new StringBuilder("");
        for (int x = 0; x < indent; x++) {
            buf.append("    ");
        }
        return buf.toString();
    }

    public boolean hasChanged() {
        if (this.changed) {
            return true;
        }
        for (Property prop : this.properties.values()) {
            if (prop.hasChanged()) {
                return true;
            }
        }
        return false;
    }

    void resetChangedState() {
        this.changed = false;
        for (Property prop : this.properties.values()) {
            prop.resetChangedState();
        }
    }

    @Override // java.util.Map
    public int size() {
        return this.properties.size();
    }

    @Override // java.util.Map
    public boolean isEmpty() {
        return this.properties.isEmpty();
    }

    @Override // java.util.Map
    public boolean containsKey(Object key) {
        return this.properties.containsKey(key);
    }

    @Override // java.util.Map
    public boolean containsValue(Object value) {
        return this.properties.containsValue(value);
    }

    @Override // java.util.Map
    public Property get(Object key) {
        return this.properties.get(key);
    }

    @Override // java.util.Map
    public Property put(String key, Property value) {
        this.changed = true;
        return this.properties.put(key, value);
    }

    @Override // java.util.Map
    public Property remove(Object key) {
        this.changed = true;
        return this.properties.remove(key);
    }

    @Override // java.util.Map
    public void putAll(Map<? extends String, ? extends Property> m) {
        this.changed = true;
        this.properties.putAll(m);
    }

    @Override // java.util.Map
    public void clear() {
        this.changed = true;
        this.properties.clear();
    }

    @Override // java.util.Map
    public Set<String> keySet() {
        return this.properties.keySet();
    }

    @Override // java.util.Map
    public Collection<Property> values() {
        return this.properties.values();
    }

    @Override // java.util.Map
    public Set<Map.Entry<String, Property>> entrySet() {
        return ImmutableSet.copyOf(this.properties.entrySet());
    }

    public Set<ConfigCategory> getChildren() {
        return ImmutableSet.copyOf(this.children);
    }

    public void removeChild(ConfigCategory child) {
        if (this.children.contains(child)) {
            this.children.remove(child);
            this.changed = true;
        }
    }
}
