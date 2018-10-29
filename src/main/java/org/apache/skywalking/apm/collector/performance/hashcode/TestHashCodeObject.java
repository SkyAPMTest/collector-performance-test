package org.apache.skywalking.apm.collector.performance.hashcode;

/**
 * @author peng-yongsheng
 */
public class TestHashCodeObject {

    private String id;
    private int time;
    private int front;
    private int behind;

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getFront() {
        return front;
    }

    public void setFront(int front) {
        this.front = front;
    }

    public int getBehind() {
        return behind;
    }

    public void setBehind(int behind) {
        this.behind = behind;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override public int hashCode() {
        int result = 1;
        result = 31 * result + time;
        result = 31 * result + front;
        result = 31 * result + behind;
        return result;
    }
}
