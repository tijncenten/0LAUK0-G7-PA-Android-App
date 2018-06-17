package nl.tue.robotseverywhere.personalassistant;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import nl.tue.robotseverywhere.personalassistant.adapters.ListMessagesAdapter;
import pma.PersonalMessagingAssistant;
import pma.chatparsers.MessageParser;
import pma.message.Message;

public class PAHandler {

    private PersonalMessagingAssistant pa;
    private MessageParser mp;
    private int batchSize = 200;

    private Context context;
    private ListMessagesAdapter mAdapter;

    private List<Message> messages = new ArrayList<>();

    public PAHandler(Context c, ListMessagesAdapter adapter) {
        this.context = c;
        this.mAdapter = adapter;

        this.mp = new MessageParser();
        this.pa = new PersonalMessagingAssistant(mp, batchSize);

        try {
            pa.getUserPreferences().load(context.getAssets().open("pa-network-storage/emre-es3.prefs.txt"));
            pa.getBayesianEvaluation().load(context.getAssets().open("pa-network-storage/emre-es3.bayesian.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load(List<Message> messages) {
        mAdapter.setData(messages);
        this.messages = messages;
    }

    public void load(InputStream is) throws ParseException {
        this.load(mp.parse(is));
    }

    public void load(File f) throws FileNotFoundException, ParseException {
        this.load(mp.parse(f));
    }

    public void clear() {
        this.messages = new ArrayList<>();
        mAdapter.setData(messages);
    }

    public void addMessage(Message m) {
        this.messages.add(m);
        mAdapter.setData(messages);
        mAdapter.notifyItemInserted(this.messages.size()-1);
    }

    public void process(ProcessDoneListener l) {

        if (messages != null) {
            ProcessMessagesTask task = new ProcessMessagesTask(pa, l);
            task.execute();
        }
    }

    public interface ProcessDoneListener {
        void processDone();
    }

    private class ProcessMessagesTask extends AsyncTask<Void, Void, PersonalMessagingAssistant.EvalResult[]> {

        private PersonalMessagingAssistant pa;
        private ProcessDoneListener doneListener;

        public ProcessMessagesTask(PersonalMessagingAssistant pa, ProcessDoneListener doneListener) {
            this.pa = pa;
            this.doneListener = doneListener;
        }

        @Override
        protected PersonalMessagingAssistant.EvalResult[] doInBackground(Void... v) {
            return pa.process(messages);
        }

        @Override
        protected void onPostExecute(PersonalMessagingAssistant.EvalResult[] results) {
            mAdapter.setResults(results);

            // Update thread indices
            List<Message> outputMessages = pa.getLastOutputMessages();
            int[] threadIndices = new int[outputMessages.size()];
            for (int i = 0; i < outputMessages.size(); i++) {
                threadIndices[i] = outputMessages.get(i).getThreadIndex();
            }
            mAdapter.setThreads(threadIndices);

            doneListener.processDone();
        }
    }
}
