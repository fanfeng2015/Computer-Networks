public class RingBuffer {

    private int capacity;
    private int head, tail; // tail ... head, guarantee head >= tail
    private byte[] array;

    public RingBuffer(int capacity) {
        this.capacity = capacity;
        this.head = this.tail = 0;
        this.array = new byte[capacity];
    }

    // TODO
    public byte[] get(int pos, int len) {

    }

    public int put(byte[] data) {
        int len = Math.min(arr.length, remaining()); // only enough space
        int begin = head % capacity;
        if (head + len < capacity) {
            System.arraycopy(data, 0, array, begin, len);
        }
        else {
            System.arraycopy(data, 0, array, begin, capacity - begin);
            System.arraycopy(data, capacity - begin, array, 0, len - (capacity - begin));
        }
        head += len;
        return len;
    }

    // Move tail forward by delta.
    public void move(int delta) {
        if (delta > 0) {
            tail += delta;
            tail = Math.min(head, tail);
        }
    }

    // Return current size of the ring buffer.
    public int size() {
        return head - tail;
    }

    // Returns remaining space of the ring buffer.
    public int remaining() {
        return capacity - size();
    }

}
