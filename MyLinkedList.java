import org.jetbrains.annotations.NotNull;

import java.util.AbstractSequentialList;
import java.util.ConcurrentModificationException;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class MyLinkedList<E> extends AbstractSequentialList<E> {
    private final Node<E> fake;
    private int size;
    private int modCount;
    
    public MyLinkedList() {
        fake = new Node<>();
        fake.prev = fake;
        fake.next = fake;
        
        size = 0;
        modCount = 0;
    }
    
    public void addFirst(E e) {
        Node<E> prev = fake;
        Node<E> next = fake.next;
        Node<E> newFirst = new Node<>(prev, e, next);
        
        link(prev, newFirst, next);
    }
    
    public void addLast(E e) {
        Node<E> prev = fake.prev;
        Node<E> next = fake;
        Node<E> newLast = new Node<>(prev, e, next);
        
        link(prev, newLast, next);
    }
    
    @Override
    public boolean add(E e) {
        addLast(e);
        return true;
    }
    
    @Override
    public void add(int index, E e) {
        checkIndexForAdd(index);
        
        Node<E> next = node(index);
        Node<E> prev = next.prev;
        
        Node<E> newNode = new Node<>(prev, e, next);
        
        link(prev, newNode, next);
    }
    
    public E removeFirst() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return unlink(fake.next);
    }
    
    public E removeLast() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return unlink(fake.prev);
    }
    
    @Override
    public boolean remove(Object o) {
        Node<E> node = fake.next;
        if (o == null) {
            while (node != fake) {
                if (null == node.value) {
                    unlink(node);
                    return true;
                }
                node = node.next;
            }
        } else {
            while (node != fake) {
                if (o.equals(node.value)) {
                    unlink(node);
                    return true;
                }
                node = node.next;
            }
        }
        
        return false;
    }
    
    @Override
    public E remove(int index) {
        checkIndexForGet(index);
        return unlink(node(index));
    }
    
    @Override
    public E set(int index, E e) {
        checkIndexForGet(index);
        
        Node<E> node = node(index);
        E oldValue = node.value;
        node.value = e;
        
        return oldValue;
    }
    
    @Override
    public E get(int index) {
        checkIndexForGet(index);
        return node(index).value;
    }
    
    @Override
    public void clear() {
        Node<E> node = fake.next;
        while (node != fake) {
            Node<E> next = node.next;
            
            node.value = null;
            node.prev = null;
            node.next = null;
            
            node = next;
        }
    }
    
    @Override
    public int size() {
        return size;
    }
    
    @Override
    public boolean isEmpty() {
        return size == 0;
    }
    
    @Override
    @NotNull
    public ListIterator<E> listIterator(int index) {
        checkIndexForAdd(index);
        return new ListIteratorImpl(index);
    }
    
    private void link(Node<E> prev, Node<E> node, Node<E> next) {
        // assert node already link prev and next
        prev.next = node;
        next.prev = node;
        
        size++;
        modCount++;
    }
    
    private void checkIndexForGet(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index);
        }
    }
    
    private void checkIndexForAdd(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException(index);
        }
    }
    
    private E unlink(Node<E> node) {
        // assert node isn't fake node
        Node<E> prev = node.prev;
        Node<E> next = node.next;
        
        prev.next = next;
        next.prev = prev;
        
        E returned = node.value;
        
        node.value = null;
        node.prev = null;
        node.next = null;
        
        size--;
        modCount++;
        
        return returned;
    }
    
    private Node<E> node(int index) {
        // assert index >= 0 && index <= size
        Node<E> node;
        if (index < (size >> 1)) {
            node = fake.next;
            while (index-- > 0) {
                node = node.next;
            }
        } else {
            node = fake;
            while (index++ < size) {
                node = node.prev;
            }
        }
        return node;
    }
    
    private static class Node<E> {
        E value;
        Node<E> prev;
        Node<E> next;
        
        public Node() {
        }
        
        public Node(Node<E> prev, E value, Node<E> next) {
            this.value = value;
            this.prev = prev;
            this.next = next;
        }
    }
    
    private class ListIteratorImpl implements ListIterator<E> {
        Node<E> next;
        Node<E> lastReturn;
        int nextIndex;
        int expectedModCount;
        
        public ListIteratorImpl(int index) {
            next = node(index);
            lastReturn = null;
            nextIndex = index;
            expectedModCount = modCount;
        }
        
        @Override
        public boolean hasNext() {
            return nextIndex < size;
        }
        
        @Override
        public E next() {
            checkModCount();
            
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            
            E returned = next.value;
            
            lastReturn = next;
            next = next.next;
            nextIndex++;
            
            return returned;
        }
        
        @Override
        public boolean hasPrevious() {
            return nextIndex > 0;
        }
        
        @Override
        public E previous() {
            checkModCount();
            
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            
            Node<E> previous = next.prev;
            E returned = previous.value;
            
            lastReturn = previous;
            next = previous;
            nextIndex--;
            
            return returned;
        }
        
        @Override
        public int nextIndex() {
            return nextIndex;
        }
        
        @Override
        public int previousIndex() {
            return nextIndex - 1;
        }
        
        @Override
        public void remove() {
            checkModCount();
            
            if (lastReturn == null) {
                throw new IllegalStateException();
            }
            
            if (lastReturn == next) { // last access: previous()
                Node<E> newNext = next.next;
                unlink(lastReturn);
                next = newNext;
            } else { // last access: next()
                unlink(lastReturn);
                nextIndex--;
            }
            
            lastReturn = null;
            expectedModCount++;
        }
        
        @Override
        public void set(E e) {
            checkModCount();
            
            if (lastReturn == null) {
                throw new IllegalStateException();
            }
            
            lastReturn.value = e;
        }
        
        @Override
        public void add(E e) {
            checkModCount();
            
            lastReturn = null;
            
            Node<E> prev = next.prev;
            Node<E> newNode = new Node<>(prev, e, next);
            link(prev, newNode, next);
            
            nextIndex++;
            expectedModCount++;
        }
        
        private void checkModCount() {
            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
        }
    }
}
