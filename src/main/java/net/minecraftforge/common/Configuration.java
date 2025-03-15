package net.minecraftforge.common;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableSet;
import cpw.mods.fml.common.FMLLog;
import net.minecraft.src.Block;
import net.minecraft.src.Item;
import net.minecraft.src.Minecraft;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Configuration {
    private static final int ITEM_SHIFT = 256;
    private static final int MAX_BLOCKS = 4096;
    public static final String CATEGORY_GENERAL = "general";
    public static final String CATEGORY_BLOCK = "block";
    public static final String CATEGORY_ITEM = "item";
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final String CATEGORY_SPLITTER = ".";
    public static final String NEW_LINE;
    File file;
    private Map<String, ConfigCategory> categories;
    private Map<String, Configuration> children;
    private boolean caseSensitiveCustomCategories;
    public String defaultEncoding;
    private String fileName;
    public boolean isChild;
    private boolean changed;
    private static boolean[] configMarkers = new boolean[32767];
    private static final Pattern CONFIG_START = Pattern.compile("START: \"([^\\\"]+)\"");
    private static final Pattern CONFIG_END = Pattern.compile("END: \"([^\\\"]+)\"");
    public static final String ALLOWED_CHARS = "._-";
//    public static final CharMatcher allowedProperties = CharMatcher.JAVA_LETTER_OR_DIGIT.or(CharMatcher.anyOf(ALLOWED_CHARS));
    private static Configuration PARENT = null;

    static {
        Arrays.fill(configMarkers, false);
        NEW_LINE = System.getProperty("line.separator");
    }

    public Configuration() {
        this.categories = new TreeMap();
        this.children = new TreeMap();
        this.defaultEncoding = DEFAULT_ENCODING;
        this.fileName = null;
        this.isChild = false;
        this.changed = false;
    }

    public Configuration(File file) {
        this.categories = new TreeMap();
        this.children = new TreeMap();
        this.defaultEncoding = DEFAULT_ENCODING;
        this.fileName = null;
        this.isChild = false;
        this.changed = false;
        this.file = file;
        String basePath = new File(CATEGORY_SPLITTER).getAbsolutePath().replace(File.separatorChar, '/').replace("/.", "");
        String path = file.getAbsolutePath().replace(File.separatorChar, '/').replace("/./", "/").replace(basePath, "");
        if (PARENT != null) {
            PARENT.setChild(path, this);
            this.isChild = true;
        } else {
            this.fileName = path;
            load();
        }
    }

    public Configuration(File file, boolean caseSensitiveCustomCategories) {
        this(file);
        this.caseSensitiveCustomCategories = caseSensitiveCustomCategories;
    }

//    public Property getBlock(String key, int defaultID) {
//        return getBlock(CATEGORY_BLOCK, key, defaultID, null);
//    }
//
//    public Property getBlock(String key, int defaultID, String comment) {
//        return getBlock(CATEGORY_BLOCK, key, defaultID, comment);
//    }
//
//    public Property getBlock(String category, String key, int defaultID) {
//        return getBlockInternal(category, key, defaultID, null, ITEM_SHIFT, Block.blocksList.length);
//    }
//
//    public Property getBlock(String category, String key, int defaultID, String comment) {
//        return getBlockInternal(category, key, defaultID, comment, ITEM_SHIFT, Block.blocksList.length);
//    }
//
//    public Property getTerrainBlock(String category, String key, int defaultID, String comment) {
//        return getBlockInternal(category, key, defaultID, comment, 0, ITEM_SHIFT);
//    }
//
//    private Property getBlockInternal(String category, String key, int defaultID, String comment, int lower, int upper) {
//        Property prop = get(category, key, -1, comment);
//        if (prop.getInt() != -1) {
//            configMarkers[prop.getInt()] = true;
//            return prop;
//        }
//        if (defaultID < lower) {
//            FMLLog.warning("Mod attempted to get a block ID with a default in the Terrain Generation section, mod authors should make sure there defaults are above 256 unless explicitly needed for terrain generation. Most ores do not need to be below 256.", new Object[0]);
//            FMLLog.warning("Config \"%s\" Category: \"%s\" Key: \"%s\" Default: %d", this.fileName, category, key, Integer.valueOf(defaultID));
//            defaultID = upper - 1;
//        }
//        if (Block.blocksList[defaultID] == null && !configMarkers[defaultID]) {
//            prop.set(defaultID);
//            configMarkers[defaultID] = true;
//            return prop;
//        }
//        for (int j = upper - 1; j > 0; j--) {
//            if (Block.blocksList[j] == null && !configMarkers[j]) {
//                prop.set(j);
//                configMarkers[j] = true;
//                return prop;
//            }
//        }
//        throw new RuntimeException("No more block ids available for " + key);
//    }
//
//    public Property getItem(String key, int defaultID) {
//        return getItem(CATEGORY_ITEM, key, defaultID, null);
//    }
//
//    public Property getItem(String key, int defaultID, String comment) {
//        return getItem(CATEGORY_ITEM, key, defaultID, comment);
//    }
//
//    public Property getItem(String category, String key, int defaultID) {
//        return getItem(category, key, defaultID, null);
//    }
//
//    public Property getItem(String category, String key, int defaultID, String comment) {
//        Property prop = get(category, key, -1, comment);
//        int defaultShift = defaultID + ITEM_SHIFT;
//        if (prop.getInt() != -1) {
//            configMarkers[prop.getInt() + ITEM_SHIFT] = true;
//            return prop;
//        }
//        if (defaultID < 3840) {
//            FMLLog.warning("Mod attempted to get a item ID with a default value in the block ID section, mod authors should make sure there defaults are above %d unless explicitly needed so that all block ids are free to store blocks.", 3840);
//            FMLLog.warning("Config \"%s\" Category: \"%s\" Key: \"%s\" Default: %d", this.fileName, category, key, Integer.valueOf(defaultID));
//        }
//        if (Item.itemsList[defaultShift] == null && !configMarkers[defaultShift] && defaultShift >= Block.blocksList.length) {
//            prop.set(defaultID);
//            configMarkers[defaultShift] = true;
//            return prop;
//        }
//        for (int x = Item.itemsList.length - 1; x >= ITEM_SHIFT; x--) {
//            if (Item.itemsList[x] == null && !configMarkers[x]) {
//                prop.set(x - ITEM_SHIFT);
//                configMarkers[x] = true;
//                return prop;
//            }
//        }
//        throw new RuntimeException("No more item ids available for " + key);
//    }

    public Property get(String category, String key, int defaultValue) {
        return get(category, key, defaultValue, (String) null);
    }

    public Property get(String category, String key, int defaultValue, String comment) {
        Property prop = get(category, key, Integer.toString(defaultValue), comment, Property.Type.INTEGER);
        if (!prop.isIntValue()) {
            prop.set(defaultValue);
        }
        return prop;
    }

    public Property get(String category, String key, boolean defaultValue) {
        return get(category, key, defaultValue, (String) null);
    }

    public Property get(String category, String key, boolean defaultValue, String comment) {
        Property prop = get(category, key, Boolean.toString(defaultValue), comment, Property.Type.BOOLEAN);
        if (!prop.isBooleanValue()) {
            prop.set(defaultValue);
        }
        return prop;
    }

    public Property get(String category, String key, double defaultValue) {
        return get(category, key, defaultValue, (String) null);
    }

    public Property get(String category, String key, double defaultValue, String comment) {
        Property prop = get(category, key, Double.toString(defaultValue), comment, Property.Type.DOUBLE);
        if (!prop.isDoubleValue()) {
            prop.set(defaultValue);
        }
        return prop;
    }

    public Property get(String category, String key, String defaultValue) {
        return get(category, key, defaultValue, (String) null);
    }

    public Property get(String category, String key, String defaultValue, String comment) {
        return get(category, key, defaultValue, comment, Property.Type.STRING);
    }

    public Property get(String category, String key, String[] defaultValue) {
        return get(category, key, defaultValue, (String) null);
    }

    public Property get(String category, String key, String[] defaultValue, String comment) {
        return get(category, key, defaultValue, comment, Property.Type.STRING);
    }

    public Property get(String category, String key, int[] defaultValue) {
        return get(category, key, defaultValue, (String) null);
    }

    public Property get(String category, String key, int[] defaultValue, String comment) {
        String[] values = new String[defaultValue.length];
        for (int i = 0; i < defaultValue.length; i++) {
            values[i] = Integer.toString(defaultValue[i]);
        }
        Property prop = get(category, key, values, comment, Property.Type.INTEGER);
        if (!prop.isIntList()) {
            prop.set(values);
        }
        return prop;
    }

    public Property get(String category, String key, double[] defaultValue) {
        return get(category, key, defaultValue, (String) null);
    }

    public Property get(String category, String key, double[] defaultValue, String comment) {
        String[] values = new String[defaultValue.length];
        for (int i = 0; i < defaultValue.length; i++) {
            values[i] = Double.toString(defaultValue[i]);
        }
        Property prop = get(category, key, values, comment, Property.Type.DOUBLE);
        if (!prop.isDoubleList()) {
            prop.set(values);
        }
        return prop;
    }

    public Property get(String category, String key, boolean[] defaultValue) {
        return get(category, key, defaultValue, (String) null);
    }

    public Property get(String category, String key, boolean[] defaultValue, String comment) {
        String[] values = new String[defaultValue.length];
        for (int i = 0; i < defaultValue.length; i++) {
            values[i] = Boolean.toString(defaultValue[i]);
        }
        Property prop = get(category, key, values, comment, Property.Type.BOOLEAN);
        if (!prop.isBooleanList()) {
            prop.set(values);
        }
        return prop;
    }

    public Property get(String category, String key, String defaultValue, String comment, Property.Type type) {
        if (!this.caseSensitiveCustomCategories) {
            category = category.toLowerCase(Locale.ENGLISH);
        }
        ConfigCategory cat = getCategory(category);
        if (cat.containsKey(key)) {
            Property prop = cat.get(key);
            if (prop.getType() == null) {
                prop = new Property(prop.getName(), prop.getString(), type);
                cat.put(key, prop);
            }
            prop.comment = comment;
            return prop;
        }
        if (defaultValue != null) {
            Property prop2 = new Property(key, defaultValue, type);
            prop2.set(defaultValue);
            cat.put(key, prop2);
            prop2.comment = comment;
            return prop2;
        }
        return null;
    }

    public Property get(String category, String key, String[] defaultValue, String comment, Property.Type type) {
        if (!this.caseSensitiveCustomCategories) {
            category = category.toLowerCase(Locale.ENGLISH);
        }
        ConfigCategory cat = getCategory(category);
        if (cat.containsKey(key)) {
            Property prop = cat.get(key);
            if (prop.getType() == null) {
                prop = new Property(prop.getName(), prop.getString(), type);
                cat.put(key, prop);
            }
            prop.comment = comment;
            return prop;
        }
        if (defaultValue != null) {
            Property prop2 = new Property(key, defaultValue, type);
            prop2.comment = comment;
            cat.put(key, prop2);
            return prop2;
        }
        return null;
    }

    public boolean hasCategory(String category) {
        return this.categories.get(category) != null;
    }

    public boolean hasKey(String category, String key) {
        ConfigCategory cat = this.categories.get(category);
        return cat != null && cat.containsKey(key);
    }

    public void load() {
        block53: {
            if (PARENT != null && PARENT != this) {
                return;
            }
            BufferedReader buffer = null;
            UnicodeInputStreamReader input = null;
            try {
                if (this.file.getParentFile() != null) {
                    this.file.getParentFile().mkdirs();
                }
                if (!this.file.exists() && !this.file.createNewFile()) {
                    return;
                }
                if (!this.file.canRead()) break block53;
                input = new UnicodeInputStreamReader(new FileInputStream(this.file), this.defaultEncoding);
                this.defaultEncoding = input.getEncoding();
                buffer = new BufferedReader(input);
                ConfigCategory currentCat = null;
                Property.Type type = null;
                ArrayList<String> tmpList = null;
                int lineNum = 0;
                String name = null;
                while (true) {
                    ++lineNum;
                    String line = buffer.readLine();
                    if (line == null) {
                        break;
                    }
                    Matcher start = CONFIG_START.matcher(line);
                    Matcher end = CONFIG_END.matcher(line);
                    if (start.matches()) {
                        this.fileName = start.group(1);
                        this.categories = new TreeMap<String, ConfigCategory>();
                        continue;
                    }
                    if (end.matches()) {
                        this.fileName = end.group(1);
                        Configuration child = new Configuration();
                        child.categories = this.categories;
                        this.children.put(this.fileName, child);
                        continue;
                    }
                    int nameStart = -1;
                    int nameEnd = -1;
                    boolean skip = false;
                    boolean quoted = false;
                    block33: for (int i = 0; i < line.length() && !skip; ++i) {
                        if (Character.isLetterOrDigit(line.charAt(i)) || ALLOWED_CHARS.indexOf(line.charAt(i)) != -1 || quoted && line.charAt(i) != '\"') {
                            if (nameStart == -1) {
                                nameStart = i;
                            }
                            nameEnd = i;
                            continue;
                        }
                        if (Character.isWhitespace(line.charAt(i))) continue;
                        switch (line.charAt(i)) {
                            case '#': {
                                skip = true;
                                continue block33;
                            }
                            case '\"': {
                                if (quoted) {
                                    quoted = false;
                                }
                                if (quoted || nameStart != -1) continue block33;
                                quoted = true;
                                continue block33;
                            }
                            case '{': {
                                name = line.substring(nameStart, nameEnd + 1);
                                String qualifiedName = ConfigCategory.getQualifiedName(name, currentCat);
                                ConfigCategory cat = this.categories.get(qualifiedName);
                                if (cat == null) {
                                    currentCat = new ConfigCategory(name, currentCat);
                                    this.categories.put(qualifiedName, currentCat);
                                } else {
                                    currentCat = cat;
                                }
                                name = null;
                                continue block33;
                            }
                            case '}': {
                                if (currentCat == null) {
                                    throw new RuntimeException(String.format("Config file corrupt, attepted to close to many categories '%s:%d'", this.fileName, lineNum));
                                }
                                currentCat = currentCat.parent;
                                continue block33;
                            }
                            case '=': {
                                name = line.substring(nameStart, nameEnd + 1);
                                if (currentCat == null) {
                                    throw new RuntimeException(String.format("'%s' has no scope in '%s:%d'", name, this.fileName, lineNum));
                                }
                                Property prop = new Property(name, line.substring(i + 1), type, true);
                                i = line.length();
                                currentCat.put(name, prop);
                                continue block33;
                            }
                            case ':': {
                                type = Property.Type.tryParse(line.substring(nameStart, nameEnd + 1).charAt(0));
                                nameEnd = -1;
                                nameStart = -1;
                                continue block33;
                            }
                            case '<': {
                                if (tmpList != null) {
                                    throw new RuntimeException(String.format("Malformed list property \"%s:%d\"", this.fileName, lineNum));
                                }
                                name = line.substring(nameStart, nameEnd + 1);
                                if (currentCat == null) {
                                    throw new RuntimeException(String.format("'%s' has no scope in '%s:%d'", name, this.fileName, lineNum));
                                }
                                tmpList = new ArrayList<String>();
                                skip = true;
                                continue block33;
                            }
                            case '>': {
                                if (tmpList == null) {
                                    throw new RuntimeException(String.format("Malformed list property \"%s:%d\"", this.fileName, lineNum));
                                }
                                currentCat.put(name, new Property(name, tmpList.toArray(new String[tmpList.size()]), type));
                                name = null;
                                tmpList = null;
                                type = null;
                                continue block33;
                            }
                            default: {
                                throw new RuntimeException(String.format("Unknown character '%s' in '%s:%d'", Character.valueOf(line.charAt(i)), this.fileName, lineNum));
                            }
                        }
                    }
                    if (quoted) {
                        throw new RuntimeException(String.format("Unmatched quote in '%s:%d'", this.fileName, lineNum));
                    }
                    if (tmpList == null || skip) continue;
                    tmpList.add(line.trim());
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if (buffer != null) {
                    try {
                        buffer.close();
                    }
                    catch (IOException iOException) {}
                }
                if (input != null) {
                    try {
                        input.close();
                    }
                    catch (IOException iOException) {}
                }
            }
        }
        this.resetChangedState();
    }

    public void save() {
        if (PARENT != null && PARENT != this) {
            PARENT.save();
            return;
        }
        try {
            if (this.file.getParentFile() != null) {
                this.file.getParentFile().mkdirs();
            }
            if (!this.file.exists() && !this.file.createNewFile()) {
                return;
            }
            if (this.file.canWrite()) {
                FileOutputStream fos = new FileOutputStream(this.file);
                BufferedWriter buffer = new BufferedWriter(new OutputStreamWriter(fos, this.defaultEncoding));
                buffer.write("# Configuration file" + NEW_LINE + NEW_LINE);
                if (this.children.isEmpty()) {
                    save(buffer);
                } else {
                    for (Map.Entry<String, Configuration> entry : this.children.entrySet()) {
                        buffer.write("START: \"" + entry.getKey() + "\"" + NEW_LINE);
                        entry.getValue().save(buffer);
                        buffer.write("END: \"" + entry.getKey() + "\"" + NEW_LINE + NEW_LINE);
                    }
                }
                buffer.close();
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save(BufferedWriter out) throws IOException {
        for (ConfigCategory cat : this.categories.values()) {
            if (!cat.isChild()) {
                cat.write(out, 0);
                out.newLine();
            }
        }
    }

    public ConfigCategory getCategory(String category) {
        ConfigCategory ret = this.categories.get(category);
        if (ret == null) {
            if (category.contains(CATEGORY_SPLITTER)) {
                String[] hierarchy = category.split("\\.");
                ConfigCategory parent = this.categories.get(hierarchy[0]);
                if (parent == null) {
                    parent = new ConfigCategory(hierarchy[0]);
                    this.categories.put(parent.getQualifiedName(), parent);
                    this.changed = true;
                }
                for (int i = 1; i < hierarchy.length; i++) {
                    String name = ConfigCategory.getQualifiedName(hierarchy[i], parent);
                    ConfigCategory child = this.categories.get(name);
                    if (child == null) {
                        child = new ConfigCategory(hierarchy[i], parent);
                        this.categories.put(name, child);
                        this.changed = true;
                    }
                    ret = child;
                    parent = child;
                }
            } else {
                ret = new ConfigCategory(category);
                this.categories.put(category, ret);
                this.changed = true;
            }
        }
        return ret;
    }

    public void removeCategory(ConfigCategory category) {
        for (ConfigCategory child : category.getChildren()) {
            removeCategory(child);
        }
        if (this.categories.containsKey(category.getQualifiedName())) {
            this.categories.remove(category.getQualifiedName());
            if (category.parent != null) {
                category.parent.removeChild(category);
            }
            this.changed = true;
        }
    }

    public void addCustomCategoryComment(String category, String comment) {
        if (!this.caseSensitiveCustomCategories) {
            category = category.toLowerCase(Locale.ENGLISH);
        }
        getCategory(category).setComment(comment);
    }

    private void setChild(String name, Configuration child) {
        if (!this.children.containsKey(name)) {
            this.children.put(name, child);
            this.changed = true;
        } else {
            Configuration old = this.children.get(name);
            child.categories = old.categories;
            child.fileName = old.fileName;
            old.changed = true;
        }
    }

    public static void enableGlobalConfig() {
        PARENT = new Configuration(new File(Minecraft.getMinecraft().mcDataDir + "config", "global.cfg"));
        PARENT.load();
    }

    /* loaded from: Forge Config-0.0.1.jar:net/minecraftforge/common/Configuration$UnicodeInputStreamReader.class */
    public static class UnicodeInputStreamReader extends Reader {
        private final InputStreamReader input;
        private final String defaultEnc;

        public UnicodeInputStreamReader(InputStream source, String encoding) throws IOException {
            this.defaultEnc = encoding;
            String enc = encoding;
            byte[] data = new byte[4];
            PushbackInputStream pbStream = new PushbackInputStream(source, data.length);
            int read = pbStream.read(data, 0, data.length);
            int size = 0;
            int bom16 = ((data[0] & 255) << 8) | (data[1] & 255);
            int bom24 = (bom16 << 8) | (data[2] & 255);
            int bom32 = (bom24 << 8) | (data[3] & 255);
            if (bom24 == 15711167) {
                enc = Configuration.DEFAULT_ENCODING;
                size = 3;
            } else if (bom16 == 65279) {
                enc = "UTF-16BE";
                size = 2;
            } else if (bom16 == 65534) {
                enc = "UTF-16LE";
                size = 2;
            } else if (bom32 == 65279) {
                enc = "UTF-32BE";
                size = 4;
            } else if (bom32 == -131072) {
                enc = "UTF-32LE";
                size = 4;
            }
            if (size < read) {
                pbStream.unread(data, size, read - size);
            }
            this.input = new InputStreamReader(pbStream, enc);
        }

        public String getEncoding() {
            return this.input.getEncoding();
        }

        @Override // java.io.Reader
        public int read(char[] cbuf, int off, int len) throws IOException {
            return this.input.read(cbuf, off, len);
        }

        @Override // java.io.Reader, java.io.Closeable, java.lang.AutoCloseable
        public void close() throws IOException {
            this.input.close();
        }
    }

    public boolean hasChanged() {
        if (this.changed) {
            return true;
        }
        for (ConfigCategory cat : this.categories.values()) {
            if (cat.hasChanged()) {
                return true;
            }
        }
        for (Configuration child : this.children.values()) {
            if (child.hasChanged()) {
                return true;
            }
        }
        return false;
    }

    private void resetChangedState() {
        this.changed = false;
        for (ConfigCategory cat : this.categories.values()) {
            cat.resetChangedState();
        }
        for (Configuration child : this.children.values()) {
            child.resetChangedState();
        }
    }

    public Set<String> getCategoryNames() {
        return ImmutableSet.copyOf(this.categories.keySet());
    }
}
