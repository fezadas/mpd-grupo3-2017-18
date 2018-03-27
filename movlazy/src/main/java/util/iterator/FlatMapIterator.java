package util.iterator;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class FlatMapIterator<T,R> implements Iterator<R> {
    final Function<T, Iterable<R>> mapper;
    final Iterator<T> src;
    private Iterator<R> currScr;
    private R currValue;


    public FlatMapIterator(Iterable<T> src, Function<T, Iterable<R>> mapper) {
        this.src = src.iterator();
        this.mapper = mapper;

    }

    public boolean hasNext() {
        if(currValue==null && src.hasNext()) {
            currScr = mapper.apply(src.next()).iterator();
            if(currScr.hasNext())
                currValue = currScr.next();
            else
                currValue = null;
        }
        return false;
    }

    public R next() {
        if (!hasNext()) throw new NoSuchElementException();
        return currValue;
    }

}
