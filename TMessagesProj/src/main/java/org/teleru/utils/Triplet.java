package org.teleru.utils;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Triplet<T1, T2, T3>
{
    public final T1 item1;
    public final T2 item2;
    public final T3 item3;


    public Triplet(T1 t1, T2 t2, T3 t3)
    {
        item1 = t1;
        item2 = t2;
        item3 = t3;
    }
}
