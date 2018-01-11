package ee.ria.mopplib;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class DataFile {

    public abstract String id();

    public abstract String name();

    public abstract long size();
}
