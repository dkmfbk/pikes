package eu.fbk.dkm.pikes.rdf.util;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Reflect {

    private static final Logger LOGGER = LoggerFactory.getLogger(Reflect.class);

    // These methods are to be merged in the utility class Reflect under spring-utils-common

    public static <T> T instantiate(final Class<T> baseClass, final Map<?, ?> properties,
            @Nullable String prefix, @Nullable String classProperty,
            @Nullable final String enabledProperty) {

        // Normalize prefix
        prefix = prefix == null ? "" : prefix.endsWith(".") ? prefix : prefix + ".";

        // Keep only properties matching the prefix, getting rid of it
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Map<String, Object> filteredProperties = (Map) properties;
        if (prefix.length() > 0) {
            filteredProperties = Maps.newLinkedHashMap();
            for (final Entry<?, ?> entry : properties.entrySet()) {
                final String key = entry.getKey().toString();
                if (key.startsWith(prefix) && key.length() > prefix.length()) {
                    filteredProperties.put(key.substring(prefix.length()), entry.getValue());
                }
            }
        }

        // Abort in case there is an 'enabled' properties with value false
        if (enabledProperty != null) {
            final Object enabled = filteredProperties.get(enabledProperty);
            final String enabledStr = enabled == null ? null
                    : enabled.toString().trim().toLowerCase();
            if (enabledStr != null && enabledStr.equals("false") || enabledStr.equals("no")
                    || enabledStr.equals("0")) {
                LOGGER.debug("Ignoring object: {}{} = {}", prefix, enabledProperty, enabled);
                return null;
            }
        }

        // Extract class name. Throw exception if not supplied
        classProperty = MoreObjects.firstNonNull(classProperty, "class");
        final Object classObj = filteredProperties.get(classProperty);
        if (classObj == null) {
            throw new IllegalArgumentException("Missing property " + prefix + classProperty);
        }

        // Obtain actual class object
        final Class<?> actualClass;
        try {
            actualClass = classObj instanceof Class<?> ? (Class<?>) classObj
                    : Class.forName(classObj.toString());
        } catch (final Throwable ex) {
            throw new IllegalArgumentException("Invalid class " + classObj);
        }

        // Verify that actual class extends base class
        if (!baseClass.isAssignableFrom(actualClass)) {
            throw new IllegalArgumentException("Class " + actualClass.getName()
                    + " is incompatible with " + baseClass.getName());
        }

        // TODO: below we should support different ways to instantiate the class...

        // Find suitable constructor (no parameters / Map parameter / Properties parameter)
        Constructor<?> chosenConstructor = null;
        for (final Constructor<?> constructor : actualClass.getDeclaredConstructors()) {
            final Class<?>[] argTypes = constructor.getParameterTypes();
            if (argTypes.length == 0 && chosenConstructor == null
                    || argTypes.length == 1 && argTypes[0].isAssignableFrom(Map.class)
                    || argTypes.length == 1 && argTypes[0].isAssignableFrom(Properties.class)) {
                chosenConstructor = constructor;
            }
        }
        if (chosenConstructor == null) {
            throw new IllegalArgumentException(
                    "No suitable constructor with no parameters / Map parameter / "
                            + "Properties parameter found for class " + actualClass.getName());
        }

        // Make the constructor accessible, if necessary
        if (!chosenConstructor.isAccessible()) {
            chosenConstructor.setAccessible(true);
        }

        // Prepare constructor parameters
        final Class<?>[] argTypes = chosenConstructor.getParameterTypes();
        final Object[] args = new Object[argTypes.length];
        for (int i = 0; i < argTypes.length; ++i) {
            if (argTypes[i].isAssignableFrom(Map.class)) {
                args[i] = ImmutableMap.copyOf(filteredProperties);
            } else if (argTypes[i].isAssignableFrom(Properties.class)) {
                final Properties props = new Properties();
                props.putAll(filteredProperties);
                args[i] = props;
            }
        }

        // Invoke constructor and return created instance
        try {
            return baseClass.cast(chosenConstructor.newInstance(args));
        } catch (final Throwable ex) {
            throw new IllegalArgumentException("Cannot invoke " + chosenConstructor
                    + " with parameters " + Arrays.asList(args), ex);
        }
    }

    public static <T> Map<String, T> instantiateAll(final Class<T> baseClass,
            final Map<?, ?> properties, @Nullable String prefix,
            @Nullable final String classProperty, @Nullable final String enabledProperty,
            @Nullable final String orderProperty) {

        // Normalize prefix
        prefix = prefix == null ? "" : prefix.endsWith(".") ? prefix : prefix + ".";

        // Scan properties, creating instances and extracting order specifications
        final Map<Integer, String> order = Maps.newHashMap();
        final Map<String, Optional<T>> instances = Maps.newLinkedHashMap();
        for (final Entry<?, ?> entry : properties.entrySet()) {
            final String key = entry.getKey().toString();
            if (!key.startsWith(prefix)) {
                continue;
            }
            final int index = key.indexOf('.', prefix.length());
            if (index < 0 || index == key.length() - 1) {
                continue;
            }
            final String name = key.substring(prefix.length(), index);
            final String property = key.substring(index + 1);
            if (orderProperty != null && orderProperty.equals(property)) {
                order.put(Integer.parseInt(entry.getValue().toString()), name);
            }
            if (!instances.containsKey(name)) {
                instances.put(name, Optional.ofNullable(instantiate(baseClass, properties,
                        prefix + name + ".", classProperty, enabledProperty)));
            }
        }

        // Reorder created instances based on order specifications
        final ImmutableMap.Builder<String, T> builder = ImmutableMap.builder();
        for (final Integer index : Ordering.natural().sortedCopy(order.keySet())) {
            final String name = order.get(index);
            final Optional<T> instance = instances.get(name);
            if (instance != null && instance.isPresent()) {
                builder.put(name, instance.get());
                instances.remove(name);
            }
        }
        for (final Entry<String, Optional<T>> entry : instances.entrySet()) {
            final Optional<T> instance = entry.getValue();
            if (instance.isPresent()) {
                builder.put(entry.getKey(), instance.get());
            }
        }
        return builder.build();
    }

}
