package co.uk.zerod.sample.dao;

import com.google.common.collect.ImmutableMap;
import mtymes.javafixes.object.Tuple;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.function.Function;

import static com.google.common.collect.Maps.newLinkedHashMap;
import static mtymes.javafixes.object.Tuple.tuple;

public class VersionedStore<K> {

    private final Map<K, AtomicStampedReference<Map>> values = new ConcurrentHashMap<>();

    public Set<K> keySet() {
        return values.keySet();
    }

    public Tuple<Map, Integer> getValueWithVersion(K key) {
        AtomicStampedReference<Map> versionedValue = values.get(key);

        if (versionedValue != null) {
            Map valueCopy = newLinkedHashMap(versionedValue.getReference());
            int version = versionedValue.getStamp();

            return tuple(valueCopy, version);
        } else {
            return null;
        }
    }

    public Map getValue(K key) {
        Tuple<Map, Integer> valueWithVersion = getValueWithVersion(key);

        return (valueWithVersion == null) ? null : valueWithVersion.a;
    }

    public void insert(K key, Map value) {
        Map valueCopy = newLinkedHashMap(value);

        AtomicStampedReference<Map> reference = new AtomicStampedReference<>(valueCopy, 0);
        boolean success = values.putIfAbsent(key, reference) == null;

        if (!success) {
            throw new IllegalStateException("Value already present for key " + key);
        }
    }

    public boolean update(K key, Map newValue, int lastKnownVersion) {
        AtomicStampedReference<Map> versionedValue = values.get(key);
        if (versionedValue == null) {
            throw new IllegalArgumentException("Value not found for key " + key);
        }

        return versionedValue.compareAndSet(versionedValue.getReference(), newValue, lastKnownVersion, lastKnownVersion + 1);
    }

    public void conditionalUpdate(K key, Function<Map, Optional<Map>> conditionalUpdater) {
        boolean success;
        do {
            Tuple<Map, Integer> valueWithVersion = getValueWithVersion(key);
            if (valueWithVersion == null) {
                throw new IllegalArgumentException("Value not found for key " + key);
            }

            Map oldValue = valueWithVersion.a;
            Optional<Map> possibleNewValue = conditionalUpdater.apply(oldValue);

            if (!possibleNewValue.isPresent()) {
                success = true;
            } else {
                Integer oldVersion = valueWithVersion.b;
                success = update(key, possibleNewValue.get(), oldVersion);
            }
        } while (!success);
    }

    public static void main(String[] args) {
        VersionedStore<String> store = new VersionedStore<>();

        String key = "keyId";

        store.insert(key, ImmutableMap.<String, String>builder().put("firstKey", "firstValue").build());

        System.out.println(store.getValue(key));
        System.out.println(store.getValueWithVersion(key));
        System.out.println();


        store.conditionalUpdate(key, map -> {
            map.put("ignoredKey", "ignoredKey");
            return Optional.empty();
        });

        System.out.println(store.getValue(key));
        System.out.println(store.getValueWithVersion(key));
        System.out.println();


        store.conditionalUpdate(key, map -> {
            map.put("secondKey", "secondValue");
            return Optional.of(map);
        });

        System.out.println(store.getValue(key));
        System.out.println(store.getValueWithVersion(key));
        System.out.println();


        Tuple<Map, Integer> valueWithVersion = store.getValueWithVersion(key);
        Map value = valueWithVersion.a;
        Integer lastVersion = valueWithVersion.b;
        value.put("thirdKey", "thirdValue");
        store.update(key, value, lastVersion);

        System.out.println(store.getValue(key));
        System.out.println(store.getValueWithVersion(key));
        System.out.println();


        valueWithVersion = store.getValueWithVersion(key);
        value = valueWithVersion.a;
        lastVersion = valueWithVersion.b;
        value.put("secondIgnoredKey", "secondIgnoredKey");

        store.conditionalUpdate(key, map -> {
            map.put("fourthKey", "fourthValue");
            return Optional.of(map);
        });

        store.update(key, value, lastVersion);

        System.out.println(store.getValue(key));
        System.out.println(store.getValueWithVersion(key));
        System.out.println();
    }
}
