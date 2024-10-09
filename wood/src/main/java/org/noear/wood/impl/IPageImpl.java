package org.noear.wood.impl;

import org.noear.wood.IPage;

import java.util.List;

/**
 * @author noear 2021/11/11 created
 */
public class IPageImpl<T> implements IPage<T> {

    private List<T> list;
    private long total;
    private int size;

    public IPageImpl(List<T> list, long total, int size) {
        this.list = list;
        this.total = total;
        this.size = size;
    }

    @Override
    public List<T> getList() {
        return list;
    }

    @Override
    public long getTotal() {
        return total;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public long getPages() {
        if (this.getSize() == 0L) {
            return 0L;
        } else {
            long pages = this.getTotal() / this.getSize();
            if (this.getTotal() % this.getSize() != 0L) {
                ++pages;
            }

            return pages;
        }
    }

    @Override
    public String toString() {
        return "IPage{" +
                "total=" + total +
                ", size=" + size +
                ", list=" + list +
                '}';
    }
}
