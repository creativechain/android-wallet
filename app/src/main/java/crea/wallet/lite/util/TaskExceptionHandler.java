package crea.wallet.lite.util;

import android.os.AsyncTask;

/**
 * Created by Andersson G. Acosta on 21/06/17.
 */

public abstract class TaskExceptionHandler<Params, Result> extends AsyncTask<Params, Exception, Result> {

    private Task<Exception> task;

    public TaskExceptionHandler(Task<Exception> task) {
        this.task = task;
    }

    @Override
    protected void onProgressUpdate(Exception... values) {
        if (values != null && values.length > 0 &&  task != null) {
            task.doTask(values[0]);
        }
    }
}
