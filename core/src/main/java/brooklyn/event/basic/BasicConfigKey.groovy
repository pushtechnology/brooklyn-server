package brooklyn.event.basic

import groovy.transform.EqualsAndHashCode

import java.util.Collection
import java.util.concurrent.Future

import brooklyn.entity.ConfigKey
import brooklyn.management.ExecutionContext
import brooklyn.management.Task
import brooklyn.util.internal.ConfigKeySelfExtracting;
import brooklyn.util.internal.LanguageUtils

import com.google.common.base.Splitter
import com.google.common.collect.Lists

@EqualsAndHashCode(includeFields=true)
class BasicConfigKey<T> implements ConfigKey<T>, ConfigKeySelfExtracting<T>, Serializable {
    private static final long serialVersionUID = -1762014059150215376L
    
    private static final Splitter dots = Splitter.on('.')
    
    public final String name
    public final Class<T> type
    public final String typeName
    public final String description
    public final T defaultValue
    
    public BasicConfigKey() { /* for gson */ }
    
    public BasicConfigKey(Class<T> type, String name, String description=name, T defaultValue=null) {
        this.description = description
        this.name = name
        this.type = type
        this.typeName = type.name
        this.defaultValue = defaultValue
    }

    /** @see ConfigKey#getName() */
    public String getName() { name }

    /** @see ConfigKey#getTypeName() */
    public String getTypeName() { typeName }

    /** @see ConfigKey#getType() */
    public Class<T> getType() { type }

    /** @see ConfigKey#getDescription() */
    public String getDescription() { description }

    /** @see ConfigKey#getDefaultValue() */
    public T getDefaultValue() { defaultValue }

    /** @see ConfigKey#hasDefaultValue() */
    public boolean hasDefaultValue() {
        return defaultValue != null
    }

    /** @see ConfigKey#getNameParts() */
    public Collection<String> getNameParts() {
        return Lists.newArrayList(dots.split(name));
    }
 
    @Override
    public String toString() {
        return String.format("Config: %s (%s)", name, typeName)
    }

    /**
     * Retrieves the value corresponding to this config key from the given map.
     * Could be overridden by more sophisticated config keys, such as MapConfigKey etc.
     */
    public T extractValue(Map vals, ExecutionContext exec) {
        Object v = vals.get(this)
        return resolveValue(v, exec)
    }
    
    protected Object resolveValue(Object v, ExecutionContext exec) {
        //if it's a task, we wait for the task to complete
        if (v in Task) {
            if (!((Task) v).isSubmitted() ) {
                exec.submit((Task) v)
            }
            v = ((Task) v).get()
        } else if (v in Future) {
            v = ((Future) v).get()
        } else if (v in Closure) {
            v = ((Closure) v).call()
        } else if (v in Map) {
            Map result = [:]
			v.each { k,val -> result << [(k): resolveValue(val, exec)] }
            return result
        } else if (v in List) {
            List result = []
			v.each { result << resolveValue(it, exec) }
            return result
        } else return v
        return resolveValue(v, exec)
    }
}

class SubElementConfigKey<T> extends BasicConfigKey<T> {
    public final ConfigKey parent
    
    public SubElementConfigKey(ConfigKey parent, Class<T> type, String name, String description=name, T defaultValue=null) {
        super(type, name, description, defaultValue)
        this.parent = parent
    }
    
    public T extractValue(Map vals, ExecutionContext exec) {
        return super.extractValue(vals, exec)
    }
}

// TODO Create interface
class MapConfigKey<V> extends BasicConfigKey<Map<String,V>> {
    public final Class<V> subType
    
    public MapConfigKey(Class<V> subType, String name, String description=name, Map<String,V> defaultValue=null) {
        super(Map.class, name, description, defaultValue)
        this.subType = subType
    }

    public ConfigKey<V> subKey(String subName) {
        new SubElementConfigKey(this, subType, name+"."+subName, "sub-element of "+name+", named "+subName, null)
    }
    
    public boolean isSubKey(ConfigKey<?> contender) {
        return (contender instanceof SubElementConfigKey && this == ((SubElementConfigKey)contender).parent)
    }
    
    public String extractSubKeyName(ConfigKey<?> subKey) {
        return subKey.name.substring(name.length()+1)
    }
    
    public Map<String,V> extractValue(Map vals, ExecutionContext exec) {
        Map<String,V> result = [:]
        for (Map.Entry<ConfigKey,Object> entry in vals.entrySet()) {
            if (isSubKey(entry.key)) {
                result.put(extractSubKeyName(entry.key), entry.key.extractValue(vals, exec))
            }
        }
        return result
    }
}

// TODO Create interface
class ListConfigKey<V> extends BasicConfigKey<List<V>> {
    public final Class<V> subType
    
    public ListConfigKey(Class<V> subType, String name, String description=name, Map<String,V> defaultValue=null) {
        super(List.class, name, description, defaultValue)
        this.subType = subType
    }

    public ConfigKey<V> subKey() {
        String subName = LanguageUtils.newUid()
        new SubElementConfigKey(this, subType, name+"."+subName, "element of "+name+", uid "+subName, null)
    }
    
    public boolean isSubKey(ConfigKey<?> contender) {
        return (contender instanceof SubElementConfigKey && this == ((SubElementConfigKey)contender).parent)
    }
    
    public List<V> extractValue(Map vals, ExecutionContext exec) {
        List<V> result = []
		vals.each { k,v -> 
			if (isSubKey(k))
				result << ((SubElementConfigKey)k).extractValue(vals, exec)
        }
//        for (Map.Entry<ConfigKey,Object> entry in vals.entrySet()) {
//            if (isSubKey(entry.key)) {
//                SubElementConfigKey subKey = entry.key
//                result.add(subKey.extractValue(vals, exec))
//            }
//        }
        return result
    }
}
