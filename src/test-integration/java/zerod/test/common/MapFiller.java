package zerod.test.common;

import java.util.Map;

public class MapFiller<K,V> {

    public final Map<K, V> map;

    public MapFiller(Map<K, V> map) {
        this.map = map;
    }

    public static <K, V> MapFiller<K, V> fill(Map<K, V> map) {
        return new MapFiller<K, V>(map);
    }

    public MapFiller<K, V> put(K key, V value) {
        map.put(key, value);
        return this;
    }
}
