package app;

import android.app.Application;

public class BookerApplication extends Application{

    private static BookerApplication singleton;

    public static BookerApplication getInstance() {
        return singleton;
    }

    public void BookerApplication(){
        singleton = this;
    }
}
