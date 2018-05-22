package org.teleru;

import org.telegram.messenger.ApplicationLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Observers
{
    public enum NotifyId
    {
        ConnectionChanged,
        UpdateAvailable,
        UpdateOk,

        ExportDialogsListChanged,
        ExportDialogsCurrentItemChanged,
        ExportDialogsItemAdded,
        ExportDialogsItemRemoved,
        ExportDialogsItemCompleted,
        ExportDialogsItemFailed
    }

    public interface IObserver
    {
        void onNotifyReceive(NotifyId id, Object... args);
    }

    private final static Observers instance = new Observers();

    public static void addObservers(final IObserver listener, final NotifyId... ids)
    {
        instance.addObserversInternal(listener, ids);
    }

    public static void removeObservers(final IObserver listener, final NotifyId... ids)
    {
        instance.removeObserversInternal(listener, ids);
    }

    public static void push(final NotifyId id, final Object... args)
    {
        instance.pushInternal(true, id, args);
    }

    public static void pushAsync(final NotifyId id, final Object... args)
    {
        instance.pushInternal(false, id, args);
    }


    private HashMap<NotifyId, List<IObserver>> observers = new HashMap<>();

    private Observers()
    {
    }

    private void addObserversInternal(final IObserver listener, final NotifyId... ids)
    {
        ApplicationLoader.applicationHandler.post(() -> {
            for (NotifyId id : ids)
            {
                List<IObserver> listeners = observers.get(id);
                if (listeners == null)
                {
                    listeners = new ArrayList<>();
                    observers.put(id, listeners);
                }

                if (!listeners.contains(listener))
                    listeners.add(listener);
            }
        });
    }

    private void removeObserversInternal(final IObserver listener, final NotifyId... ids)
    {
        ApplicationLoader.applicationHandler.post( () -> {
            for (NotifyId id : ids)
            {
                List<IObserver> listeners = observers.get(id);
                if (listeners != null)
                {
                    if (listeners.contains(listener))
                        listeners.remove(listener);

                    if (listeners.isEmpty())
                        observers.remove(id);
                }
            }
        });
    }

    private void pushInternal(boolean block, final NotifyId id, final Object... args)
    {
        if (block)
        {
            try
            {
                this.wait(1000 * 2);
            }
            catch (Exception ignored) {}
        }

        ApplicationLoader.applicationHandler.post(() -> {
            List<IObserver> listeners = observers.get(id);
            if (listeners != null)
            {
                for (IObserver listener : listeners)
                    listener.onNotifyReceive(id, args);
            }
        });
    }
}
