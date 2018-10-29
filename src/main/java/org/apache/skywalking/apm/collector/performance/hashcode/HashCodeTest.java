package org.apache.skywalking.apm.collector.performance.hashcode;

import java.util.*;

/**
 * @author peng-yongsheng
 */
public class HashCodeTest {

    public static void main(String[] args) {
        int size = 50000;
        List<TestHashCodeObject> objects = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            TestHashCodeObject object = new TestHashCodeObject();
            object.setTime(2018062521);
            object.setFront(i);
            object.setBehind(i);
            object.setId(String.valueOf(i) + "_" + String.valueOf(i) + "_" + String.valueOf(i));
            objects.add(object);
        }

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            objects.forEach(object -> object.hashCode());
        }
        System.out.println("duration: " + String.valueOf(System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            objects.forEach(object -> object.getId().hashCode());
        }
        System.out.println("duration: " + String.valueOf(System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        Map<TestHashCodeObject, TestHashCodeObject> map = new HashMap<>();
        objects.forEach(object -> map.put(object, object));
        objects.forEach(object -> map.get(object));
        System.out.println("duration: " + String.valueOf(System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        Map<String, TestHashCodeObject> map2 = new HashMap<>();
        objects.forEach(object -> map2.put(object.getId(), object));
        objects.forEach(object -> map2.get(object.getId()));
        System.out.println("duration: " + String.valueOf(System.currentTimeMillis() - start));
    }
}
